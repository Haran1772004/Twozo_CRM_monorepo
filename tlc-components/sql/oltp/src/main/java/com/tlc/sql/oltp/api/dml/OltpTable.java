package com.tlc.sql.oltp.api.dml;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.sql.common.api.dml.AbstractTable;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.common.api.meta.MetaCache;
import com.tlc.sql.oltp.internal.meta.OltpMetaCache;

import java.util.concurrent.TimeUnit;

public class OltpTable extends AbstractTable {

    private static final Cache<String, OltpTable> TABLE_CACHE;

    private static final MetaCache<OltpTableDefinition> TABLE_DEFINITIONS;

    static {
        final CacheConfig<String, OltpTable> config = new CacheConfig<>();
        config.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));
        TABLE_CACHE = CacheManager.getInstance().createCache(config);
        TABLE_DEFINITIONS = OltpMetaCache.get();
    }

    public static OltpTable get(String name) {
        return TABLE_CACHE.computeIfAbsent(name, tableName -> new OltpTable(name, null));
    }

    public OltpTable(String name, String alias) {
        super(name, alias);
    }

    public OltpTable(OltpTableDefinition tableDefinition, String alias) {
        super(tableDefinition, alias);
    }

    @Override
    protected Column buildColumn(final String columnName, final String columnAlias, final boolean isPkColumn) {
        return new OltpColumn(this, columnName, columnAlias, isPkColumn);
    }

    @Override
    protected OltpTableDefinition getTableDefinition(final String tableName) {
        return TABLE_DEFINITIONS.get(tableName);
    }
}