package com.tlc.sql.oltp.internal.pgsql;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.handler.AbstractDMLHandler;
import com.tlc.sql.common.api.meta.ArrayColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.common.util.DDLDMLUtil;
import com.tlc.sql.oltp.api.dml.function.OltpSqlFunctionBuilderFactory;
import com.tlc.sql.oltp.api.meta.OltpDataType;
import com.tlc.sql.oltp.internal.handler.OltpDMLHandler;
import com.tlc.sql.oltp.internal.util.PgsqlDdlDmlUtil;
import com.tlc.sql.oltp.resolver.OltpTableResolver;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 *
 * This class provides PostgreSQL-specific implementations of DML (Data Manipulation Language) operations.
 * It extends the AbstractDMLHandler and implements the OltpDMLHandler interface.
 */
public class PgsqlDMLHandler extends AbstractDMLHandler implements OltpDMLHandler {

    private final TableResolver tableResolver;

    /**
     * Default constructor for PgsqlDMLHandler.
     * Initializes the table resolver with the OltpTableResolver instance.
     */
    PgsqlDMLHandler() {
        this.tableResolver = OltpTableResolver.get();
    }

    /**
     * Generates the SQL query to fetch the next value from a sequence.
     *
     * @param sequenceName the name of the sequence
     * @return the SQL query to fetch the next value from the sequence
     */
    @Override
    public String getNextValueSequenceSQL(String sequenceName) {
        return "SELECT nextval('" + sequenceName + "')";
    }

    /**
     * Generates the SQL query to fetch the next value and increment value from a sequence.
     *
     * @param sequenceName the name of the sequence
     * @return the SQL query to fetch the next value and increment value from the sequence
     */
    @Override
    public String getNextValueLimitSequenceSQL(String sequenceName) {
        return "SELECT nextval('" + sequenceName + "'), increment FROM information_schema.sequences where sequence_name='" + sequenceName + "'";
    }

    /**
     * Retrieves the table resolver for PostgreSQL.
     *
     * @return the table resolver instance
     */
    @Override
    public TableResolver getTableResolver() {
        return tableResolver;
    }

    /**
     * Wraps the given value with the LOWER SQL function for case-insensitive matching.
     *
     * @param value    the formatted value string to wrap
     * @param dataType the data type of the column
     * @return the value wrapped with {@code LOWER()}
     */
    @Override
    protected String wrapWithLower(final String value, final DataType dataType) {
        final SqlFunction lowerFunction = OltpSqlFunctionBuilderFactory.lower()
                .addArgument(columnResolver -> value)
                .build();
        return lowerFunction.toString(Column::getColumnName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSqlArrayType(final DataType dataType) {
        if (dataType.isCommon()) {
            final CommonDataType commonDataType = (CommonDataType) dataType;

            return switch (commonDataType) {
                case SMALLINT -> "SMALLINT";
                case INTEGER -> "INTEGER";
                case BIGINT -> "BIGINT";
                case DECIMAL -> "DECIMAL";
                case TEXT -> "TEXT";
                case BOOLEAN -> "BOOLEAN";
                default -> DDLDMLUtil.getDataTypeStr(commonDataType);
            };
        } else if (dataType instanceof final OltpDataType oltpDataType) {
            return switch (oltpDataType) {
                case DATE, TIME, DATETIME -> "BIGINT";
                case CHAR, SCHAR, KCHAR, STEXT -> "TEXT";
                default -> throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            };
        }
        throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
    }

    /**
     * Retrieves a value from a ResultSet for a given position and data type.
     *
     * @param resultSet the ResultSet object
     * @param position the column position
     * @param dataType the data type of the column
     * @return the retrieved value
     * @throws SQLException if an SQL error occurs
     */
    @Override
    public Object getValueFromResultSet(ResultSet resultSet, int position, DataType dataType) throws SQLException {

        if (dataType.isArray()) {
            final Array array = resultSet.getArray(position);
            if (Objects.isNull(array)) {
                return List.of();
            }
            final List<Object> arrayList = getArrayResultSet(array);
            return dataType.getWrappedValue(arrayList);
        }
        final Object object = resultSet.getObject(position);

        if (resultSet.wasNull()) {
            return null;
        } else {
            return dataType.getWrappedValue(object);
        }
    }

    /**
     * Reads array elements from a JDBC Array using the driver's intrinsic type info.
     * This approach works regardless of whether the column was defined via legacy or generic ARRAY model.
     */
    private List<Object> getArrayResultSet(final Array array) throws SQLException {
        final Object javaArray = array.getArray();

        if (javaArray instanceof Object[] objectArray) {
            final List<Object> result = new ArrayList<>(objectArray.length);

            for (final Object element : objectArray) {

                if (element != null) {
                    result.add(element);
                }
            }
            return result;
        }
        return null;
    }

    /**
     * <p>
     * Sets the array value on a PreparedStatement for a given parameter index and data type.
     * </p>
     *
     * @param statement the PreparedStatement to set the value on
     * @param index     the parameter index (1-based)
     * @param dataType  the DataType of the column, which should be an array type
     * @param value     the value to set, which may be a Java array or collection
     * @throws SQLException
     */
    @Override
    protected void handleArrayType(final PreparedStatement statement, final int index, final DataType dataType, final Object value) throws SQLException {
        final Connection connection = statement.getConnection();

        if (value instanceof final List<?> list) {
            final String typeName;

            if (dataType instanceof final OltpDataType oltpDataType) {
                final CommonDataType elementType = oltpDataType.getElementDataType();

                if (elementType == null) {
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
                }
                typeName = PgsqlDdlDmlUtil.getArrayTypeName(elementType);
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
            }
            statement.setArray(index, connection.createArrayOf(typeName, list.toArray()));
        }
    }

    /**
     * Dispatches comparator value loading based on the column's data type.
     * Default implementation handles standard OLTP types: filterable-string, numeric, boolean, and string.
     * Subclasses can override to add dialect-specific type handling (e.g., JSON, Array, BuiltInFunction types).
     *
     * @param column         the column being compared
     * @param value          the comparator value
     * @param builder        the SQL string builder
     * @param operator       the comparison operator
     * @param updateTableDef whether to include table aliases
     */
    @Override
    protected void loadComparatorByDataType(final Column column, final Object value, final StringBuilder builder, final Operator operator, final boolean updateTableDef) {
        final DataType dataType = column.getColumnDefinition().getDataType();

        if (dataType.isArray()) {
            if (Objects.isNull(value)) {
                builder.append(getArrayNullComparator(getColumnIdentity(column, updateTableDef, true), operator));
            } else {
                final DataType elementType;

                if (column.getColumnDefinition() instanceof final ArrayColumnDefinition arrayColDef) {
                    elementType = arrayColDef.getElementType();
                } else if (dataType instanceof final OltpDataType oltpDataType) {
                    elementType = oltpDataType.getElementDataType();
                } else {
                    elementType = CommonDataType.TEXT;
                }

                if (elementType == CommonDataType.TEXT || (elementType instanceof OltpDataType oltpDataType && oltpDataType.isString())) {
                    loadStringArrayComparator(column, value, builder, operator, updateTableDef);
                } else {
                    loadNumberArrayComparator(column, value, builder, operator, updateTableDef);
                }
            }
        } else {
            super.loadComparatorByDataType(column, value, builder, operator, updateTableDef);
        }
    }

    /**
     * <p>
     * Builds the null/empty comparator for array-type columns.
     * Array columns can have either NULL or an empty array '{}'. Both should be treated as "empty".
     * </p>
     *
     * <p>For EQUAL (is empty): {@code (column IS NULL OR CARDINALITY(column) = 0)}</p>
     * <p>For NOT_EQUAL (is not empty): {@code (column IS NOT NULL AND CARDINALITY(column) > 0)}</p>
     *
     * @param columnIdentity the SQL column identity string.
     * @param operator       the operator (EQUAL or NOT_EQUAL).
     * @return the SQL fragment for array empty/not-empty check.
     */
    private String getArrayNullComparator(final String columnIdentity, final Operator operator) {

        if (operator == Operator.EQUAL) {
            return String.join("", "(", columnIdentity, " IS NULL OR CARDINALITY(", columnIdentity, ") = 0)");
        } else if (operator == Operator.NOT_EQUAL) {
            return String.join("", "(", columnIdentity, " IS NOT NULL AND CARDINALITY(", columnIdentity, ") > 0)");
        } else {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }
    }

    /**
     * <p>
     * Loads the string array comparator.
     * </p>
     *
     * <p>Supported operations:</p>
     * <ul>
     *   <li><b>EQUAL + String</b>: {@code value = ANY(column)} — checks if array contains the exact value</li>
     *   <li><b>NOT_EQUAL + String</b>: {@code value <> ALL(column)} — checks if array does NOT contain the exact value</li>
     *   <li><b>IN + Iterable&lt;String&gt;</b>: {@code (v1 = ANY(column) OR v2 = ANY(column) ...)} — checks if array contains any of the values</li>
     *   <li><b>NOT_IN + Iterable&lt;String&gt;</b>: {@code (v1 <> ALL(column) AND v2 <> ALL(column) ...)} — checks if array contains none of the values</li>
     *   <li><b>CONTAINS + String</b>: {@code column::text LIKE '%value%'} — text search across the array</li>
     *   <li><b>NOT_CONTAINS + String</b>: {@code column::text NOT LIKE '%value%'} — negated text search across the array</li>
     * </ul>
     *
     * @param column         Contains the column details
     * @param value          Represents the value (String or Iterable of Strings)
     * @param builder        String builder to append the SQL fragment
     * @param operator       The comparison operator
     * @param updateTableDef Update table definition flag
     */
    private void loadStringArrayComparator(final Column column, final Object value, final StringBuilder builder, final Operator operator,
            final boolean updateTableDef) {
        final DataType dataType = column.getColumnDefinition().getDataType();
        final String columnIdentity = getColumnIdentity(column, true, updateTableDef);

        if (value instanceof final Iterable<?> collection) {
            final Iterator<?> iterator = collection.iterator();

            if (!iterator.hasNext()) {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }

            if (Operator.IN == operator || Operator.NOT_IN == operator) {
                final boolean isInOperator = (Operator.IN == operator);
                final String logicalOperator = isInOperator ? " OR " : " AND ";
                final String comparator = isInOperator ? " = " : " != ";
                final String quantifier = isInOperator ? "ANY" : "ALL";

                builder.append("(");
                int index = 0;

                do {
                    final Object nextValue = iterator.next();

                    if (!(nextValue instanceof String)) {
                        throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
                    }
                    if (index++ > 0) {
                        builder.append(logicalOperator);
                    }
                    builder.append(getFormattedString(nextValue, dataType)).append(comparator).append(quantifier).append("(").append(columnIdentity).append(")");
                } while (iterator.hasNext());
                builder.append(")");
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }
        } else {
            final Object converted = dataType.getUnWrappedValue(value);

            if (!(converted instanceof String)) {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }

            if (PREPARED_STMT_CONSTANT.equals(converted)) {
                builder.append(columnIdentity).append(getComparator(operator)).append(converted);
            } else if (Operator.CONTAINS == operator || Operator.NOT_CONTAINS == operator) {
                builder.append(columnIdentity).append("::").append("text").append(getComparator(operator))
                        .append(getFormattedString("%" + converted + "%", dataType));
            } else if (Operator.EQUAL == operator) {
                builder.append(getFormattedString(converted, dataType)).append(" = ").append("ANY").append("(").append(columnIdentity).append(")");
            } else if (Operator.NOT_EQUAL == operator) {
                builder.append(getFormattedString(converted, dataType)).append(" != ").append("ALL").append("(").append(columnIdentity).append(")");
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }
        }
    }

    /**
     * <p>
     * Loads the number array comparator.
     * </p>
     *
     * <p>Supported operations:</p>
     * <ul>
     *   <li><b>EQUAL + Number</b>: {@code value = ANY(column)} — checks if array contains the value</li>
     *   <li><b>NOT_EQUAL + Number</b>: {@code value <> ALL(column)} — checks if array does NOT contain the value</li>
     *   <li><b>IN + Iterable&lt;Number&gt;</b>: {@code (v1 = ANY(column) OR v2 = ANY(column) ...)} — checks if array contains any of the values</li>
     *   <li><b>NOT_IN + Iterable&lt;Number&gt;</b>: {@code (v1 <> ALL(column) AND v2 <> ALL(column) ...)} — checks if array contains none of the values</li>
     * </ul>
     *
     * @param column         Contains the column details
     * @param value          Value (Number or Iterable of Numbers)
     * @param builder        String builder to append the SQL fragment
     * @param operator       The comparison operator
     * @param updateTableDef Update table definition flag
     */
    private void loadNumberArrayComparator(final Column column, final Object value, final StringBuilder builder, final Operator operator,
            final boolean updateTableDef) {

        if (value instanceof Number) {

            if (Operator.EQUAL == operator || Operator.NOT_EQUAL == operator) {
                final String quantifier = (Operator.EQUAL == operator) ? "ANY" : "ALL";
                builder.append(value).append(getComparator(operator)).append(quantifier).append("(").append(getColumnIdentity(column, true, updateTableDef)).append(")");
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }
        } else if (value instanceof final Iterable<?> collection) {
            final Iterator<?> iterator = collection.iterator();

            if (!iterator.hasNext()) {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }
            final String columnIdentity = getColumnIdentity(column, true, updateTableDef);

            if (Operator.IN == operator || Operator.NOT_IN == operator) {
                final boolean isInOperator = (Operator.IN == operator);
                final String logicalOperator = isInOperator ? " OR " : " AND ";
                final String comparator = isInOperator ? " = " : " != ";
                final String quantifier = isInOperator ? "ANY" : "ALL";

                builder.append("(");
                int index = 0;

                do {
                    final Object nextValue = iterator.next();

                    if (!(nextValue instanceof Number)) {
                        throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
                    }
                    if (index++ > 0) {
                        builder.append(logicalOperator);
                    }
                    builder.append(nextValue).append(comparator).append(quantifier).append("(").append(columnIdentity).append(")");
                } while (iterator.hasNext());
                builder.append(")");
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }
        } else {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }
    }
}