package com.tlc.sql.olap.internal.meta;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.sql.common.api.meta.MetaCache;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

import java.util.Map;
import java.util.Set;

public class CHMetaCache implements MetaCache<CHTableDefinition> {

    private static final MetaCache<CHTableDefinition> INSTANCE;

    static {
        INSTANCE = new CHMetaCache();
    }

    private final Cache<String, CHTableDefinition> tableDefinitions;

    private CHMetaCache() {
        this.tableDefinitions = CacheManager.getInstance().createCache();
    }

    public static MetaCache<CHTableDefinition> get() {
        return INSTANCE;
    }

    @Override
    public void put(final Map<String, CHTableDefinition> olapTableDefinitions) {
        tableDefinitions.putAll(olapTableDefinitions);
    }

    public void put(final CHTableDefinition tableDefinition) {
        tableDefinitions.put(tableDefinition.getName(), tableDefinition);
    }

    public void remove(final String tableName) {
        tableDefinitions.remove(tableName);
    }

    public void remove(final Set<String> tableNames) {
        tableNames.forEach(tableDefinitions::remove);
    }

    public CHTableDefinition get(String tableName) {
        return tableDefinitions.get(tableName);
    }
}