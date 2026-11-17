package com.tlc.analytics.cache.table;

import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.crm.common.cache.LocalCache;
import com.tlc.analytics.cache.internal.TableRelationCacheImpl;

import java.util.Collection;

/**
 * Cache interface for {@link TableRelation} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface TableRelationCache extends LocalCache {

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableRelationCache}.
     * </p>
     *
     * @return The {@link TableRelationCache} instance
     */
    static TableRelationCache getInstance() {
        return TableRelationCacheImpl.getInstance();
    }

    /**
     * <p>
     * Synchronously retrieves cached table relations for the given source/module ID.
     * </p>
     *
     * @param parentTableId The parent table identifier
     * @return The collection of cached {@link TableRelation} objects
     */
    Collection<TableRelation> get(final Long parentTableId);

    /**
     * <p>
     * Synchronously retrieves a single {@link TableRelation} by its ID.
     * </p>
     *
     * @param relationId The relation identifier
     * @return The cached {@link TableRelation} object, or null if not found
     */
    TableRelation getById(final Long relationId);

    /**
     * <p>
     * Synchronously puts table relations into cache for the given source/module ID.
     * </p>
     *
     * @param parentTableId       The parent table identifier
     * @param tableRelations The collection of {@link TableRelation} to cache
     */
    void put(final Long parentTableId, final Collection<TableRelation> tableRelations);

    /**
     * <p>
     * Synchronously puts a single {@link TableRelation} into cache by its ID.
     * </p>
     *
     * @param relationId    The relation identifier
     * @param tableRelation The {@link TableRelation} to cache
     */
    void putById(final Long relationId, final TableRelation tableRelation);
}
