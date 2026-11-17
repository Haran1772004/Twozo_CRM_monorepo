package com.tlc.social.server.whatsapp.internal.service;

import com.tlc.sync.service.oauth.internal.AccessTokenService;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * Provides abstract services to manage the Whatsapp sync tokens
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface WASyncTokenService extends AccessTokenService {

    /**
     * <p>
     * Gets the {@link WASyncTokenService} Instance
     * </p>
     *
     * @return {@link WASyncTokenService}
     */
    static WASyncTokenService getInstance() {
        return WASyncTokenServiceImpl.getInstance();
    }

    /**
     * <p>
     * Puts the access token into the cache
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void putAccessToken(final TokenWriteRequest tokenWriteRequest);
}
