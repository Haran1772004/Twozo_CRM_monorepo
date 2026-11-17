package com.tlc.social.server.whatsapp.internal.service;

import com.tlc.social.server.whatsapp.internal.resolver.WASyncTokenResolverImpl;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * Provides the services to manage the Whatsapp sync tokens
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WASyncTokenServiceImpl implements WASyncTokenService {

    private final WASyncTokenResolverImpl waSyncTokenResolver;

    private WASyncTokenServiceImpl() {
        waSyncTokenResolver = WASyncTokenResolverImpl.getInstance();
    }

    /**
     * Holds the instance of {@link WASyncTokenServiceImpl}
     */
    private static final class InstanceHolder {
        private static final WASyncTokenServiceImpl INSTANCE = new WASyncTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link WASyncTokenServiceImpl} Instance
     * </p>
     *
     * @return {@link WASyncTokenServiceImpl} Instance
     */
    public static WASyncTokenServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    @Override
    public String getAccessToken(final TokenReadRequest tokenReadRequest) {
        return waSyncTokenResolver.getAccessToken(tokenReadRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putAccessToken(final TokenWriteRequest tokenWriteRequest) {
        waSyncTokenResolver.putAccessToken(tokenWriteRequest);
    }
}