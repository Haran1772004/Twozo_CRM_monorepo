package com.tlc.sql.oltp.internal.meta;

import com.tlc.sql.oltp.api.meta.OltpPKDefinition;
import com.tlc.sql.common.api.meta.SequenceDefinition;
import com.tlc.sql.common.api.meta.MetaCache;
import com.tlc.sql.common.api.meta.handler.AbstractMetaDataHandler;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.ds.OltpAdminDataStore;
import com.tlc.sql.oltp.internal.ds.OltpSchemaDataStore;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.oltp.internal.parser.OltpMetaDataParser;
import com.tlc.sql.common.api.parser.TableDefinitionLoader;
import com.tlc.sql.oltp.internal.sequence.BatchSeqGeneratorImpl;
import com.tlc.sql.oltp.internal.sequence.SequenceGeneratorImpl;
import com.tlc.sql.oltp.resolver.OltpTableResolver;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class OltpMetaDataHandler extends AbstractMetaDataHandler<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler, OltpAdminDataStore, OltpSchemaDataStore> {

    private final MetaCache<OltpTableDefinition> oltpMetaCache;

    public OltpMetaDataHandler(final OltpAdminDataStore adminDataStore, final OltpSchemaDataStore schemaDataStore) {
        super(adminDataStore, schemaDataStore, OltpTableResolver.get());
        this.oltpMetaCache = OltpMetaCache.get();
    }

    public void loadMetaData(Map<String, OltpTableDefinition> tableDefinitions) {
        super.loadMetaData(tableDefinitions);
        initSequenceGenerator(tableDefinitions);
    }

    public void populateMetaData(Map<String, OltpTableDefinition> oltpTableDefinitions) {
        super.populateMetaData(oltpTableDefinitions);
        initSequenceGenerator(oltpTableDefinitions);
    }

    public Map<String, OltpTableDefinition> fetchTableDefinitions(InputStream inputStream) {
        final Map<String, OltpTableDefinition> definitions = new LinkedHashMap<>();
        final TableDefinitionLoader<OltpColumnDefinition, OltpTableDefinition> definitionProvider = new TableDefinitionLoader<>() {
            final MetaCache<OltpTableDefinition> metaCache = OltpMetaCache.get();

            @Override
            public void add(OltpTableDefinition tableDefinition) {
                definitions.put(tableDefinition.getName(), tableDefinition);
            }

            @Override
            public boolean isDuplicate(String tableName) {
                return resolve(tableName) != null;
            }

            @Override
            public OltpTableDefinition resolve(String tableName) {
                final OltpTableDefinition definition = definitions.get(tableName);
                return definition == null ? metaCache.get(tableName) : definition;
            }

            @Override
            public boolean loadKeys() {
                return TableDefinitionLoader.super.loadKeys();
            }
        };
        OltpMetaDataParser.getInstance().processMetaData(inputStream, definitionProvider);
        return definitions;
    }

    @Override
    protected MetaCache<OltpTableDefinition> getMetaCache() {
        return oltpMetaCache;
    }

    private void initSequenceGenerator(Map<String, OltpTableDefinition> tableDefinitions) {

        for (OltpTableDefinition tableDefinition : tableDefinitions.values()) {
            final OltpPKDefinition pkDefinition = tableDefinition.getPkDefinition();

            if (pkDefinition.hasSequenceGenerator()) {
                final SequenceDefinition sequenceDefinition = pkDefinition.getSequenceDefinition();
                final String sequenceName = sequenceDefinition.getSequenceName();

                if (sequenceDefinition.getIncrementBy() == 1) {
                    sequenceDefinition.setSequenceGenerator(new SequenceGeneratorImpl(sequenceName, adminDataStore));
                } else {
                    sequenceDefinition.setSequenceGenerator(new BatchSeqGeneratorImpl(sequenceName, adminDataStore));
                }
            }
        }
    }
}