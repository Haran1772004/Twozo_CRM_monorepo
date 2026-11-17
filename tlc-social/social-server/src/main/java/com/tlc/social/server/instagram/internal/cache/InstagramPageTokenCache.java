package com.tlc.social.server.instagram.internal.cache;

/**
 * <p>
 * Facilitates to store and retrieve instagram page's access token.
 * </p>
 *
 * @author Ajay
 * @version 1.0.
 */
public interface InstagramPageTokenCache {

    /**
     * <p>
     * Gets {@link InstagramPageTokenCache}.
     * </p>
     *
     * @return {@link InstagramPageTokenCache}.
     */
    static InstagramPageTokenCache getInstance() {
        return InstagramPageTokenCacheImpl.getInstance();
    }

    /**
     * <p>
     * Puts the page access token into the cache.
     * </p>
     *
     * @param pageId          Represents the page id.
     * @param pageAccessToken Represents the page access token.
     */
    void put(final Long pageId, final String pageAccessToken);

    /**
     * <p>
     * Gets the page access token from the cache.
     * </p>
     *
     * @param pageId Represents the page id.
     * @return the page access token.
     */
    String get(final Long pageId);
}