package com.tlc.sql.oltp.api.meta;

import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.IndexDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.meta.TableType;
import com.tlc.sql.common.api.meta.UKDefinition;
import com.tlc.sql.oltp.internal.meta.OltpTableMetaInput;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * OLTP (PostgreSQL) implementation of {@link TableDefinition}.
 * Extends the base definition with unique key and index definitions.
 */
public class OltpTableDefinition extends TableDefinition<OltpColumnDefinition> {

    private final Map<String, UKDefinition> ukDefinitions;

    @Getter
    private final Map<String, IndexDefinition> indexDefinitions;

    public OltpTableDefinition(final String name, final TableType type, final int seqId, final OltpTableMetaInput metaInput) {
        super(name, type, seqId, metaInput);
        this.ukDefinitions = metaInput.getUkDefinitions();
        this.indexDefinitions = Map.copyOf(metaInput.getIndexDefinitions());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link DatabaseType#OLTP}
     */
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.OLTP;
    }

    /**
     * Returns the unique key definitions for this table.
     *
     * @return an unmodifiable map of unique key name to {@link UKDefinition}
     */
    public Map<String, UKDefinition> getUkDefinitions() {
        return Collections.unmodifiableMap(ukDefinitions);
    }

}