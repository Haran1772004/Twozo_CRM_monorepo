package com.tlc.sql.oltp.internal.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.ds.AbstractAdminDataStore;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.oltp.internal.handler.OltpDMLHandler;
import org.javatuples.Pair;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

public class OltpAdminDataStoreImpl extends AbstractAdminDataStore<OltpDMLHandler> implements OltpAdminDataStore {

    public OltpAdminDataStoreImpl(final Map<DsProviderType, DsProvider> dsProvider, final OltpDMLHandler dmlHandler, final OltpDDLHandler ddlHandler) {
        super(dsProvider, dmlHandler);
    }

    @Override
    public long getSequenceNextValue(String sequenceName)
    {
        final String sql = dmlHandler.getNextValueSequenceSQL(sequenceName);
        try (Connection conn = getConnection(); Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery(sql))
        {
            resultSet.next();
            return resultSet.getLong(1);
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_SEQUENCE_LOOKUP_FAILED, exp);
        }
    }

    @Override
    public Pair<Long, Long> getSequenceNextAndLimitValue(String sequenceName)
    {
        final String sql = dmlHandler.getNextValueLimitSequenceSQL(sequenceName);
        try (Connection conn = getConnection(); Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery(sql))
        {
            if(resultSet.next())
            {
                final Long nextValue = resultSet.getLong(1);
                final Long limit = resultSet.getLong(2);
                return Pair.with(nextValue, limit);
            }
            else
            {
                throw ErrorCode.get(SQLErrorCodes.DB_SEQUENCE_LOOKUP_FAILED);
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_SEQUENCE_LOOKUP_FAILED, exp);
        }
    }

}