package com.tlc.sync.service.oauth.internal;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;

/**
 * <p>
 * Provides the service for managing the tokens for authentication
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface AccessTokenService {

    /**
     * <p>
     * Gets the access token
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    String getAccessToken(final TokenReadRequest tokenReadRequest);
}