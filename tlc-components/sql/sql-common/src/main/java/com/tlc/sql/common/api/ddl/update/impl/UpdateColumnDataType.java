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
public abstract class UpdateColumnDataType<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements AdvDDLAction<C, T, D> {

    private final T tableDefinition;
    private final C oldC;
    private final C columnDefinition;

    protected UpdateColumnDataType(T tableDefinition, C oldC, C columnDefinition) {
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
        this.oldC = Objects.requireNonNull(oldC);
        this.columnDefinition = Objects.requireNonNull(columnDefinition);
    }

    @Override
    public String[] getSQL(D ddlHandler) {
        return ddlHandler.getChangeColumnDataTypeSQL(tableDefinition, columnDefinition);
    }

    @Override
    public String[] getRevertSQL(D ddlHandler) {
        return ddlHandler.getChangeColumnDataTypeSQL(tableDefinition, oldC);
    }
}