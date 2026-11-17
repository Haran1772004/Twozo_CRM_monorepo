package com.tlc.sync.service.oauth.internal;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * <p>
 * Provides the cache service to authorize the user and stores the tokens required for authentication
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface AccessTokenCache {

    /**
     * <p>
     * Puts the access token into the cache
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void putAccessToken(final TokenWriteRequest tokenWriteRequest);

    /**
     * <p>
     * Gets the access token from the provided {@link TokenReadRequest}
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    String getAccessToken(final TokenReadRequest tokenReadRequest);
}
