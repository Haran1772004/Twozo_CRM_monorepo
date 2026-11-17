package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.secure.impl.CTESelectQueryImpl;

/**
 * Represents a SelectQuery specialized for Common Table Expressions (CTEs).
 * Implementations produced by this interface wrap a {@link DerivedTable} and expose a CTE-specific name accessor.
 *
 * @author Sathish
 * @version 1.0
 */
public interface CTESelectQuery extends SelectQuery {

    /**
     * Create a CTE-backed SelectQuery instance for the supplied derived table.
     *
     * @param table the derived table representing the CTE
     * @return a {@link CTESelectQuery} instance wrapping the provided table
     */
    static CTESelectQuery get(final DerivedTable table) {
        return new CTESelectQueryImpl(table);
    }

    /**
     * Return the CTE name used in SQL generation for this query.
     *
     * @return CTE alias/name
     */
    String getCteName();
}
