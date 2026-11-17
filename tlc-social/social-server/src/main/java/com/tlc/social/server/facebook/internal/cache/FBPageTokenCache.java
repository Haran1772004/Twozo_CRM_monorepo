package com.tlc.social.server.facebook.internal.cache;

/**
 * Provides the cache service to store and retrieve facebook page access token
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface FBPageTokenCache {

    /**
     * <p>
     * Gets the instance of {@link FBPageTokenCache}
     * </p>
     *
     * @return the instance of {@link FBPageTokenCache}
     */
    static FBPageTokenCache getInstance() {
        return FBPageTokenCacheImpl.getInstance();
    }

    /**
     * <p>
     * Puts the page access token into the cache
     * </p>
     *
     * @param pageId          Represents the page id
     * @param pageAccessToken Represents the page access token
     */
    void put(final Long pageId, final String pageAccessToken);

    /**
     * <p>
     * Gets the page access token from the cache
     * </p>
     *
     * @param pageId Represents the page id
     * @return the page access token
     */
    String get(final Long pageId);
}
