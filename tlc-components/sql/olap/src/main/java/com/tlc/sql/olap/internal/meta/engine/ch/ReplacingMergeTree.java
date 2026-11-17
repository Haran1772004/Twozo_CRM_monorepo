package com.tlc.sql.olap.internal.meta.engine.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.olap.api.meta.OlapDataType;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

public class ReplacingMergeTree implements CHTableEngine {

    private static final String NAME = "ReplacingMergeTree";

    @Getter
    private final ColumnDefinition versioningColumnDefinition;
    private final ColumnDefinition deleteFlagColumnDefinition;

    public ReplacingMergeTree(final ColumnDefinition versioningColumnDefinition) {
        this(versioningColumnDefinition, null);
    }

    public ReplacingMergeTree(final ColumnDefinition versioningColumnDefinition, final ColumnDefinition deleteFlagColumnDefinition) {
        final DataType dataType = Objects.requireNonNull(versioningColumnDefinition).getDataType();

        if (!(dataType.isUnsignedInteger() || dataType.isDate() || OlapDataType.DATETIME == dataType)) {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
        }
        this.versioningColumnDefinition = versioningColumnDefinition;

        if (Objects.nonNull(deleteFlagColumnDefinition) && !deleteFlagColumnDefinition.getDataType().isUnsignedInteger()) {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
        }
        this.deleteFlagColumnDefinition = deleteFlagColumnDefinition;
    }

    public Optional<ColumnDefinition> getDeleteFlagColumnDefinition() {
        return Optional.ofNullable(deleteFlagColumnDefinition);
    }

    @Override
    public String getName() {
        final StringBuilder nameBuilder = new StringBuilder(NAME);

        nameBuilder.append("(").append(versioningColumnDefinition.getColumnName());

        if (Objects.isNull(deleteFlagColumnDefinition)) {
            nameBuilder.append(")");
        } else {
            nameBuilder.append(", ").append(deleteFlagColumnDefinition.getColumnName()).append(")");
        }
        return nameBuilder.toString();
    }
}
