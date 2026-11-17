package com.tlc.sync.service.oauth.service.token;

import com.tlc.sync.service.oauth.internal.AccessTokenService;
import com.tlc.sync.service.oauth.internal.RefreshTokenService;
import com.tlc.sync.service.oauth.internal.contact.token.ContactSyncTokenServiceImpl;

/**
 * <p>
 * Provides the cache service to authorize the user and stores the tokens required for authentication
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactSyncTokenService extends AccessTokenService, RefreshTokenService {

    /**
     * <p>
     * Gets the {@link ContactSyncTokenServiceImpl} Instance
     * </p>
     *
     * @return {@link ContactSyncTokenServiceImpl}
     */
    static ContactSyncTokenService getInstance() {
        return ContactSyncTokenServiceImpl.getInstance();
    }
}
