package com.tlc.crm.entity.cache.internal;

import com.tlc.crm.entity.cache.field.FieldCache;

/**
 * <p>
 * Acts a gateway to load the entity into cache
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class CacheLoader {

    private final FieldCache fieldCache;
    private final FieldCache secureFieldCache;
    private final FilterFieldCacheImpl filterFieldCache;
    private final ComparatorCacheImpl comparatorCache;
    private final DateIntervalCacheImpl dateIntervalCache;
    private final ProviderCacheImpl providerCache;
    private final FilterSourceCacheImpl filterSourceCache;
    private final SearchIndexCacheImpl searchIndexCache;
    private final SearchFieldCacheImpl searchFieldCache;

    private static class Instance {
        private static final CacheLoader INSTANCE = new CacheLoader();
    }

    public static CacheLoader getInstance() {
        return CacheLoader.Instance.INSTANCE;
    }

    private CacheLoader() {
        fieldCache = FieldCache.getInstance();
        secureFieldCache = FieldCache.getSecureInstance();
        filterFieldCache = FilterFieldCacheImpl.getInstance();
        comparatorCache = ComparatorCacheImpl.getInstance();
        dateIntervalCache = DateIntervalCacheImpl.getInstance();
        providerCache = ProviderCacheImpl.getInstance();
        filterSourceCache = FilterSourceCacheImpl.getInstance();
        searchIndexCache = SearchIndexCacheImpl.getInstance();
        searchFieldCache = SearchFieldCacheImpl.getInstance();
    }

    /**
     * <p>
     * Loads the field details into cache
     * </p>
     */
    public void load() {
        fieldCache.load();
        secureFieldCache.load();
        filterFieldCache.load();
        comparatorCache.load();
        dateIntervalCache.load();
        providerCache.load();
        filterSourceCache.load();
        searchIndexCache.load();
        searchFieldCache.load();
    }

    /**
     * <p>
     * Clears all the field related caches
     * </p>
     */
    public void clear() {
        ((AbstractFieldCache) fieldCache).clearDefaultFields();
        filterFieldCache.clear();
        comparatorCache.clear();
        dateIntervalCache.clear();
        providerCache.clear();
        filterSourceCache.clear();
        searchIndexCache.clear();
        searchFieldCache.clear();
    }

}
