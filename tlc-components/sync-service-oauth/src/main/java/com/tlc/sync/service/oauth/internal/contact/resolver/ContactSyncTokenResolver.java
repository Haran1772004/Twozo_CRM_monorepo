package com.tlc.sync.service.oauth.internal.contact.resolver;

import com.tlc.sync.service.oauth.internal.AccessTokenResolver;
import com.tlc.sync.service.oauth.internal.RefreshTokenResolver;

/**
 * <p>
 * Provides the services to resolve the tokens used for contact synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactSyncTokenResolver extends AccessTokenResolver, RefreshTokenResolver {

    /**
     * <p>
     * Gets the instance for {@link ContactSyncTokenResolver}.
     * </p>
     *
     * @return {@link ContactSyncTokenResolver}
     */
    static ContactSyncTokenResolver getInstance() {
        return ContactSyncTokenResolverImpl.getInstance();
    }
}
