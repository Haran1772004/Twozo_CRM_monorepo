package com.tlc.sql.olap.internal.dml;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.internal.dml.SelectQueryImpl;
import com.tlc.sql.olap.api.dml.OlapSelectQuery;

/**
 * OLAP-specific implementation of {@link OlapSelectQuery}.
 * <p>
 * Extends {@link SelectQueryImpl} with support for returning the latest version
 * of each record, used by ClickHouse's FINAL modifier for ReplacingMergeTree tables.
 * </p>
 */
public class OlapSelectQueryImpl extends SelectQueryImpl implements OlapSelectQuery {

    private boolean returnUpdatedData;

    /**
     * Constructs an OlapSelectQueryImpl for the given table with the latest record flag
     * defaulted to {@code true}.
     *
     * @param table the base table for the query
     */
    public OlapSelectQueryImpl(final Table table) {
        this(table, true);
    }

    /**
     * Constructs an OlapSelectQueryImpl for the given table.
     *
     * @param table             the base table for the query
     * @param returnUpdatedData {@code true} to return only the latest record version,
     *                          {@code false} otherwise
     */
    public OlapSelectQueryImpl(final Table table, final boolean returnUpdatedData) {
        super(table);
        this.returnUpdatedData = returnUpdatedData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void returnUpdatedData(final boolean returnUpdatedData) {
        this.returnUpdatedData = returnUpdatedData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReturnUpdatedData() {
        return this.returnUpdatedData;
    }
}
