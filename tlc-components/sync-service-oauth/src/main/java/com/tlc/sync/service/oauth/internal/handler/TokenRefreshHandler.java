package com.tlc.sync.service.oauth.internal.handler;

import com.tlc.sync.service.oauth.model.RefreshableToken;

/**
 * A handler which handles the token refresh process
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface TokenRefreshHandler {

    /**
     * Handles the token refresh
     *
     * @param refreshableToken Represents the {@link RefreshableToken}
     */
    void handle(final RefreshableToken refreshableToken);
}