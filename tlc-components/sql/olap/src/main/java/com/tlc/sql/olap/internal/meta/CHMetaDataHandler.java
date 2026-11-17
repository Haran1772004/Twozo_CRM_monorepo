package com.tlc.sql.olap.internal.meta;

import com.tlc.sql.common.api.meta.handler.AbstractMetaDataHandler;
import com.tlc.sql.common.api.meta.MetaCache;
import com.tlc.sql.common.api.parser.TableDefinitionLoader;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.ds.OlapAdminDataStore;
import com.tlc.sql.olap.internal.ds.ch.CHSchemaDataStore;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.parser.ch.CHMetaDataParser;
import com.tlc.sql.olap.resolver.OlapTableResolver;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class CHMetaDataHandler extends AbstractMetaDataHandler<OlapColumnDefinition, CHTableDefinition, CHDDLHandler, OlapAdminDataStore, CHSchemaDataStore> {

    private final MetaCache<CHTableDefinition> olapMetaCache;

    public CHMetaDataHandler(final OlapAdminDataStore adminDataStore, final CHSchemaDataStore schemaDataStore) {
        super(adminDataStore, schemaDataStore, OlapTableResolver.get());
        this.olapMetaCache = CHMetaCache.get();
    }

    public Map<String, CHTableDefinition> fetchTableDefinitions(InputStream inputStream) {
        final Map<String, CHTableDefinition> definitions = new LinkedHashMap<>();
        final TableDefinitionLoader<OlapColumnDefinition, CHTableDefinition> definitionProvider = new TableDefinitionLoader<>() {
            final MetaCache<CHTableDefinition> metaCache = CHMetaCache.get();

            @Override
            public void add(CHTableDefinition tableDefinition) {
                definitions.put(tableDefinition.getName(), tableDefinition);
            }

            @Override
            public boolean isDuplicate(String tableName) {
                return resolve(tableName) != null;
            }

            @Override
            public CHTableDefinition resolve(String tableName) {
                final CHTableDefinition definition = definitions.get(tableName);
                return (definition == null ? metaCache.get(tableName) : definition);
            }

            @Override
            public boolean loadKeys() {
                return TableDefinitionLoader.super.loadKeys();
            }
        };
        CHMetaDataParser.getInstance().processMetaData(inputStream, definitionProvider);
        return definitions;
    }

    @Override
    protected MetaCache<CHTableDefinition> getMetaCache() {
        return olapMetaCache;
    }
}