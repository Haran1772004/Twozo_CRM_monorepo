package com.tlc.crm.fields.cache.internal;

/**
 * <p>
 * Acts a gateway to load the entity into cache
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public class CacheLoader {

    private final FieldSourceCacheImpl fieldSourceCache;
    private final FieldStoreCacheImpl fieldStoreCache;
    private final FieldTypeCacheImpl fieldTypeCache;
    private final SourceSettingsCacheImpl sourceSettingsCache;
    private static class Instance {
        private static final CacheLoader INSTANCE = new CacheLoader();
    }

    public static CacheLoader getInstance() {
        return Instance.INSTANCE;
    }

    private CacheLoader() {
        this.fieldSourceCache = FieldSourceCacheImpl.getInstance();
        this.fieldStoreCache = FieldStoreCacheImpl.getInstance();
        this.fieldTypeCache = FieldTypeCacheImpl.getInstance();
        this.sourceSettingsCache = SourceSettingsCacheImpl.getInstance();
    }

    /**
     * <p>
     * Loads the field details into cache
     * </p>
     */
    public void load() {
        fieldSourceCache.load();
        fieldTypeCache.load();
        fieldStoreCache.load();
        sourceSettingsCache.load();
    }

    /**
     * <p>
     * Clears all the field related caches
     * </p>
     */
    public void clear() {
        fieldStoreCache.clear();
        fieldSourceCache.clear();
        fieldTypeCache.clear();
    }

}
