package com.tlc.sql.olap.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.*;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapDataType;
import com.tlc.sql.olap.api.meta.OlapDecimalColumnDefinition;

import java.util.Objects;

/**
 * @author Sundar
 * @version 1.0
 */
public class OlapColumn extends AbstractColumn {

    /**
     * Constructor for creating an OlapColumn with a table, column definition, and SQL function.
     *
     * @param table the table associated with the column
     * @param columnDefinition the column definition
     * @param function the SQL function applied to the column
     */
    public OlapColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function) {
        super(table, columnDefinition, function);
    }

    /**
     * Constructor for creating an OlapColumn with a table, column definition, SQL function, and PK flag.
     *
     * @param table the table associated with the column
     * @param columnDefinition the column definition
     * @param function the SQL function applied to the column
     * @param isPkColumn whether the source column is a primary key column
     */
    public OlapColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function, final boolean isPkColumn) {
        super(table, columnDefinition, function, isPkColumn);
    }


    /**
     * Constructor for creating an OlapColumn with table and column details.
     *
     * @param table the table associated with the column
     * @param columnName the name of the column
     * @param columnAlias the alias for the column
     * @param isPkColumn whether the column is a primary key
     */
    OlapColumn(final Table table, final String columnName, final String columnAlias, final boolean isPkColumn) {
        super(table, columnName, columnAlias, isPkColumn);
    }

    /**
     * Constructor for derived table columns - accepts column definition directly
     * to bypass table definition lookup which doesn't work for derived tables.
     *
     * @param table the table (can be a DerivedTable)
     * @param columnDefinition the column definition from the source column
     * @param columnName the column name
     * @param columnAlias the column alias
     */
    OlapColumn(final Table table, final ColumnDefinition columnDefinition, final String columnName, final String columnAlias) {
        super(table, columnDefinition, columnName, columnAlias);
    }

    /**
     * Constructor for wrapping an existing column with a different column type.
     *
     * @param column the source column
     * @param type the column type to apply
     */
    OlapColumn(final OlapColumn column, final Column.ColumnType type) {
        super(column, type);
    }

    /**
     * Constructor for creating an OlapColumn with a column and alias.
     *
     * @param column the source column
     * @param columnAlias the alias for the column
     */
    OlapColumn(final Column column, final String columnAlias) {
        super(column.getTable(), column.getColumnName(), columnAlias, false);
    }

    /**
     * Constructor that optionally bypasses table-definition lookup for derived table columns.
     * When {@code isDerivedTable} is true, uses the source column's existing {@link ColumnDefinition}
     * directly, avoiding {@code DB_ROW_UNKNOWN_COLUMN} when the derived table's physical definition
     * does not contain the column.
     *
     * @param column         the source column
     * @param columnAlias    the alias for this column
     * @param isDerivedTable true to bypass table-definition lookup
     */
    OlapColumn(final Column column, final String columnAlias, final boolean isDerivedTable) {
        super(column, columnAlias, isDerivedTable);
    }


    /**
     * {@inheritDoc}
     *
     * @param columnType the type of the column
     * @return the newly built column
     */
    @Override
    protected Column buildColumn(final Column.ColumnType columnType) {
        return new OlapColumn(this, columnType);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnAlias the alias for the JSON column
     * @return the newly created JSON column
     */
    @Override
    public JsonColumn jsonColumn(final String columnAlias) {
        return jsonColumn(columnAlias, null, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnAlias the alias for the JSON column
     * @param returnType the return type of the JSON column
     * @return the newly created JSON column
     */
    @Override
    public JsonColumn jsonColumn(final String columnAlias, final DataType returnType) {
        Objects.requireNonNull(returnType);
        return jsonColumn(columnAlias, returnType, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnAlias the alias for the JSON column
     * @param returnType the return type of the JSON column
     * @param isArray whether the JSON column is an array
     * @return the newly created JSON column
     */
    @Override
    public JsonColumn jsonColumn(final String columnAlias, final DataType returnType, final boolean isArray) {

        if (OlapDataType.JSON == getColumnDefinition().getDataType()) {

            if (Objects.nonNull(returnType)) {
                return isArray ? new OlapJsonColumn(this, columnAlias, returnType, true) : new OlapJsonColumn(this, columnAlias, returnType);
            } else {
                return new OlapJsonColumn(this, columnAlias);
            }
        }
        throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnAlias the alias for the JSON decimal column
     * @param precision the precision of the decimal column
     * @param scale the scale of the decimal column
     * @return the newly created JSON decimal column
     */
    @Override
    public JsonColumn jsonDecimalColumn(final String columnAlias, final int precision, final int scale) {
        return new OlapJsonColumn(this, columnAlias, precision, scale);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnAlias the alias for the JSON decimal column
     * @param precision the precision of the decimal column
     * @param scale the scale of the decimal column
     * @param isArray whether the JSON decimal column is an array
     * @return the newly created JSON decimal column
     */
    @Override
    public JsonColumn jsonDecimalColumn(final String columnAlias, final int precision, final int scale, final boolean isArray) {
        return new OlapJsonColumn(this, columnAlias, precision, scale, isArray);
    }

    /**
     * {@inheritDoc}
     *
     * @param table the table associated with the column
     * @param columnDefinition the column definition
     * @param function the SQL function applied to the column
     * @return the newly built function column
     */
    @Override
    protected Column buildFunctionColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function) {
        return new OlapColumn(table, columnDefinition, function);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Column buildFunctionColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function, final boolean isPkColumn) {
        return new OlapColumn(table, columnDefinition, function, isPkColumn);
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasName the alias name for the column definition
     * @param returnType the return type of the column definition
     * @return the newly built column definition
     */
    @Override
    protected ColumnDefinition buildColumnDefinition(final String aliasName, final DataType returnType) {
        return new OlapColumnDefinition(aliasName, returnType);
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasName the alias name for the decimal column definition
     * @return the newly built decimal column definition
     */
    @Override
    protected ColumnDefinition buildDecimalColumnDefinition(final String aliasName) {
        return new OlapDecimalColumnDefinition(aliasName);
    }

    /**
     * {@inheritDoc}
     *
     * @return the text data type for OLAP platform (TEXT)
     */
    @Override
    protected DataType getTextDataType() {
        return CommonDataType.TEXT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Column buildExpressionColumn(final Expression expression) {
        final OlapColumn column = new OlapColumn(this, ColumnType.EXPRESSION);

        column.setExpression(expression);

        return column;
    }
}
