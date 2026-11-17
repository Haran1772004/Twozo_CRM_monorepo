package com.tlc.social.server.facebook.internal.service;

import com.tlc.social.server.facebook.internal.resolver.FBPageTokenResolver;

/**
 * An implementation of {@link FBPageTokenService} which provides service to get and store facebook page access tokens
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBPageTokenServiceImpl implements FBPageTokenService {

    private final FBPageTokenResolver fbPageTokenResolver;

    private FBPageTokenServiceImpl() {
        fbPageTokenResolver = FBPageTokenResolver.getInstance();
    }

    private static final class InstanceHolder {
        private static final FBPageTokenServiceImpl PAGE_TOKEN_SERVICE = new FBPageTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of{@link FBPageTokenServiceImpl}
     * </p>
     *
     * @return the instance of{@link FBPageTokenServiceImpl}
     */
    public static FBPageTokenServiceImpl getInstance() {
        return InstanceHolder.PAGE_TOKEN_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId          Represents the page id
     * @param pageAccessToken Represents the page access token
     */
    @Override
    public void put(final Long pageId, final String pageAccessToken) {
        fbPageTokenResolver.put(pageId, pageAccessToken);
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId Represents the page id
     * @return the page access token
     */
    @Override
    public String get(final Long pageId) {
        return fbPageTokenResolver.get(pageId);
    }
}
