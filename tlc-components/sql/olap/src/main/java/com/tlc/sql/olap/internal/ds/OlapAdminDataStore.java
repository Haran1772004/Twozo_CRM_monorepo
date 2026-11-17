package com.tlc.sql.olap.internal.ds;

import com.tlc.sql.common.api.ds.AbstractAdminDataStore;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.olap.internal.handler.OlapDMLHandler;

import java.util.Map;

public class OlapAdminDataStore extends AbstractAdminDataStore<OlapDMLHandler> {

    public OlapAdminDataStore(final Map<DsProviderType, DsProvider> dsProvider, final OlapDMLHandler dmlHandler) {
        super(dsProvider, dmlHandler);
    }

}