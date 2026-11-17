package com.tlc.sql.common.internal.dml;

import com.tlc.sql.common.api.dml.ExistsQuery;
import com.tlc.sql.common.api.dml.Table;

/**
 * This class represents the implementation of ExistsQuery.
 * It is used to build the ExistsQuery object and hold the properties
 * of the ExistsQuery like table, joins, where clause etc.
 *
 *
 * @author Sathish
 * @version 1.0
 */
public class ExistsQueryImpl extends MultiQueryImpl implements ExistsQuery {

    public ExistsQueryImpl(final Table table) {
        super(table);
    }
}
