package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.SelectQuery;

/**
 * Serves as a wrapper for inner {@link SelectQuery}, enabling the storage of the refactored history of the {@link SelectQuery}
 *
 * @author sundar
 * @version 1.0
 */
public class InnerSelectQueryWrapper extends SelectQueryWrapper {

    private final SelectQuery query;

    public InnerSelectQueryWrapper(final SelectQuery selectQuery) {
        super(selectQuery);
        this.query = selectQuery;
    }

    /**
     * {@inheritDoc}
     *
     * @param column The column to be added
     */
    @Override
    public void addSelectClause(final Column column) {
        query.addSelectClause(column);
    }
}