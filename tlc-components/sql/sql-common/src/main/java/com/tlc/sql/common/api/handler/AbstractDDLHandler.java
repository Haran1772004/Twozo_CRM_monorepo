package com.tlc.sql.common.api.handler;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DecimalColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.util.DDLDMLUtil;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class AbstractDDLHandler<C extends ColumnDefinition, T extends TableDefinition<C>> implements DDLHandler<C, T> {

    @Override
    public String getAddColumnSQL(final T tableDefinition, final C columnDefinition) {
        final StringBuilder alterBuilder = new StringBuilder();
        alterBuilder.append("ALTER TABLE ").append(getTableName(tableDefinition)).append(" ADD ");
        final String columnSQL = getColumnSQL(columnDefinition);
        alterBuilder.append(columnSQL);
        return alterBuilder.toString();
    }

    protected String getTableName(TableDefinition<?> tableDefinition) {
        return DDLDMLUtil.getTableName(tableDefinition);
    }

    protected String getColumnName(ColumnDefinition columnDefinition) {
        return DDLDMLUtil.getColumnName(columnDefinition);
    }

    protected String getColumnName(String column) {
        return DDLDMLUtil.getColumnName(column);
    }

    @Override
    public String getDropColumnSQL(final T tableDefinition, final C columnDefinition) {
        return "ALTER TABLE " + getTableName(tableDefinition) + " DROP COLUMN " + getColumnName(columnDefinition);
    }

    @Override
    public String getDropTableSQL(final T tableDefinition) {
        return "DROP TABLE " + getTableName(tableDefinition);
    }

    protected abstract String getColumnSQL(final C definition);

    protected abstract String getDataType(C columnDefinition, int maxLength);

    protected String getCommonDataType(final CommonDataType dataType, final C columnDefinition) {

        return switch (dataType) {
            case SMALLINT -> "SMALLINT";
            case INTEGER -> "INT";
            case BIGINT -> "BIGINT";
            case DECIMAL -> {
                final DecimalColumnDefinition decimalColumnDefinition = (DecimalColumnDefinition) columnDefinition;

                yield "DECIMAL(" + decimalColumnDefinition.getPrecision() + "," + decimalColumnDefinition.getScale() + ")";
            }
            case BOOLEAN -> "BOOLEAN";
            case TEXT -> "TEXT";
            case ARRAY -> "ARRAY";
            case NULLABLE_BIGINT -> "NULLABLE(BIGINT)";
        };
    }
}