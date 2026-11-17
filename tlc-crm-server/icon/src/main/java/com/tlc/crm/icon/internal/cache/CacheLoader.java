package com.tlc.crm.icon.internal.cache;

/**
 * <p>
 * Provides the functionality for loading data into a cache and clear data from it
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class CacheLoader {

    private final IconCacheImpl iconCache;

    private CacheLoader() {
        this.iconCache = IconCacheImpl.getInstance();
    }

    private static class Instance {
        private static final CacheLoader INSTANCE = new CacheLoader();
    }

    /**
     * <p>
     * Gets the instance of {@link CacheLoader}
     * </p>
     *
     * @return {@link CacheLoader}
     */
    public static CacheLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads the data into cache
     * </p>
     */
    public void load() {
        iconCache.load();
    }

    /**
     * <p>
     * Clears the data from the cache
     * </p>
     */
    public void clear() {
        iconCache.clear();
    }
}
