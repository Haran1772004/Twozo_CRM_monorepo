package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.dml.update.AbstractDMLUpdateManager;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.meta.handler.MetaDataHandler;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.resolver.OlapTableResolver;

public class CHDMLUpdateManager extends AbstractDMLUpdateManager<OlapColumnDefinition, CHTableDefinition> {

    private final TableResolver tableResolver;

    public CHDMLUpdateManager(final MetaDataHandler<OlapColumnDefinition, CHTableDefinition> metaDataHandler,
                              final AdminDataStore adminDataStore) {
        super(metaDataHandler, adminDataStore);
        this.tableResolver = OlapTableResolver.get();
    }

    @Override
    protected TableResolver getTableResolver() {
        return tableResolver;
    }
}