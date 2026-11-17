package com.tlc.sql.olap.internal.meta.engine.ch;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.meta.ch.CHDataType;

import java.util.Collection;
import java.util.Objects;

public class AggregatingMergeTree implements CHTableEngine {

    private static final String NAME = "AggregatingMergeTree()";

    public AggregatingMergeTree(final Collection<OlapColumnDefinition> columnDefinitions) {

        if (Objects.isNull(columnDefinitions) || columnDefinitions.isEmpty()) {
            throw ErrorCode.get(SQLErrorCodes.TABLE_ENGINE_INITIALIZATION_FAILED);
        }
        final Counter aggregateFunctionColumnCounter = new Counter();

        for (final ColumnDefinition columnDefinition : columnDefinitions) {
            final DataType dataType = columnDefinition.getDataType();

            if (CHDataType.AGGREGATE_FUNCTION == dataType) {
                aggregateFunctionColumnCounter.increment();
            }
        }

        if (aggregateFunctionColumnCounter.getCount() == 0) {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
