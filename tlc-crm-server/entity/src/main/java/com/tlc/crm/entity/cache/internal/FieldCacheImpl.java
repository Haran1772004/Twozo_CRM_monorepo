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
 * The default implementation of the field metadata cache.
 * <p>
 * This class extends {@link AbstractFieldCache} and is responsible for initializing the
 * underlying cache storage mechanisms for both Field Configurations and Field Definitions.
 * It defines specific eviction policies (TTL) to balance performance with data freshness.
 * </p>
 * <p>
 * Implements the Singleton pattern using the Initialization-on-demand holder idiom.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldCacheImpl extends AbstractFieldCache {

    private FieldCacheImpl() {
        super(createFieldCache(), createFieldConfigCache());
    }

    private static class Instance {
        private static final FieldCacheImpl INSTANCE = new FieldCacheImpl();
    }

    /**
     * Retrieves the singleton instance of the {@link FieldCacheImpl}.
     *
     * @return the singleton instance.
     */
    public static FieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates and configures the cache for {@link FieldConfig} objects.
     *
     * @return the configured Cache instance for field configurations.
     */
    private static Cache<Long, Map<Long, Map<Long, FieldConfig>>> createFieldConfigCache() {
        final CacheConfig<Long, Map<Long, Map<Long, FieldConfig>>> cacheConfig = new CacheConfig<>("fieldConfigs");

        cacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(1));
        return CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }

    /**
     * Creates and configures the primary cache for {@link FieldInfo} collections.
     *
     * @return the configured Cache instance for field information.
     */
    private static Cache<Long, Map<Long, Map<String, Collection<FieldInfo>>>> createFieldCache() {
        final CacheConfig<Long, Map<Long, Map<String, Collection<FieldInfo>>>> cacheConfig = new CacheConfig<>("fields");

        cacheConfig.setTtl((int) TimeUnit.SECONDS.toMillis(10));
        return CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }
}