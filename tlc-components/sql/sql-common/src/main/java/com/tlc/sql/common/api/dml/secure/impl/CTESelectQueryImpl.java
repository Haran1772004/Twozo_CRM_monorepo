package com.tlc.sql.common.api.dml.secure.impl;

import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.secure.CTESelectQuery;
import com.tlc.sql.common.internal.dml.SelectQueryImpl;

/**
 * Concrete SelectQuery implementation that wraps a {@link DerivedTable} and exposes the CTE name used during SQL generation.
 *
 * @author Sathish
 * @version 1.0
 */
public class CTESelectQueryImpl extends SelectQueryImpl implements CTESelectQuery {

    private final String cteName;

    /**
     * Create a new {@link CTESelectQueryImpl} for the provided derived table.
     *
     * @param table derived table representing the CTE
     */
    public CTESelectQueryImpl(final DerivedTable table) {
        super(table);
        this.cteName = table.getAlias();
    }

    /**
     * Return the alias used for the CTE.
     *
     * @return CTE alias/name
     */
    @Override
    public String getCteName() {
        return cteName;
    }
}
