package com.tlc.sync.service.oauth.internal;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * <p>
 * Provides the services to resolve the refresh tokens
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface RefreshTokenResolver {

    /**
     * <p>
     * Stores the refresh token
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void putRefreshToken(final TokenWriteRequest tokenWriteRequest);

    /**
     * <p>
     * Resolves and retrieves the refresh token
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    String getRefreshToken(final TokenReadRequest tokenReadRequest);
}