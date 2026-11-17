package com.tlc.analytics.config.cache;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.entity.cache.CacheAdapter;
import com.tlc.analytics.config.entity.cache.CachedValue;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;

import lombok.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Implementation of the CacheAdapter interface for managing cached values of type CachedValue<M>..
 *
 * @author Aswath
 * @version 1.0
 */
class CacheAdapterImpl<M extends BaseModel> implements CacheAdapter<Long, CachedValue<M>>
{
    private final Cache<Long, CachedValue<M>> cache;

    private CacheAdapterImpl()
    {
        final CacheConfig<Long, CachedValue<M>> cacheConfig = new CacheConfig<>();

        cacheConfig.setExpireAfterAccess( TimeUnit.MINUTES.toMillis( 15 ) );
        cacheConfig.setMaxSize( 300 );
        cache = CacheManager.getInstance().createCache( cacheConfig );
    }

    /**
     * Creates a new instance of CacheAdapterImpl with default cache configuration.
     * The cache is configured with an expiration time of 15 minutes after access and a maximum size of 300 entries.
     *
     * @param <V> The type of the model being cached, which extends BaseModel.
     * @return A new instance of CacheAdapterImpl.
     */
    static <V extends BaseModel> CacheAdapterImpl<V> create()
    {
        return new CacheAdapterImpl<>();
    }

    /**
     * Puts a single cache entry into the cache.
     *
     * @param key   The key under which the value is stored in the cache.
     * @param value The cached value to be stored.
     */
    @Override
    public void put( final @NonNull Long key, final CachedValue<M> value )
    {
        cache.put( key, value );
    }

    /**
     * Puts multiple cache entries into the cache.
     *
     * @param pairs A map of keys and values to be stored in the cache.
     */
    @Override
    public void putAll( final @NonNull Map<Long, CachedValue<M>> pairs )
    {
        cache.putAll( pairs );
    }

    /**
     * Removes a single cache entry from the cache.
     *
     * @param key The key of the cache entry to be removed.
     */
    @Override
    public void remove( final @NonNull Long key )
    {
        cache.remove( key );
    }

    /**
     * Removes multiple cache entries from the cache.
     *
     * @param keys A set of keys representing the cache entries to be removed.
     */
    @Override
    public void removeAll( final @NonNull Set<Long> keys )
    {
        cache.removeAll( keys );
    }

    /**
     * Checks if a cache entry exists for the specified key.
     *
     * @param key The key to check for existence in the cache.
     * @return true if the cache contains the specified key, false otherwise.
     */
    @Override
    public boolean containsKey( final @NonNull Long key )
    {
        return cache.containsKey( key );
    }

    /**
     * Retrieves a cache entry for the specified key.
     *
     * @param key The key of the cache entry to retrieve.
     * @return The cached value associated with the key, or null if no entry exists.
     */
    @Override
    public @NonNull CachedValue<M> get( final @NonNull Long key )
    {
        return cache.get( key );
    }

    /**
     * Retrieves multiple cache entries for the specified set of keys.
     *
     * @param keys A set of keys for which to retrieve cache entries.
     * @return A map of keys and their associated cached values.
     */
    @Override
    public @NonNull Map<Long, CachedValue<M>> getAll( @NonNull Set<Long> keys )
    {
        return cache.getAll( keys );
    }

    /**
     * Performs a specified action for each cache entry in the cache.
     *
     * @param consumer A BiConsumer that processes each cache entry (key-value pair).
     */
    @Override
    public void forEach( final @NonNull BiConsumer<Long, CachedValue<M>> consumer )
    {
        cache.forEach( consumer );
    }
}
