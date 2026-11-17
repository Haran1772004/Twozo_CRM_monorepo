package com.tlc.sync.service.oauth.model.request;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;

/**
 * <p>
 * Represents the token read request
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class TokenReadRequest extends TokenRequest {

    protected TokenReadRequest(final SyncServiceCategory syncServiceCategory, final TokenType tokenType) {
        super(syncServiceCategory, tokenType);
    }
}