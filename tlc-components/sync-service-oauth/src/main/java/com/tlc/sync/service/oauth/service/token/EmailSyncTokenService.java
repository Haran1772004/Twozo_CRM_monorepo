package com.tlc.sync.service.oauth.service.token;

import com.tlc.sync.service.oauth.internal.AccessTokenService;
import com.tlc.sync.service.oauth.internal.RefreshTokenService;
import com.tlc.sync.service.oauth.internal.email.token.EmailSyncTokenServiceImpl;
import com.tlc.sync.service.oauth.model.EmailAddress;

/**
 * <p>
 * Provides the service for managing email tokens.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface EmailSyncTokenService extends AccessTokenService, RefreshTokenService {

    /**
     * <p>
     * Gets the {@link EmailSyncTokenServiceImpl} Instance
     * </p>
     *
     * @return {@link EmailSyncTokenServiceImpl}
     */
    static EmailSyncTokenService getInstance() {
        return EmailSyncTokenServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets client credentials token for the given {@link EmailAddress}
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return the token
     */
    String getClientCredentialToken(final EmailAddress emailAddress);
}
