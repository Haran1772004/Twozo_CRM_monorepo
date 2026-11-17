package com.tlc.sql.oltp.internal.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.ds.AbstractSchemaDataStore;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Default implementation of {@link OltpSchemaDataStore} that provides
 * OLTP schema operations including sequence management for PostgreSQL databases.
 */
public class OltpSchemaDataStoreImpl extends AbstractSchemaDataStore<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> implements OltpSchemaDataStore {

    public OltpSchemaDataStoreImpl(final DsProvider dsProvider, final OltpDDLHandler ddlHandler) {
        super(dsProvider, ddlHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeSequence(String sequenceName, int start, int incrementBy)
    {
        final String sql = ddlHandler.getInitializeSequenceSQL(sequenceName, start, incrementBy);
        try (Connection conn = dsProvider.getConnection(); Statement statement = conn.createStatement())
        {
            statement.execute(sql);
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_SEQUENCE_INITIALIZE_FAILED, exp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSequence(String sequenceName, int start, int incrementBy)
    {
        final String sql = ddlHandler.getCreateSequenceSQL(sequenceName, start, incrementBy);
        try (Connection conn = dsProvider.getConnection(); Statement statement = conn.createStatement())
        {
            statement.execute(sql);
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_SEQUENCE_CREATE_FAILED, exp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSequence(String sequenceName, int incrementBy)
    {
        final String sql = ddlHandler.getUpdateSequenceSQL(sequenceName, incrementBy);
        try (Connection conn = dsProvider.getConnection(); Statement statement = conn.createStatement())
        {
            statement.execute(sql);
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_SEQUENCE_UPDATE_FAILED, exp);
        }
    }

}