package com.tlc.sql.common.api.ds;

import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.common.internal.ds.WritableDataStoreImpl;

import java.sql.Connection;
import java.util.Map;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class AbstractAdminDataStore<D extends DMLHandler> extends WritableDataStoreImpl implements AdminDataStore {

    protected final D dmlHandler;

    protected AbstractAdminDataStore(Map<DsProviderType, DsProvider> dsProvider, D dmlHandler) {
        super(dsProvider, dmlHandler);
        this.dmlHandler = Objects.requireNonNull(dmlHandler);
    }

    @Override
    public Connection getConnection() {
        return super.getConnection();
    }
}