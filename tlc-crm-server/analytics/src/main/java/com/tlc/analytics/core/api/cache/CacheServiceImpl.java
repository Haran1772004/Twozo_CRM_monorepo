package com.tlc.analytics.core.api.cache;

import com.tlc.commons.json.JsonObject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

/**
 * Implementation of the analytics cache service interface, providing methods for caching operations.
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
class CacheServiceImpl implements AnalyticsCacheService
{
    private static class Instance
    {
        private static final CacheServiceImpl INSTANCE = new CacheServiceImpl();
    }

    public static CacheServiceImpl INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param cacheKey The key under which to store the JSON object in the cache. Must not be null.
     * @param cache    The JSON object to store in the cache. Must not be null.
     */
    @Override
    public void store( final @NonNull String cacheKey, final @NonNull JsonObject cache )
    {

    }

    /**
     * {@inheritDoc}
     *
     * @param cacheKey The key associated with the JSON object in the cache. Must not be null.
     * @return The JSON object stored in the cache, or null if not found.
     */
    @Override
    public @Nullable JsonObject getCache( final @NonNull String cacheKey )
    {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param cacheKey The key associated with the JSON object to remove from the cache. Must not be null.
     */
    @Override
    public void remove( final @NonNull String cacheKey )
    {

    }
}
