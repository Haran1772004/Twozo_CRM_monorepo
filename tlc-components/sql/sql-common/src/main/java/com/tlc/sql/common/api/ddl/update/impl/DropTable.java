package com.tlc.sql.common.api.ddl.update.impl;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.ddl.DDLAction;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.api.meta.PKDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class DropTable<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements DDLAction<C, T, D> {

    private final T tableDefinition;

    protected DropTable(T tableDefinition) {
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getSQL(D ddlHandler) {
        final String sql = ddlHandler.getDropTableSQL(tableDefinition);
        final PKDefinition pkDefinition = tableDefinition.getPkDefinition();

        if (pkDefinition.hasSequenceGenerator()) {
            return new String[]{sql, getDropSequenceSQL(ddlHandler, pkDefinition)};
        } else {
            return new String[]{sql};
        }
    }

    protected abstract String getDropSequenceSQL(final D ddlHandler, final PKDefinition pkDefinition);
}