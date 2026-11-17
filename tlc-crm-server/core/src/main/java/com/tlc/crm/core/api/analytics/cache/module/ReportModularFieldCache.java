package com.tlc.crm.core.api.analytics.cache.module;

import com.tlc.commons.listeners.Callback;
import com.tlc.crm.common.cache.RemoteCache;
import com.tlc.crm.core.api.analytics.cache.internal.ReportModularFieldCacheImpl;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;

import java.util.Collection;
import java.util.Map;

/**
 * Cache interface for {@link ReportModularField} objects using remote cache with TTL.
 * Uses composed keys ({@code "orgId:moduleId"}) for tenant isolation.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ReportModularFieldCache extends RemoteCache {

    /**
     * <p>Retrieves the singleton instance of {@link ReportModularFieldCache}.</p>
     *
     * @return The {@link ReportModularFieldCache} instance
     */
    static ReportModularFieldCache getInstance() {
        return ReportModularFieldCacheImpl.getInstance();
    }

    /**
     * <p>Asynchronously retrieves cached report modular fields for the given org.</p>
     *
     * @param orgId    The organization identifier 
     * @param callback The callback to handle the result
     */
    void getAsync(final Long orgId, final Callback<Collection<ReportModularField>> callback);

    /**
     * <p>Asynchronously retrieves a single {@link ReportModularField} by its ID (org-scoped).</p>
     *
     * @param orgId    The organization identifier
     * @param fieldId  The field identifier
     * @param callback The callback to handle the result
     */
    void getByIdAsync(final Long orgId, final Long fieldId, final Callback<ReportModularField> callback);

    /**
     * <p>Asynchronously puts report modular fields into cache for the given org.</p>
     *
     * @param orgId    The organization identifier 
     * @param fields   The collection of {@link ReportModularField} to cache
     * @param callback The callback to handle completion
     */
    void putAsync(final Long orgId, final Collection<ReportModularField> fields,
                  final Callback<Map.Entry<Long, Collection<ReportModularField>>> callback);

    /**
     * <p>Asynchronously puts a single {@link ReportModularField} into cache by its ID (org-scoped).</p>
     *
     * @param orgId    The organization identifier
     * @param fieldId  The field identifier
     * @param field    The {@link ReportModularField} to cache
     * @param callback The callback to handle completion
     */
    void putByIdAsync(final Long orgId, final Long fieldId, final ReportModularField field,
                      final Callback<Map.Entry<String, ReportModularField>> callback);

    /**
     * <p>Invalidates the cache for the given org.</p>
     *
     * @param orgId The organization identifier
     */
    void invalidateCache(final Long orgId);

    /**
     * <p>Invalidates the cached collection for the given org
     * and removes specific field entries from the byId cache.</p>
     *
     * @param orgId    The organization identifier
     * @param fieldIds The specific field IDs to remove from byId cache
     */
    void invalidateCache(final Long orgId, final Collection<Long> fieldIds);

    /**
     * <p>Invalidates only the cached collection for the given org.
     * Used when new entries are added and byId cache has no stale entries to remove.</p>
     *
     * @param orgId The organization identifier
     */
    void invalidateCacheBySourceId(final Long orgId);
}
