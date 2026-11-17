package com.tlc.sql.olap.api.meta;

import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.meta.TableType;
import com.tlc.sql.olap.internal.meta.engine.OlapTableEngineConfiguration;
import com.tlc.sql.olap.internal.meta.OlapTableMetaInput;

/**
 * OLAP (ClickHouse) implementation of {@link TableDefinition}.
 */
public class OlapTableDefinition extends TableDefinition<OlapColumnDefinition> {

    public <E extends OlapTableEngineConfiguration, M extends OlapTableMetaInput<E>> OlapTableDefinition(final String name,
                                                                                                         final TableType type,
                                                                                                         final int seqId,
                                                                                                         final M metaInput) {
        super(name, type, seqId, metaInput);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link DatabaseType#OLAP}
     */
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.OLAP;
    }
}