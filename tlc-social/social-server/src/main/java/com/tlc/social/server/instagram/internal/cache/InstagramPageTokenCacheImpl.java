package com.tlc.social.server.instagram.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.social.common.facebook.fields.FBFields;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Facilitates to store and retrieve instagram page's access token.
 * </p>
 *
 * @author Ajay
 * @version 1.0.
 */
public class InstagramPageTokenCacheImpl implements InstagramPageTokenCache {

    private final Cache<Long, String> pageTokenCache;

    private InstagramPageTokenCacheImpl() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<Long, String> pageTokenCacheConfig = new CacheConfig<>(FBFields.FB_PAGE_TOKEN_CACHE);

        pageTokenCacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(45));
        pageTokenCache = cacheService.createOrOpenCache(pageTokenCacheConfig);
    }

    private static final class Instance {
        private static final InstagramPageTokenCacheImpl INSTANCE = new InstagramPageTokenCacheImpl();
    }

    /**
     * <p>
     * Gets {@link InstagramPageTokenCacheImpl}.
     * </p>
     *
     * @return {@link InstagramPageTokenCacheImpl}.
     */
    public static InstagramPageTokenCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId          Represents the page id.
     * @param pageAccessToken Represents the page access token.
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
