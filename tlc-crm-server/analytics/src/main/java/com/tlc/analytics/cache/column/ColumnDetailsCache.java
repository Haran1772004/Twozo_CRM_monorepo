package com.tlc.analytics.cache.column;

import com.tlc.analytics.config.model.table.ColumnDetail;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.analytics.cache.internal.ColumnDetailsCacheImpl;

import java.util.Collection;

/**
 * Cache interface for {@link ColumnDetail} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ColumnDetailsCache extends LocalCache {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnDetailsCache}.
     * </p>
     *
     * @return The {@link ColumnDetailsCache} instance
     */
    static ColumnDetailsCache getInstance() {
        return ColumnDetailsCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves cached column details for the given source/module ID.
     * </p>
     *
     * @param tableId The table identifier
     * @return The cached collection of {@link ColumnDetail}, or {@code null} if not cached
     */
    Collection<ColumnDetail> get(final Long tableId);

    /**
     * <p>
     * Retrieves a single {@link ColumnDetail} by its column ID.
     * </p>
     *
     * @param columnId The column identifier
     * @return The cached {@link ColumnDetail}, or {@code null} if not cached
     */
    ColumnDetail getById(final Long columnId);

    /**
     * <p>
     * Puts column details into cache for the given source/module ID.
     * </p>
     *
     * @param tableId      The table identifier
     * @param columnDetails The collection of {@link ColumnDetail} to cache
     */
    void put(final Long tableId, final Collection<ColumnDetail> columnDetails);

    /**
     * <p>
     * Puts a single {@link ColumnDetail} into cache by its column ID.
     * </p>
     *
     * @param columnId      The column identifier
     * @param columnDetail The {@link ColumnDetail} to cache
     */
    void putById(final Long columnId, final ColumnDetail columnDetail);
}
