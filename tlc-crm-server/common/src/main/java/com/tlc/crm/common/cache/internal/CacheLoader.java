package com.tlc.crm.common.cache.internal;

import com.tlc.crm.common.iam.cache.internal.permission.PermissionAuthorizerStoreImpl;

/**
 * <p>
 * Provides the functionality for loading data into a cache and clear data from it
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public final class CacheLoader {

    private final PermissionAuthorizerStoreImpl authorizerStore;
    private final PropertyDescriptorCacheImpl propertyDescriptorCache;
    private final FieldPropertiesCacheImpl fieldPropertiesCache;
    private final MethodPropertiesCacheImpl methodPropertiesCache;

    private CacheLoader() {
        this.authorizerStore = PermissionAuthorizerStoreImpl.getInstance();
        this.propertyDescriptorCache = PropertyDescriptorCacheImpl.getInstance();
        this.fieldPropertiesCache = FieldPropertiesCacheImpl.getInstance();
        this.methodPropertiesCache = MethodPropertiesCacheImpl.getInstance();
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
    }

    /**
     * <p>
     * Clears the data from the cache
     * </p>
     */
    public void clear() {
        authorizerStore.clearAnnotations();
        propertyDescriptorCache.clear();
        fieldPropertiesCache.clear();
        methodPropertiesCache.clear();
    }
}
