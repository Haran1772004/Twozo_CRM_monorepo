package com.tlc.social.server.facebook.internal.service;

/**
 * Provides services to store and retrieve facebook page access token
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface FBPageTokenService {

    /**
     * <p>
     * Gets the instance of {@link FBPageTokenService}
     * </p>
     *
     * @return the instance of {@link FBPageTokenService}
     */
    static FBPageTokenService getInstance() {
        return FBPageTokenServiceImpl.getInstance();
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
     * Gets the page access token
     * </p>
     *
     * @param pageId Represents the page id
     * @return the page access token
     */
    String get(final Long pageId);
}
