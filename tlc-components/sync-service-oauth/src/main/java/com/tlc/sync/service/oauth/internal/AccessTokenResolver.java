package com.tlc.sync.service.oauth.internal;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;

/**
 * <p>
 * Provides the services to resolve the tokens used for synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface AccessTokenResolver {

    /**
     * <p>
     * Fetches the access token from the provided {@link TokenReadRequest}
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    String getAccessToken(final TokenReadRequest tokenReadRequest);
}
