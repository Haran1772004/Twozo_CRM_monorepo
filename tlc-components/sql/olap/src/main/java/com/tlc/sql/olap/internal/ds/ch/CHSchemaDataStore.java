package com.tlc.sql.olap.internal.ds.ch;

import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.olap.internal.ds.OlapSchemaDataStore;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

/**
 * ClickHouse-specific implementation of {@link OlapSchemaDataStore} that provides
 * schema-level operations for ClickHouse database tables.
 */
public class CHSchemaDataStore extends OlapSchemaDataStore<CHTableDefinition, CHDDLHandler> {

    public CHSchemaDataStore(final DsProvider dsProvider, final CHDDLHandler chddlHandler) {
        super(dsProvider, chddlHandler);
    }
}