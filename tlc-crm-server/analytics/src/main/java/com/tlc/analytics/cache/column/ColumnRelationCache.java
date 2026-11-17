package com.tlc.analytics.cache.column;

import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.crm.common.cache.LocalCache;
import com.tlc.analytics.cache.internal.ColumnRelationCacheImpl;

import java.util.Collection;

/**
 * Cache interface for {@link ColumnRelation} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ColumnRelationCache extends LocalCache {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnRelationCache}.
     * </p>
     *
     * @return The {@link ColumnRelationCache} instance
     */
    static ColumnRelationCache getInstance() {
        return ColumnRelationCacheImpl.getInstance();
    }

    /**
     * <p>
     * Synchronously retrieves cached related columns for the given relation ID.
     * </p>
     *
     * @param relationId The relation identifier
     * @return The cached {@link ColumnRelation} collection, or null if not found
     */
    Collection<ColumnRelation> get(final Long relationId);

    /**
     * <p>
     * Synchronously retrieves a single {@link ColumnRelation} by its ID.
     * </p>
     *
     * @param columnRelationId The related column identifier
     * @return The cached {@link ColumnRelation} object, or null if not found
     */
    ColumnRelation getById(final Long columnRelationId);

    /**
     * <p>
     * Synchronously puts related columns into cache for the given relation ID.
     * </p>
     *
     * @param relationId      The relation identifier
     * @param columnRelations The collection of {@link ColumnRelation} to cache
     */
    void put(final Long relationId, final Collection<ColumnRelation> columnRelations);

    /**
     * <p>
     * Synchronously puts a single {@link ColumnRelation} into cache by its ID.
     * </p>
     *
     * @param columnRelationId The related column identifier
     * @param columnRelation   The {@link ColumnRelation} to cache
     */
    void putById(final Long columnRelationId, final ColumnRelation columnRelation);
}
