package com.tlc.sql.olap.internal.meta;

import com.tlc.sql.common.api.meta.TableMetaInput;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.meta.engine.OlapTableEngineConfiguration;

public abstract class OlapTableMetaInput<T extends OlapTableEngineConfiguration> extends TableMetaInput<T, OlapColumnDefinition> {

    protected OlapTableMetaInput(final String tableName, final T tableEngineConfiguration) {
        super(tableName, tableEngineConfiguration);
    }
}