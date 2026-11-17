package com.tlc.sql.olap.internal.meta.engine.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.Getter;

import java.util.Collection;
import java.util.Objects;

public class SummingMergeTree implements CHTableEngine {

    private static final String NAME = "SummingMergeTree";

    @Getter
    private final Collection<ColumnDefinition> sumColumnDefinitions;

    public SummingMergeTree(final Collection<ColumnDefinition> columnDefinitions) {

        if (Objects.isNull(columnDefinitions) || columnDefinitions.isEmpty()) {
            throw ErrorCode.get(SQLErrorCodes.TABLE_ENGINE_INITIALIZATION_FAILED);
        }

        for (final ColumnDefinition columnDefinition : columnDefinitions) {
            final DataType dataType = columnDefinition.getDataType();

            if (!dataType.isNumeric()) {
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
            }
        }
        this.sumColumnDefinitions = columnDefinitions;
    }

    @Override
    public String getName() {
        final StringBuilder nameBuilder = new StringBuilder(NAME);

        nameBuilder.append("(");

        for (final ColumnDefinition sumColumnDefinition : sumColumnDefinitions) {
            nameBuilder.append(sumColumnDefinition.getColumnName()).append(", ");
        }
        return StringUtil.replaceSuffix(nameBuilder, NumericalConstant.TWO, ")").toString();
    }
}