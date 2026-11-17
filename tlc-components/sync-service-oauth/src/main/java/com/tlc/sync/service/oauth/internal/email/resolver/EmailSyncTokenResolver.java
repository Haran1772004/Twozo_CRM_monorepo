package com.tlc.sync.service.oauth.internal.email.resolver;

import com.tlc.sync.service.oauth.internal.AccessTokenResolver;
import com.tlc.sync.service.oauth.internal.RefreshTokenResolver;
import com.tlc.sync.service.oauth.model.EmailAddress;

/**
 * <p>
 * Provides the services to resolve the tokens used for email synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface EmailSyncTokenResolver extends AccessTokenResolver, RefreshTokenResolver {

    /**
     * <p>
     * Gets the instance for {@link EmailSyncTokenResolver}.
     * </p>
     *
     * @return {@link EmailSyncTokenResolver}
     */
    static EmailSyncTokenResolver getInstance() {
        return EmailSyncTokenResolverImpl.getInstance();
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
