package com.tlc.sql.common.api.ddl.update.impl;

import com.tlc.sql.common.ddl.DDLAction;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class DropColumn<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements DDLAction<C, T, D> {

    private final C columnDefinition;
    private final T tableDefinition;

    protected DropColumn(T tableDefinition, C columnDefinition) {
        this.columnDefinition = Objects.requireNonNull(columnDefinition);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getSQL(D ddlHandler) {
        return new String[]{ddlHandler.getDropColumnSQL(tableDefinition, columnDefinition)};
    }
}