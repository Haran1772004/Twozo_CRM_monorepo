package com.tlc.social.server.instagram.internal.service;

import com.tlc.social.server.instagram.internal.resolver.InstagramPageTokenResolver;

/**
 * <p>
 * Fetches and stores instagram page's token.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramPageTokenServiceImpl implements InstagramPageTokenService {

    private final InstagramPageTokenResolver instagramPageTokenResolver;

    private InstagramPageTokenServiceImpl() {
        instagramPageTokenResolver = InstagramPageTokenResolver.getInstance();
    }

    private static final class InstanceHolder {
        private static final InstagramPageTokenServiceImpl PAGE_TOKEN_SERVICE = new InstagramPageTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of{@link InstagramPageTokenServiceImpl}
     * </p>
     *
     * @return the instance of{@link InstagramPageTokenServiceImpl}
     */
    public static InstagramPageTokenServiceImpl getInstance() {
        return InstanceHolder.PAGE_TOKEN_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId          Represents the page id
     * @param pageAccessToken Represents the page access token
     */
    @Override
    public void putLinkedPageToken(final Long pageId, final String pageAccessToken) {
        instagramPageTokenResolver.put(pageId, pageAccessToken);
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId Represents the page id
     * @return the page access token
     */
    @Override
    public String getLinkedPageToken(final Long pageId) {
        return instagramPageTokenResolver.get(pageId);
    }
}
