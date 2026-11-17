package com.tlc.social.server.instagram.internal.resolver;

/**
 * <p>
 * Acts as a bridge between Cache and DB to fetch/store Instagram's page token.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface InstagramPageTokenResolver {

    /**
     * <p>
     * Gets {@link InstagramPageTokenResolver}.
     * </p>
     *
     * @return {@link InstagramPageTokenResolver}.
     */
    static InstagramPageTokenResolver getInstance() {
        return InstagramPageTokenResolverImpl.getInstance();
    }

    /**
     * <p>
     * Puts the page access token.
     * </p>
     *
     * @param pageId          Represents the page id.
     * @param pageAccessToken Represents the page access token.
     */
    void put(final Long pageId, final String pageAccessToken);

    /**
     * <p>
     * Gets the page access token.
     * </p>
     *
     * @param pageId Represents the page id.
     * @return the page access token.
     */
    String get(final Long pageId);
}
