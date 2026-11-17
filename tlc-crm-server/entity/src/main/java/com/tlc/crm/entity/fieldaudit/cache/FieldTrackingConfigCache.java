package com.tlc.crm.entity.fieldaudit.cache;

import com.tlc.crm.entity.fieldaudit.cache.internal.FieldTrackingConfigCacheImpl;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;


/**
 * Cache provider for {@link FieldTrackingConfig} entries keyed by (orgId, sourceId).
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface FieldTrackingConfigCache {

    /**
     * Gets the {@link FieldTrackingConfigCache} instance.
     */
    static FieldTrackingConfigCache getInstance() {
        return FieldTrackingConfigCacheImpl.getInstance();
    }

    /**
     * Returns the cached config for the given org and source, or {@code null} if not cached.
     */
    FieldTrackingConfig get(Long orgId, short sourceId);

    /**
     * Puts the config into the cache.
     */
    void put(Long orgId, short sourceId, FieldTrackingConfig config);

    /**
     * Removes the cached config for the given org and source.
     */
    void invalidate(Long orgId, short sourceId);
}