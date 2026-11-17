package com.tlc.sql.oltp.internal.meta;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.sql.common.api.meta.MetaCache;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;

import java.util.Map;
import java.util.Set;

public class OltpMetaCache implements MetaCache<OltpTableDefinition> {

    private static final MetaCache<OltpTableDefinition> INSTANCE;

    static {
        INSTANCE = new OltpMetaCache();
    }

    private final Cache<String, OltpTableDefinition> tableDefinitions;

    private OltpMetaCache() {
        this.tableDefinitions = CacheManager.getInstance().createCache();
    }

    public static MetaCache<OltpTableDefinition> get() {
        return INSTANCE;
    }

    public void put(final Map<String, OltpTableDefinition> tableDefinitions) {
        this.tableDefinitions.putAll(tableDefinitions);
    }

    public void put(final OltpTableDefinition tableDefinition) {
        tableDefinitions.put(tableDefinition.getName(), tableDefinition);
    }

    public void remove(final String tableName) {
        tableDefinitions.remove(tableName);
    }

    public void remove(final Set<String> tableNames) {
        tableNames.forEach(tableDefinitions::remove);
    }

    public OltpTableDefinition get(String tableName) {
        return tableDefinitions.get(tableName);
    }
}
