package com.tlc.cache.remote;

import com.tlc.cache.AsyncCache;
import com.tlc.cache.Cache;
import com.tlc.commons.service.Service;

/**
 * @author Abishek
 * @version 1.0
 */
public interface CacheService extends Service
{
	<D, E> Cache<D, E> createOrOpenCache(final CacheConfig<D, E> config);

    /**
     * Creates or opens a cache with async operation support.
     *
     * @param config The cache configuration
     * @param <D>    The key type
     * @param <E>    The value type
     * @return An {@link AsyncCache} instance supporting async operations
     */
    <D, E> AsyncCache<D, E> createOrOpenAsyncCache(final CacheConfig<D, E> config);

    /**
     * Retrieves an existing Redis atomic counter or creates a new one if it does not exist.
     *
     * <p>This method ensures idempotent access to a Redis-backed {@link DistributedAtomicCounter}
     * instance. It is commonly used for distributed counters.</p>
     *
     * @param key The unique Redis key representing the counter.
     * @return A {@link DistributedAtomicCounter} instance bound to the specified key.
     */
    DistributedAtomicCounter getOrInitAtomicCounter(final String key);
}
