package com.tlc.sql.common.api.ddl.update.impl;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.ddl.AdvDDLAction;

/**
 * {@inheritDoc}
 *
 * @author Sundar
 * @version 1.0
 */
public abstract class IncreaseDecimalColumnWeight<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements AdvDDLAction<C, T, D> {

    private final T tableDefinition;
    private final C columnDefinition;
    private final C oldC;

    protected IncreaseDecimalColumnWeight(T tableDefinition, C columnDefinition,
                                       C oldC) {
        this.tableDefinition = tableDefinition;
        this.columnDefinition = columnDefinition;
        this.oldC = oldC;
    }

    /**
     * {@inheritDoc}
     *
     * @param ddlHandler Data definition handler
     * @return The SQL queries to increase decimal weight
     */
    @Override
    public String[] getSQL(final D ddlHandler) {
        return ddlHandler.getIncreaseDecimalWeightSQL(tableDefinition, columnDefinition);
    }

    /**
     * {@inheritDoc}
     *
     * @param ddlHandler Data definition handler
     * @return The SQL queries to revert action
     */
    @Override
    public String[] getRevertSQL(final D ddlHandler) {
        return ddlHandler.getIncreaseDecimalWeightSQL(tableDefinition, oldC);
    }
}