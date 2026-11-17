package com.tlc.sql.oltp.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.AbstractColumn;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Expression;
import com.tlc.sql.common.api.dml.JsonColumn;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpDataType;
import com.tlc.sql.oltp.api.meta.OltpDecimalColumnDefinition;

public class OltpColumn extends AbstractColumn {

    /**
     * Constructor for creating an OltpColumn with a table, column definition, and SQL function.
     *
     * @param table the table associated with the column
     * @param columnDefinition the column definition
     * @param function the SQL function applied to the column
     */
    public OltpColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function) {
        super(table, columnDefinition, function);
    }

    /**
     * Constructor for creating an OltpColumn with a table, column definition, SQL function, and PK flag.
     *
     * @param table the table associated with the column
     * @param columnDefinition the column definition
     * @param function the SQL function applied to the column
     * @param isPkColumn whether the source column is a primary key column
     */
    public OltpColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function, final boolean isPkColumn) {
        super(table, columnDefinition, function, isPkColumn);
    }

    /**
     * Package-private constructor for creating an OltpColumn with table and column details.
     *
     * @param oltpTable the OLTP table associated with the column
     * @param columnName the name of the column
     * @param columnAlias the alias for the column
     * @param isPkColumn whether the column is a primary key
     */
    OltpColumn(final Table oltpTable, final String columnName, final String columnAlias, final boolean isPkColumn) {
        super(oltpTable, columnName, columnAlias, isPkColumn);
    }


    /**
     * Constructor for wrapping an existing column with a different column type.
     *
     * @param column the source column
     * @param type the column type to apply
     */
    public OltpColumn(final OltpColumn column, final Column.ColumnType type) {
        super(column, type);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnType the type of the column
     * @return the newly built column
     */
    @Override
    protected Column buildColumn(final Column.ColumnType columnType) {
        return new OltpColumn(this, columnType);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnAlias the alias for the JSON column
     * @return the newly created JSON column
     */
    @Override
    public JsonColumn jsonColumn(final String columnAlias) {
        throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
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
        throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
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
        throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
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
        throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
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
        return new OltpColumn(table, columnDefinition, function);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Column buildFunctionColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function, final boolean isPkColumn) {
        return new OltpColumn(table, columnDefinition, function, isPkColumn);
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
        return new OltpColumnDefinition(aliasName, returnType);
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasName the alias name for the decimal column definition
     * @return the newly built decimal column definition
     */
    @Override
    protected ColumnDefinition buildDecimalColumnDefinition(final String aliasName) {
        return new OltpDecimalColumnDefinition(aliasName);
    }

    /**
     * {@inheritDoc}
     *
     * @return the text data type for OLTP platform (CHAR)
     */
    @Override
    protected DataType getTextDataType() {
        return OltpDataType.CHAR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Column buildExpressionColumn(final Expression expression) {
        final OltpColumn column = new OltpColumn(this, ColumnType.EXPRESSION);

        column.setExpression(expression);
        return column;
    }
}
