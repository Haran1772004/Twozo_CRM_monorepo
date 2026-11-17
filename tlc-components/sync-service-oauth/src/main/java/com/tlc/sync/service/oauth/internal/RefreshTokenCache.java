package com.tlc.sync.service.oauth.internal;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * <p>
 * Provides the cache service to store and retrieve the refresh token
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface RefreshTokenCache {

    /**
     * <p>
     * Puts the refresh token into the cache based on the provided {@link TokenWriteRequest}
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void putRefreshToken(final TokenWriteRequest tokenWriteRequest);

    /**
     * <p>
     * Gets the refresh token based on provided {@link TokenReadRequest}
     * </p>
     *
     * @param tokenReadRequest Represents {@link TokenReadRequest}
     * @return the refresh token
     */
    String getRefreshToken(final TokenReadRequest tokenReadRequest);

    /**
     * <p>
     * Clears the refresh token based on the provided {@link TokenReadRequest}
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     */
    void clearRefreshToken(final TokenReadRequest tokenReadRequest);
}