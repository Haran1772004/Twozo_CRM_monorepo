package com.tlc.analytics.core.api.cache;

import com.tlc.commons.json.JsonObject;
import lombok.NonNull;

import javax.annotation.Nullable;

/**
 * Interface defining the operations for a generic cache API.
 *
 * @author Aswath
 * @version 1.0
 */
public interface AnalyticsCacheService
{
    /**
     * Stores the provided JSON object in the cache under the given cache key.
     *
     * @param cacheKey   The key under which to store the JSON object in the cache. Must not be null.
     * @param jsonObject The JSON object to store in the cache. Must not be null.
     */
    void store( final @NonNull String cacheKey, final @NonNull JsonObject jsonObject );

    /**
     * Retrieves the JSON object from the cache associated with the given cache key.
     *
     * @param cacheKey The key associated with the JSON object in the cache. Must not be null.
     * @return The JSON object stored in the cache, or null if not found.
     */
    @Nullable
    JsonObject getCache( final @NonNull String cacheKey );

    /**
     * Removes the JSON object from the cache associated with the given cache key.
     *
     * @param cacheKey The key associated with the JSON object to remove from the cache. Must not be null.
     */
    void remove( final @NonNull String cacheKey );

    /**
     * Retrieves an instance of the cache API.
     *
     * @return An instance of the cache API implementation.
     */
    static AnalyticsCacheService getInstance()
    {
        return CacheServiceImpl.INSTANCE();
    }
}
