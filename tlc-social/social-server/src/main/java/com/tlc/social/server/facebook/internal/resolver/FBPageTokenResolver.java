package com.tlc.social.server.facebook.internal.resolver;

/**
 * A resolver which resolves the facebook page access token
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface FBPageTokenResolver {

    /**
     * <p>
     * Gets the instance of {@link FBPageTokenResolver}
     * </p>
     *
     * @return the instance of {@link FBPageTokenResolver}
     */
    static FBPageTokenResolver getInstance() {
        return FBPageTokenResolverImpl.getInstance();
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
