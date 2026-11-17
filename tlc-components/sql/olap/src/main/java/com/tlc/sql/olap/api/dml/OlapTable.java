package com.tlc.sql.olap.api.dml;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.sql.common.api.dml.AbstractTable;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.meta.MetaCache;
import com.tlc.sql.olap.api.meta.OlapTableDefinition;
import com.tlc.sql.olap.internal.meta.CHMetaCache;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

import java.util.concurrent.TimeUnit;

/**
 * @author Sundar
 * @version 1.0
 */
public class OlapTable extends AbstractTable {

    private static final Cache<String, Table> TABLE_CACHE;

    private static final MetaCache<CHTableDefinition> TABLE_DEFINITIONS;

    static {
        final CacheConfig<String, Table> config = new CacheConfig<>();
        config.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));
        TABLE_CACHE = CacheManager.getInstance().createCache(config);
        TABLE_DEFINITIONS = CHMetaCache.get();
    }

    /**
     * Retrieves a table instance from the cache or creates a new one if not present.
     *
     * @param name the name of the table
     * @return the table instance
     */
    public static Table get(final String name) {
        return TABLE_CACHE.computeIfAbsent(name, tableName -> new OlapTable(name, null));
    }

    /**
     * Constructor for creating an OlapTable with a specified name and alias.
     *
     * @param name  the name of the table
     * @param alias the alias for the table
     */
    public OlapTable(final String name, final String alias) {
        super(name, alias);
    }

    /**
     * Constructor for creating an OlapTable with a specified table definition and alias.
     *
     * @param tableDefinition the table definition for the OlapTable
     * @param alias           the alias for the table
     */
    public OlapTable(final OlapTableDefinition tableDefinition, final String alias) {
        super(tableDefinition, alias);
    }

    /**
     * Builds a column for the table with the specified parameters.
     *
     * @param columnName  the name of the column
     * @param columnAlias the alias for the column
     * @param isPkColumn  whether the column is a primary key column
     * @return the constructed column
     */
    @Override
    protected Column buildColumn(final String columnName, final String columnAlias, final boolean isPkColumn) {
        return new OlapColumn(this, columnName, columnAlias, isPkColumn);
    }

    /**
     * Retrieves the table definition for the specified table name.
     *
     * @param tableName the name of the table
     * @return the table definition
     */
    @Override
    protected OlapTableDefinition getTableDefinition(final String tableName) {
        return TABLE_DEFINITIONS.get(tableName);
    }

}