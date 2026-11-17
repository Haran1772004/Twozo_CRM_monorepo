package com.tlc.sync.service.oauth.internal;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * <p>
 * Provides the service for managing the refresh tokens
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface RefreshTokenService {

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
     * Gets the refresh token
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     */
    String getRefreshToken(final TokenReadRequest tokenReadRequest);
}