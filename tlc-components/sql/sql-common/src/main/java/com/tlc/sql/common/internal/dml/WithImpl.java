package com.tlc.sql.common.internal.dml;

import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.With;

import java.util.Collection;
import java.util.List;

/**
 * Default implementation of {@link With} that holds a list of derived tables (CTEs)
 * and the main select query that references them.
 */
public class WithImpl implements With {

    private final List<DerivedTable> derivedTables;
    private final SelectQuery selectQuery;

    public WithImpl(final List<DerivedTable> derivedTables, final SelectQuery selectQuery) {
        this.derivedTables = derivedTables;
        this.selectQuery = selectQuery;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<DerivedTable> getDerivedTables() {
        return derivedTables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SelectQuery getSelectQuery() {
        return selectQuery;
    }
}