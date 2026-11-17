package com.tlc.social.server.instagram.internal.service;

/**
 * <p>
 * Fetches and stores instagram page's token.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface InstagramPageTokenService {

    /**
     * <p>
     * Gets {@link InstagramPageTokenService}.
     * </p>
     *
     * @return {@link InstagramPageTokenService}.
     */
    static InstagramPageTokenService getInstance() {
        return InstagramPageTokenServiceImpl.getInstance();
    }

    /**
     * <p>
     * Puts the page access token into the cache.
     * </p>
     *
     * @param pageId          Represents the page id.
     * @param pageAccessToken Represents the page access token.
     */
    void putLinkedPageToken(final Long pageId, final String pageAccessToken);

    /**
     * <p>
     * Gets the page access token.
     * </p>
     *
     * @param pageId Represents the page id.
     * @return the page access token.
     */
    String getLinkedPageToken(final Long pageId);
}
