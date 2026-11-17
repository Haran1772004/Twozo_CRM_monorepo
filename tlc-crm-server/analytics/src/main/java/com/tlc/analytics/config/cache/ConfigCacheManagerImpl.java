package com.tlc.analytics.config.cache;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.entity.cache.AbstractConfigCacheManager;
import com.tlc.analytics.config.entity.cache.CacheAdapter;
import com.tlc.analytics.config.entity.cache.CachedValue;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;

import com.tlc.commons.functions.TriFunction;

import lombok.NonNull;

import javax.annotation.Nonnull;

/**
 * <p>
 * ConfigCacheManagerImpl is an implementation of the AbstractConfigCacheManager that provides cache management for configuration models
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
class ConfigCacheManagerImpl<M extends BaseModel> extends AbstractConfigCacheManager<M>
{
    private final CacheAdapter<Long, CachedValue<M>> cacheAdapter;
    private final AnalyticsConfigManager<M> configManager;
    private final TriFunction<Long, Long, CachedValue<M>, Boolean> isCacheModified;

    private ConfigCacheManagerImpl( final @NonNull CacheAdapter<Long, CachedValue<M>> cacheAdapter,
                                    final @NonNull AnalyticsConfigManager<M> configManager,
                                    final @NonNull TriFunction<Long, Long, CachedValue<M>, Boolean> isCacheModified )
    {
        this.cacheAdapter = cacheAdapter;
        this.configManager = configManager;
        this.isCacheModified = isCacheModified;
    }

    /**
     * Creates a new instance of {@link ConfigCacheManagerImpl} with the specified parameters.
     *
     * @param cacheAdapter    a non-null {@link CacheAdapter} used for handling cache operations.
     * @param configManager   a non-null {@link AnalyticsConfigManager} used for managing configuration.
     * @param isCacheModified a non-null {@link TriFunction} that determines if the cache has been modified.
     * @param <M>             the type of model that extends {@link BaseModel}.
     * @return a new instance of {@link ConfigCacheManagerImpl} initialized with the provided arguments.
     */
    static <M extends BaseModel> ConfigCacheManagerImpl<M> create( final @NonNull CacheAdapter<Long, CachedValue<M>> cacheAdapter,
                                                                   final @NonNull AnalyticsConfigManager<M> configManager,
                                                                   final @NonNull TriFunction<Long, Long, CachedValue<M>, Boolean> isCacheModified )
    {
        return new ConfigCacheManagerImpl<>( cacheAdapter, configManager, isCacheModified );
    }

    /**
     * Retrieves the {@link CacheAdapter} used for handling cache operations.
     *
     * @return the {@link CacheAdapter} instance associated with this manager.
     */
    @Override
    protected @Nonnull CacheAdapter<Long, CachedValue<M>> getCacheAdapter()
    {
        return cacheAdapter;
    }

    /**
     * Retrieves the {@link AnalyticsConfigManager} used for managing configuration.
     *
     * @return the {@link AnalyticsConfigManager} instance associated with this manager.
     */
    @Override
    protected @Nonnull AnalyticsConfigManager<M> getConfigManager()
    {
        return configManager;
    }

    /**
     * Determines if the cache has been modified for the given parameters.
     *
     * @param orgId       Represents the organization ID.
     * @param id          Represents the ID of the cache entry.
     * @param cachedValue the {@link CachedValue} object to check for modification.
     * @return {@code true} if the cache is modified based on the given parameters; {@code false} otherwise.
     */
    @Override
    protected boolean isModified( final long orgId,
                                  final long id,
                                  final @NonNull CachedValue<M> cachedValue )
    {
        return isCacheModified.apply( orgId, id, cachedValue );
    }
}
