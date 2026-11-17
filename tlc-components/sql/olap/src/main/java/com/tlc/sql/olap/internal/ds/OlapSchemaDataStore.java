package com.tlc.sql.olap.internal.ds;

import com.tlc.sql.common.api.ds.AbstractSchemaDataStore;
import com.tlc.sql.common.api.ds.SchemaDataStore;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapTableDefinition;
import com.tlc.sql.olap.internal.handler.OlapDDLHandler;

/**
 * Abstract OLAP-specific extension of {@link AbstractSchemaDataStore} that provides
 * schema-level operations for OLAP (ClickHouse) database tables.
 */
public abstract class OlapSchemaDataStore<T extends OlapTableDefinition, D extends OlapDDLHandler<T>> extends AbstractSchemaDataStore<OlapColumnDefinition, T, D>  implements SchemaDataStore<OlapColumnDefinition, T, D> {

    protected OlapSchemaDataStore(final DsProvider dsProvider, final D ddlHandler) {
        super(dsProvider, ddlHandler);
    }
}