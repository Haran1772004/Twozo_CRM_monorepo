package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.meta.handler.MetaDataHandler;
import com.tlc.sql.common.api.dml.update.AbstractDMLUpdateManager;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.ds.OltpAdminDataStore;
import com.tlc.sql.oltp.resolver.OltpTableResolver;

public class OltpDMLUpdateManager extends AbstractDMLUpdateManager<OltpColumnDefinition, OltpTableDefinition> {

    private final TableResolver tableResolver;

    public OltpDMLUpdateManager(final MetaDataHandler<OltpColumnDefinition, OltpTableDefinition> metaDataHandler,
                                final OltpAdminDataStore adminDataStore) {
        super(metaDataHandler, adminDataStore);
        this.tableResolver = OltpTableResolver.get();
    }

    @Override
    protected TableResolver getTableResolver() {
        return tableResolver;
    }
}