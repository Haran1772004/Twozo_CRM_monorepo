package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldConfig;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A specialized implementation of the field metadata cache dedicated to "Secure" fields.
 * <p>
 * This class extends {@link AbstractFieldCache} to manage the caching of fields that contain
 * sensitive information (PII, encrypted data, etc.). It isolates these definitions in a separate
 * cache namespace ("secureFields" and "secureFieldConfigs") to allow for granular control
 * over eviction policies and memory usage distinct from standard fields.
 * </p>
 * <p>
 * Implements the Singleton pattern via the Initialization-on-demand holder idiom.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SecureFieldCache extends AbstractFieldCache {

    private SecureFieldCache() {
        super(createFieldCache(), createFieldConfigCache());
    }

    private static class Instance {
        private static final SecureFieldCache INSTANCE = new SecureFieldCache();
    }

    /**
     * Retrieves the singleton instance of the {@link SecureFieldCache}.
     *
     * @return the singleton instance.
     */
    public static SecureFieldCache getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates and configures the cache for Secure Field Configurations.
     *
     * @return the configured Cache instance for secure field configurations.
     */
    private static Cache<Long, Map<Long, Map<Long, FieldConfig>>> createFieldConfigCache() {
        final CacheConfig<Long, Map<Long, Map<Long, FieldConfig>>> cacheConfig = new CacheConfig<>("secureFieldConfigs");

        cacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(1));
        return CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }

    /**
     * Creates and configures the primary cache for Secure {@link FieldInfo} collections.
     *
     * @return the configured Cache instance for secure field information.
     */
    private static Cache<Long, Map<Long, Map<String, Collection<FieldInfo>>>> createFieldCache() {
        final CacheConfig<Long, Map<Long, Map<String, Collection<FieldInfo>>>> cacheConfig = new CacheConfig<>("secureFields");

        cacheConfig.setTtl((int) TimeUnit.SECONDS.toMillis(10));

        return CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }
}