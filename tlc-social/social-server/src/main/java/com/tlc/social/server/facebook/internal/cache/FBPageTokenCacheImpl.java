package com.tlc.social.server.facebook.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.social.common.facebook.fields.FBFields;

import java.util.concurrent.TimeUnit;

/**
 * A cache which stores and retrieves the facebook page access token
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBPageTokenCacheImpl implements FBPageTokenCache {

    private final Cache<Long, String> pageTokenCache;

    private FBPageTokenCacheImpl() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<Long, String> pageTokenCacheConfig = new CacheConfig<>(FBFields.FB_PAGE_TOKEN_CACHE);

        pageTokenCacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(45));
        pageTokenCache = cacheService.createOrOpenCache(pageTokenCacheConfig);
    }

    private static final class InstanceHolder {
        private static final FBPageTokenCacheImpl INSTANCE = new FBPageTokenCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link FBPageTokenCacheImpl} instance
     * </p>
     *
     * @return {@link FBPageTokenCacheImpl} instance
     */
    public static FBPageTokenCacheImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId          Represents the page id
     * @param pageAccessToken Represents the page access token
     */
    @Override
    public void put(final Long pageId, final String pageAccessToken) {
        pageTokenCache.put(pageId, pageAccessToken);
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId Represents the page id
     * @return the page access token
     */
    @Override
    public String get(final Long pageId) {
        return pageTokenCache.get(pageId);
    }
}
