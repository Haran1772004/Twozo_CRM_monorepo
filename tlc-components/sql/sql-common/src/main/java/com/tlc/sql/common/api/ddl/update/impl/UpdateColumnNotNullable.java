package com.tlc.sql.common.api.ddl.update.impl;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.ddl.AdvDDLAction;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class UpdateColumnNotNullable<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements AdvDDLAction<C, T, D> {

    private final C columnDefinition;
    private final T tableDefinition;

    protected UpdateColumnNotNullable(T tableDefinition, C columnDefinition) {
        this.columnDefinition = Objects.requireNonNull(columnDefinition);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getSQL(D ddlHandler) {
        final String sql = ddlHandler.getChangeColumnNotNullableSQL(tableDefinition, columnDefinition);
        return new String[]{sql};
    }

    @Override
    public String[] getRevertSQL(D ddlHandler) {
        final String sql = ddlHandler.getChangeColumnNullableSQL(tableDefinition, columnDefinition);
        return new String[]{sql};
    }
}