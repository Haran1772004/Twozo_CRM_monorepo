package com.tlc.crm.entity.fieldaudit.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.crm.entity.fieldaudit.cache.FieldTrackingConfigCache;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * In-process cache for {@link FieldTrackingConfig} entries.
 *
 * <p>The tracking config table is small (one row per (org, sourceId)) and is read on every
 * write to a tracked record, so values are cached here and explicitly invalidated
 * on update.</p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class FieldTrackingConfigCacheImpl implements FieldTrackingConfigCache {

    private final Cache<CacheKey, FieldTrackingConfig> cache;

    /**
     * Private constructor for singleton initialization.
     *
     * <p>Initializes the unbounded thread-safe cache map for storing tracking configurations.</p>
     */
    private FieldTrackingConfigCacheImpl() {
        final CacheConfig<CacheKey, FieldTrackingConfig> cacheConfig = new CacheConfig<>();
        final long expireTime = TimeUnit.MINUTES.toMillis(2);

        cacheConfig.setExpireAfterAccess(expireTime);
        cache = CacheManager.getInstance().createCache(cacheConfig);
    }

    /**
     * Singleton instance holder using Bill Pugh pattern.
     */
    private static final class InstanceHolder {
        private static final FieldTrackingConfigCacheImpl INSTANCE = new FieldTrackingConfigCacheImpl();
    }

    /**
     * Retrieves the singleton instance of {@link FieldTrackingConfigCacheImpl}.
     *
     * @return the singleton instance
     */
    public static FieldTrackingConfigCacheImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Retrieves a field tracking configuration from the cache.
     *
     * <p>This method looks up the configuration using a composite key combining the
     * organization ID and entity source. Returns null if the key is not found in the cache.</p>
     *
     * @param orgId  the organization ID
     * @param sourceId the entity source (e.g., CONTACT, COMPANY, DEAL)
     * @return the {@link FieldTrackingConfig} for the org/source pair, or null if not cached
     */
    @Override
    public FieldTrackingConfig get(final Long orgId, final short sourceId) {
        return cache.get(new CacheKey(orgId, sourceId));
    }

    /**
     * Caches a field tracking configuration.
     * <p>This method stores the provided configuration in the cache using a composite key</p>
     *
     * @param orgId    the organization ID
     * @param sourceId the entity source (e.g., CONTACT, COMPANY, DEAL)
     * @param config   the {@link FieldTrackingConfig} to cache
     */
    @Override
    public void put(final Long orgId, final short sourceId, final FieldTrackingConfig config) {
        cache.put(new CacheKey(orgId, sourceId), config);
    }

    /**
     * Removes a field tracking configuration from the cache.
     *
     * @param orgId    the organization ID
     * @param sourceId the entity source (e.g., CONTACT, COMPANY, DEAL)
     */
    @Override
    public void invalidate(final Long orgId, final short sourceId) {
        cache.remove(new CacheKey(orgId, sourceId));
    }

    /**
     * Composite key for the cache, combining orgId and source.
     *
     * <p>This inner class implements proper {@link #equals(Object)} and {@link #hashCode()}
     * methods to provide consistent behavior in hash-based collections like ConcurrentHashMap.</p>
     */
    private static final class CacheKey {

        private final Long orgId;
        private final short sourceId;

        /**
         * Constructs a cache key from organization ID and entity source.
         *
         * @param orgId    the organization ID
         * @param sourceId the entity source
         */
        private CacheKey(final Long orgId, final short sourceId) {
            this.orgId = orgId;
            this.sourceId = sourceId;
        }

        /**
         * Checks equality based on both orgId and source.
         *
         * <p>Two cache keys are considered equal if they have the same organization ID
         * and entity source (compared by reference for enum Source).</p>
         *
         * @param object the object to compare with
         * @return {@code true} if both orgId and source are equal; {@code false} otherwise
         */
        @Override
        public boolean equals(final Object object) {
            if (this == object) return true;
            if (!(object instanceof CacheKey key)) return false;
            return Objects.equals(orgId, key.orgId) && sourceId == key.sourceId;
        }

        /**
         * Computes a hash code based on both orgId and source.
         *
         * <p>This method combined hashes of orgId and source to produce a consistent
         * hash code suitable for use in hash-based collections.</p>
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(orgId, sourceId);
        }
    }
}