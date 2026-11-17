package com.tlc.sql.common.api.ddl.update.impl;

import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

import java.util.Objects;


/**
 * @author Abishek
 * @version 1.0
 */
public abstract class AddColumnNullable<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements AdvDDLAction<C, T, D> {

    private final C columnDefinition;
    private final T tableDefinition;

    protected AddColumnNullable(T tableDefinition, C columnDefinition) {
        this.columnDefinition = Objects.requireNonNull(columnDefinition);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getSQL(final D ddlHandler) {
        return new String[]{ddlHandler.getAddColumnSQL(tableDefinition, this.columnDefinition)};
    }

    @Override
    public String[] getRevertSQL(final D ddlHandler) {
        return new String[]{ddlHandler.getDropColumnSQL(tableDefinition, columnDefinition)};
    }
}