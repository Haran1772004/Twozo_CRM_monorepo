package com.tlc.sql.olap.internal.handler;

import com.tlc.sql.common.api.handler.AbstractDMLHandler;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.olap.resolver.OlapTableResolver;

/**
 * @author Sundar
 * @version 1.0
 *
 * Abstract implementation of the OLAP-specific DML handler.
 * Provides methods for handling OLAP columns and resolving table definitions.
 */
public abstract class AbstractOlapDMLHandler extends AbstractDMLHandler implements OlapDMLHandler {

    private final TableResolver tableResolver;

    /**
     * Constructs an AbstractOlapDMLHandler and initializes the table resolver.
     */
    protected AbstractOlapDMLHandler() {
        this.tableResolver = OlapTableResolver.get();
    }

    /**
     * Retrieves the table resolver for OLAP tables.
     *
     * @return the table resolver instance
     */
    @Override
    public TableResolver getTableResolver() {
        return tableResolver;
    }
}