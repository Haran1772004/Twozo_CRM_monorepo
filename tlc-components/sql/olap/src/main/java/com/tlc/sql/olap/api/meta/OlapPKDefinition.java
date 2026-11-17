package com.tlc.sql.olap.api.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.PKDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

public class OlapPKDefinition implements PKDefinition {

    private final String columnName;

    public OlapPKDefinition(String columnName) {
        this.columnName = columnName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        return columnName;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code false} always, as OLAP tables do not use sequence generators
     */
    @Override
    public boolean hasSequenceGenerator() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.tlc.commons.code.impl.DefaultErrorCode always, as sequence generation is not supported for OLAP tables
     */
    @Override
    public long getNextSequence() {
        throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
    }
}