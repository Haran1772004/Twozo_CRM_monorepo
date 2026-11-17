package com.tlc.sql.oltp.internal.pgsql;

import com.tlc.sql.common.api.meta.ArrayColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.DecimalColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.AbstractOltpDDLHandler;
import com.tlc.sql.oltp.internal.util.PgsqlDdlDmlUtil;

public class PgsqlDDLHandler extends AbstractOltpDDLHandler
{
    private static final String LENGTH_CHECK_CONSTRAINT_NAME_FORMAT = "%s_LENGTH_AUTO";
    PgsqlDDLHandler()
    {
    }

    @Override
    protected String getColumnSQL(OltpColumnDefinition definition)
    {
        final StringBuilder builder = new StringBuilder();
        builder.append(getColumnName(definition));
        builder.append(" ");
        builder.append(getDataType(definition, definition.getMaxLength()));
        builder.append(definition.isNullable() ? " NULL " : " NOT NULL ");

        final DataType dataType = definition.getDataType();
        if(dataType.isFilterableString())
        {
            builder.append(" ").append(getLengthCheckConstraint(definition, String.format(LENGTH_CHECK_CONSTRAINT_NAME_FORMAT, definition.getColumnName())));
        }
        return builder.toString();
    }

    @Override
    public String[] getUpdateColumnLengthSQL(OltpTableDefinition tableDefinition, OltpColumnDefinition columnDefinition)
    {
        final String constraintName = String.format(LENGTH_CHECK_CONSTRAINT_NAME_FORMAT, columnDefinition.getColumnName());
        final String tableName = getTableName(tableDefinition);
        return new String[] { "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName,
                "ALTER TABLE " + tableName + " ADD " + getLengthCheckConstraint(columnDefinition, constraintName)};
    }

    @Override
    public String getChangeColumnNotNullableSQL(OltpTableDefinition tableDefinition, OltpColumnDefinition columnDefinition)
    {
        return "ALTER TABLE " + getTableName(tableDefinition) + " ALTER COLUMN " + getColumnName(columnDefinition)
                + " SET NOT NULL";
    }

    @Override
    public String getChangeColumnNullableSQL(OltpTableDefinition tableDefinition, OltpColumnDefinition columnDefinition)
    {
        return "ALTER TABLE " + getTableName(tableDefinition) + " ALTER COLUMN " + getColumnName(columnDefinition)
                + " DROP NOT NULL";
    }

    @Override
    public String[] getChangeColumnDataTypeSQL(OltpTableDefinition tableDefinition, OltpColumnDefinition columnDefinition)
    {
        final String tableName = getTableName(tableDefinition);
        final DataType dataType = columnDefinition.getDataType();
        if(dataType.isLargeString())
        {
            final String constraintName = String.format(LENGTH_CHECK_CONSTRAINT_NAME_FORMAT, columnDefinition.getColumnName());
            return new String[] { "ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS "+ constraintName,
                    " ALTER TABLE " + tableName + " ALTER COLUMN " + getColumnName(columnDefinition)+ " TYPE " + getDataType(columnDefinition, columnDefinition.getMaxLength())};
        }
        else if(dataType.isFilterableString())
        {
            final String constraintName = String.format(LENGTH_CHECK_CONSTRAINT_NAME_FORMAT, columnDefinition.getColumnName());
            return new String[] { "ALTER TABLE " + tableName + " ALTER COLUMN " + getColumnName(columnDefinition)+ " TYPE " + getDataType(columnDefinition, columnDefinition.getMaxLength()),
                    "ALTER TABLE "+ tableName +" ADD "+getLengthCheckConstraint(columnDefinition, constraintName)};
        }
        else
        {
            return new String[] { "ALTER TABLE " + tableName +" ALTER COLUMN " + getColumnName(columnDefinition)+ " TYPE " + getDataType(columnDefinition, columnDefinition.getMaxLength())};
        }
    }

    @Override
    protected String getDataType(OltpColumnDefinition definition, int maxLength) {
        final DataType dataType = definition.getDataType();

        if (dataType.isArray() && definition instanceof final ArrayColumnDefinition arrayDef) {
            return PgsqlDdlDmlUtil.getArrayColumnType(arrayDef.getElementType());
        }
        if (dataType.isDecimal() && definition instanceof final DecimalColumnDefinition decimalColumnDefinition) {
            return PgsqlDdlDmlUtil.getDecimalDataType(decimalColumnDefinition.getPrecision(), decimalColumnDefinition.getScale());
        }
        return PgsqlDdlDmlUtil.getDataType(dataType);
    }

    private String getLengthCheckConstraint(OltpColumnDefinition definition, String name)
    {
        return "CONSTRAINT " + name +
                " CHECK ( LENGTH (" +
                getColumnName(definition) +
                ") <= " +
                definition.getMaxLength() +
                ")";
    }

}