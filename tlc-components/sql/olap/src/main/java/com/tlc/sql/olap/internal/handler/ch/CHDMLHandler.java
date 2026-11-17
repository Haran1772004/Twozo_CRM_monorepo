package com.tlc.sql.olap.internal.handler.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JsonColumn;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.meta.ArrayColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.util.DDLDMLUtil;
import com.tlc.sql.olap.api.dml.OlapSelectQuery;
import com.tlc.sql.olap.internal.handler.AbstractOlapDMLHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Sundar
 * @version 1.0
 *
 * Handles ClickHouse-specific SQL generation for OLAP queries.
 * Extends the AbstractOlapDMLHandler to provide database-specific implementations
 * for ClickHouse, including support for FINAL modifier, hasAny function, and
 * other ClickHouse-specific SQL features.
 */
public class CHDMLHandler extends AbstractOlapDMLHandler {

    private static final DDLDMLUtil.IdentifierQuote CH_IDENTIFIER_QUOTE = DDLDMLUtil.IdentifierQuote.BACK_TICK;
    private static final DDLDMLUtil.IdentifierQuote CH_EMPTY_IDENTIFIER_QUOTE = DDLDMLUtil.IdentifierQuote.EMPTY;
    private static final String MAX_LIMIT_VALUE = "18446744073709551615"; // Max UINT64

    private boolean returnUpdatedData = false;

    /**
     * Constructor for CHDMLHandler.
     * Initializes the handler with default settings.
     */
    public CHDMLHandler() {
        super();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Captures the query-level {@code returnUpdatedData} flag before delegating to the parent
     * implementation, so that {@link #getTableSource(Table)} can append the ClickHouse FINAL
     * modifier when required.
     * </p>
     */
    @Override
    public String getSelectSQL(final SelectQuery selectQuery) {
        if (selectQuery instanceof final OlapSelectQuery olapSelectQuery) {
            this.returnUpdatedData = olapSelectQuery.isReturnUpdatedData();
        }
        try {
            return super.getSelectSQL(selectQuery);
        } finally {
            this.returnUpdatedData = false;
        }
    }

    /**
     * Generates a COUNT query for ClickHouse.
     * Supports unique count using uniqExact function.
     *
     * @param query the CountQuery object containing query details
     * @return the generated COUNT query as a String
     */
    @Override
    public String getCountQuery(final CountQuery query) {
        final Table baseTable = query.getBaseTable();
        final StringBuilder selectQueryBuilder = new StringBuilder(500);

        selectQueryBuilder.append("SELECT ");
        query.getColumn().ifPresentOrElse(column ->
                        selectQueryBuilder.append("uniqExact(").append(getColumnIdentity(column, true, false)),
                () -> selectQueryBuilder.append("COUNT(*"));
        selectQueryBuilder.append(") FROM ");
        selectQueryBuilder.append(getTableSource(baseTable));
        fillFromClause(selectQueryBuilder, query.getJoinClause());

        final WhereClause whereClause = query.getWhereClause();

        if (whereClause != null) {
            selectQueryBuilder.append(" WHERE ");
            fillWhereClause(selectQueryBuilder, whereClause, true);
        }

        return selectQueryBuilder.toString();
    }

    /**
     * Generates the LIMIT clause for ClickHouse queries.
     *
     * @param limitClause the LimitClause object containing limit details
     * @return the generated LIMIT clause as a String
     */
    @Override
    protected String getLimitClauseStr(final LimitClause limitClause) {
        final int start = limitClause.getStart();
        final int numberOfResults = limitClause.getNumberOfResults();

        if (start <= 0) {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_LIMIT_CLAUSE);
        }

        final StringBuilder limitBuilder = new StringBuilder(25);

        limitBuilder.append("LIMIT ").append(numberOfResults <= 0 ? MAX_LIMIT_VALUE : numberOfResults);

        if (start > 1) {
            limitBuilder.append(" OFFSET ").append(start - 1);
        }

        return limitBuilder.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Appends the ClickHouse FINAL modifier to the table source when the query-level
     * {@code returnUpdatedData} flag is set via {@link OlapSelectQuery#isReturnUpdatedData()}, ensuring
     * the latest version of each row is returned for ReplacingMergeTree tables.
     * </p>
     *
     * @param table the table for which the source expression is resolved
     * @return the table source expression, with " FINAL" appended if the query-level flag is set
     */
    @Override
    protected String getTableSource(final Table table) {
        final String tableSource = super.getTableSource(table);

        if (table instanceof DerivedTable) {
            return tableSource;
        }

        return returnUpdatedData ? tableSource + " FINAL" : tableSource;
    }

    /**
     * Retrieves the table name with ClickHouse-specific identifier quoting.
     *
     * @param table the table name as a String
     * @return the quoted table name as a String
     */
    @Override
    protected String getTableName(final String table) {
        return DDLDMLUtil.getTableName(table, CH_IDENTIFIER_QUOTE);
    }

    /**
     * Retrieves the table name for a given TableDefinition with ClickHouse-specific identifier quoting.
     *
     * @param tableDefinition the TableDefinition object
     * @return the quoted table name as a String
     */
    @Override
    protected String getTableName(final TableDefinition<?> tableDefinition) {
        if (tableDefinition == null) {
            return getTableName("");
        }
        return DDLDMLUtil.getTableName(tableDefinition, CH_IDENTIFIER_QUOTE);
    }

    /**
     * Retrieves the column name with ClickHouse-specific identifier quoting.
     *
     * @param column the column name as a String
     * @return the quoted column name as a String
     */
    @Override
    protected String getColumnName(final String column) {
        return DDLDMLUtil.getColumnName(column, CH_EMPTY_IDENTIFIER_QUOTE);
    }

    /**
     * Returns an empty string for ClickHouse ORDER BY clause null handling.
     *
     * @param type the OrderType (ASC/DESC)
     * @param nullFirst whether nulls should appear first
     * @return an empty string
     */
    @Override
    protected String getOrderByClauseNullStr(final OrderByClause.OrderType type, final boolean nullFirst) {
        return "";
    }

    /**
     * Converts a Boolean value to its ClickHouse string representation.
     *
     * @param value the Boolean value
     * @return "1" for true, "0" for false
     */
    @Override
    protected String getBooleanString(final Boolean value) {
        return value ? "1" : "0";
    }

    /**
     * Escapes special characters in a string for ClickHouse.
     *
     * @param value the input string
     * @return the escaped string
     */
    @Override
    protected String escapeSpecialChars(final String value) {
        return value.replace("\\", "\\\\").replace("'", "''").trim();
    }

    /**
     * Returns the value as-is without wrapping with LOWER function.
     * ClickHouse uses ILIKE for case-insensitive matching, so LOWER is not needed.
     *
     * @param value    the formatted value string
     * @param dataType the data type of the column
     * @return the value without LOWER wrapping
     */
    @Override
    protected String wrapWithLower(final String value, final DataType dataType) {
        return value;
    }

    /**
     * Dispatches comparator loading based on data type.
     * Handles ClickHouse-specific data types like JSON, DATETIME/DATE, and Array.
     *
     * @param column the Column object
     * @param value the value to compare
     * @param builder the StringBuilder for appending SQL
     * @param operator the comparison operator
     * @param updateTableDef whether to update the table definition
     */
    @Override
    protected void loadComparatorByDataType(final Column column, final Object value, final StringBuilder builder,
                                            final Operator operator, final boolean updateTableDef) {
        final DataType dataType = column.getColumnDefinition().getDataType();

        if (dataType.isJson()) {
            if (value instanceof String) {
                loadStringComparatorValue(column, value, builder, operator, updateTableDef);
            } else {
                loadNumberComparatorValue(column, value, builder, operator, updateTableDef);
            }
        } else if (dataType.isArray()) {
            loadArrayComparator(column, value, builder, operator, updateTableDef);
        } else {
            super.loadComparatorByDataType(column, value, builder, operator, updateTableDef);
        }
    }

    /**
     * {@inheritDoc}
     * <p>Dispatches JSON comparator generation by the column's extracted return type
     * (numeric, string, array, or JSON null), using ClickHouse-specific SQL semantics.</p>
     */
    @Override
    protected void loadJsonComparatorValue(final Column column, final Object value, final StringBuilder builder,
                                           final Operator operator, final boolean updateTableDef) {
        if (!(column instanceof final JsonColumn jsonColumn)) {
            throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
        }

        final DataType returnType = jsonColumn.getReturnType();

        if (returnType == null) {
            throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
        }

        if (value == null) {
            appendJsonNullComparator(builder, column, returnType, operator, updateTableDef);
            return;
        }

        if (returnType.isArray()) {
            if (operator == Operator.HAS_ANY || operator == Operator.NOT_HAS_ANY) {
                loadHasAnyComparator(column, value, builder, operator, updateTableDef);
                return;
            }
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }

        if (returnType.isNumeric()) {
            loadNumberComparatorValue(column, getJsonNumericValue(value, operator), builder, operator, updateTableDef);
            return;
        }

        if (returnType.isString() || returnType.isFilterableString()) {

            if (value instanceof final String[] pair && (operator == Operator.BETWEEN || operator == Operator.NOT_BETWEEN)) {

                if (pair.length != 2) {
                    throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
                }

                builder.append(getColumnIdentity(column, updateTableDef, true))
                        .append(getComparator(operator))
                        .append(getFormattedString(pair[0], returnType))
                        .append(" AND ")
                        .append(getFormattedString(pair[1], returnType));
                return;
            }

            loadStringComparatorValue(column, value, builder, operator, updateTableDef);
            return;
        }

        throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
    }

    /**
     * ClickHouse JSON null semantics are mapped by extracted return type.
     */
    private void appendJsonNullComparator(final StringBuilder builder, final Column column, final DataType returnType,
                                          final Operator operator, final boolean updateTableDef) {
        if (operator != Operator.EQUAL && operator != Operator.NOT_EQUAL) {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }

        final String columnIdentity = getColumnIdentity(column, updateTableDef, true);
        final boolean isNotNullCheck = operator == Operator.NOT_EQUAL;

        if (returnType.isArray()) {
            builder.append(isNotNullCheck ? "NOT empty(" : "empty(").append(columnIdentity).append(")");
            return;
        }

        if (returnType.isJson()) {
            builder.append(columnIdentity).append(isNotNullCheck ? " != '{}'" : " = '{}'");
            return;
        }

        if (returnType.isString() || returnType.isFilterableString()) {
            builder.append(columnIdentity).append(isNotNullCheck ? " != ''" : " = ''");
            return;
        }

        if (returnType.isNumeric()) {
            builder.append(columnIdentity).append(isNotNullCheck ? " IS NOT NULL" : " IS NULL");
            return;
        }

        throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
    }

    /**
     * Converts a JSON comparator value to its numeric representation.
     * Handles single strings, string pairs (for BETWEEN), and iterables.
     *
     * @param value    the raw value from the criteria (String, String[], or Iterable)
     * @param operator the comparison operator
     * @return the converted numeric value (Number, Number[], or List&lt;Number&gt;)
     */
    private Object getJsonNumericValue(final Object value, final Operator operator) {
        if (value instanceof final String stringValue) {
            return parseNumber(stringValue);
        }

        if (value instanceof final String[] pair) {

            if (pair.length != 2 || (operator != Operator.BETWEEN && operator != Operator.NOT_BETWEEN)) {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }

            return new Number[]{parseNumber(pair[0]), parseNumber(pair[1])};
        }

        if (value instanceof final Iterable<?> iterable) {
            final List<Number> parsedValues = new ArrayList<>();

            for (final Object item : iterable) {

                if (item instanceof final Number number) {
                    parsedValues.add(number);
                } else if (item != null) {
                    parsedValues.add(parseNumber(item.toString()));
                } else {
                    throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
                }
            }
            return parsedValues;
        }

        return value;
    }

    /**
     * Parses a string value into a {@link Number} (Long or Double).
     *
     * @param value the string to parse
     * @return the parsed number
     * @throws com.tlc.commons.code.impl.DefaultErrorCode if the value is null, empty, or not a valid number
     */
    private Number parseNumber(final String value) {
        final String parsedValue = value == null ? null : value.trim();

        if (parsedValue == null || parsedValue.isEmpty()) {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }

        try {
            if (parsedValue.contains(".") || parsedValue.contains("e") || parsedValue.contains("E")) {
                return Double.parseDouble(parsedValue);
            }

            return Long.parseLong(parsedValue);
        } catch (final NumberFormatException ex) {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR, ex);
        }
    }

    /**
     * Handles comparisons for Array-typed columns in ClickHouse.
     * Routes HAS_ANY/NOT_HAS_ANY to loadHasAnyComparator,
     * otherwise dispatches by the array's element type.
     *
     * @param column the Column object
     * @param value the comparator value
     * @param builder the StringBuilder for appending SQL
     * @param operator the comparison operator
     * @param updateTableDef whether to update the table definition
     */
    private void loadArrayComparator(final Column column, final Object value, final StringBuilder builder,
                                     final Operator operator, final boolean updateTableDef) {
        if (operator == Operator.HAS_ANY || operator == Operator.NOT_HAS_ANY) {
            loadHasAnyComparator(column, value, builder, operator, updateTableDef);
        } else {
            final ArrayColumnDefinition columnDefinition = (ArrayColumnDefinition) column.getColumnDefinition();
            final DataType elementType = columnDefinition.getElementType();

            if (elementType.isFilterableString() || elementType.isString()) {
                loadStringComparatorValue(column, value, builder, operator, updateTableDef);
            } else if (elementType.isNumeric()) {
                loadNumberComparatorValue(column, value, builder, operator, updateTableDef);
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
            }
        }
    }

    /**
     * Generates ClickHouse hasAny(column, [values]) or NOT hasAny(column, [values]) SQL.
     * Resolves element type from either ArrayColumnDefinition or JsonColumn.
     *
     * @param column the Column object
     * @param value the comparator value
     * @param builder the StringBuilder for appending SQL
     * @param operator the comparison operator
     * @param updateTableDef whether to update the table definition
     */
    private void loadHasAnyComparator(final Column column, final Object value, final StringBuilder builder,
                                      final Operator operator, final boolean updateTableDef) {
        if (value == null) {
            final String columnIdentity = getColumnIdentity(column, updateTableDef, true);

            if (operator == Operator.HAS_ANY) {
                builder.append("empty(").append(columnIdentity).append(")");
                return;
            } else if (operator == Operator.NOT_HAS_ANY) {
                builder.append("NOT empty(").append(columnIdentity).append(")");
                return;
            }

            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }

        if (!(value instanceof final Iterable<?> collection)) {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }

        final Iterator<?> itr = collection.iterator();

        if (!itr.hasNext()) {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }

        if (operator == Operator.NOT_HAS_ANY) {
            builder.append("NOT ");
        }

        builder.append("hasAny(");
        builder.append(getColumnIdentity(column, updateTableDef, true));
        builder.append(", [");

        final DataType elementType;

        if (column.getColumnDefinition() instanceof final ArrayColumnDefinition arrayColumnDefinition) {
            elementType = arrayColumnDefinition.getElementType();
        } else if (column instanceof final JsonColumn jsonColumn) {
            elementType = jsonColumn.getReturnType();
        } else {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
        }

        do {
            final Object cValue = itr.next();

            if (elementType.isNumeric()) {
                builder.append(cValue);
            } else {
                final Object converted = elementType.getUnWrappedValue(cValue);
                builder.append(getFormattedString(converted, elementType));
            }

            if (itr.hasNext()) {
                builder.append(",");
            } else {
                break;
            }

        } while (true);

        builder.append("])");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getComparator(final Operator operator) {
        return switch (operator) {
            case CONTAINS, STARTS_WITH, ENDS_WITH -> " ILIKE ";
            case NOT_CONTAINS, NOT_STARTS_WITH, NOT_ENDS_WITH -> " NOT ILIKE ";
            default -> super.getComparator(operator);
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadStringComparatorValue(final Column column, final Object value, final StringBuilder builder,
                                             final Operator operator, final boolean updateTableDef) {
        if (operator == Operator.EQUAL || operator == Operator.NOT_EQUAL) {
            builder.append("lower(").append(getColumnIdentity(column, updateTableDef, true)).append(")");

            final DataType dataType = column.getColumnDefinition().getDataType();
            final String unWrappedValue = (String) dataType.getUnWrappedValue(value);
            final String lowerValue = unWrappedValue.toLowerCase();

            builder.append(getComparator(operator)).append(getFormattedString(lowerValue, dataType));
        } else if (operator == Operator.IN || operator == Operator.NOT_IN) {

            if (!(value instanceof final Iterable<?> collection)) {
                super.loadStringComparatorValue(column, value, builder, operator, updateTableDef);
                return;
            }

            final Iterator<?> itr = collection.iterator();

            if (!itr.hasNext()) {
                super.loadStringComparatorValue(column, value, builder, operator, updateTableDef);
                return;
            }

            final DataType dataType = column.getColumnDefinition().getDataType();

            builder.append("lower(").append(getColumnIdentity(column, updateTableDef, true)).append(")");
            builder.append(getComparator(operator));

            if (Objects.equals(CommonDataType.ARRAY, dataType)) {
                builder.append(" [ ");
            } else {
                builder.append(" ( ");
            }

            do {
                final Object cValue = itr.next();
                final String converted = (String) dataType.getUnWrappedValue(cValue);
                final String lowerVal = converted.toLowerCase();
                builder.append(getFormattedString(lowerVal, dataType));
                if (itr.hasNext()) {
                    builder.append(",");
                } else {
                    break;
                }
            } while (true);

            if (Objects.equals(CommonDataType.ARRAY, dataType)) {
                builder.append(" ] ");
            } else {
                builder.append(" ) ");
            }
        } else {
            super.loadStringComparatorValue(column, value, builder, operator, updateTableDef);
        }
    }
}