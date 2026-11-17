package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.internal.dml.ExistsQueryImpl;

/**
 * This interface represents the ExistsQuery.
 * It is used to build the ExistsQuery object and hold the properties
 * of the ExistsQuery like table, joins, where clause etc.
 *
 * @author Sathish
 * @version 1.0
 */
public interface ExistsQuery extends MultiQuery {

    static ExistsQuery get(final Table table) {
        return new ExistsQueryImpl(table);
    }
}