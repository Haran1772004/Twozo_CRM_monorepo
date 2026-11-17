package com.tlc.analytics.cache.table;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.crm.common.cache.LocalCache;
import com.tlc.analytics.cache.internal.TableDetailCacheImpl;

import java.util.Collection;

/**
 * Cache interface for {@link TableDetail} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface TableDetailCache extends LocalCache {

    /**
     * <p>
     * Gets the singleton instance of {@link TableDetailCache}.
     * </p>
     *
     * @return The {@link TableDetailCache} instance.
     */
    static TableDetailCache getInstance() {
        return TableDetailCacheImpl.getInstance();
    }

    /**
     * <p>
     * Synchronously retrieves cached table details for the given source/module ID.
     * </p>
     *
     * @param moduleId The module identifier.
     * @return The cached collection of {@link TableDetail} objects.
     */
    Collection<TableDetail> get(final Integer moduleId);

    /**
     * <p>
     * Synchronously retrieves a single {@link TableDetail} by its table ID.
     * </p>
     *
     * @param tableId The table identifier.
     * @return The cached {@link TableDetail} object, or null if not found.
     */
    TableDetail getById(final Long tableId);

    /**
     * <p>
     * Puts table details into cache for the given source/module ID.
     * </p>
     *
     * @param moduleId     The module identifier.
     * @param tableDetails The collection of {@link TableDetail} to cache.
     */
    void put(final Integer moduleId, final Collection<TableDetail> tableDetails);

    /**
     * <p>
     * Puts a single {@link TableDetail} into cache by its table ID.
     * </p>
     *
     * @param tableId      The table identifier.
     * @param tableDetail The {@link TableDetail} to cache.
     */
    void putById(final Long tableId, final TableDetail tableDetail);
}
