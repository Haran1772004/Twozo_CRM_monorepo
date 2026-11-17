package com.tlc.sync.service.oauth.internal.contact.token;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.service.token.ContactSyncTokenService;
import com.tlc.sync.service.oauth.internal.contact.resolver.ContactSyncTokenResolver;

/**
 * <p>
 * Provides the cache service to authorize the user and stores the tokens required for authentication
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactSyncTokenServiceImpl implements ContactSyncTokenService {

    private final ContactSyncTokenResolver contactSyncTokenResolver;

    public ContactSyncTokenServiceImpl() {
        this.contactSyncTokenResolver = ContactSyncTokenResolver.getInstance();
    }

    private static class Instance {
        private static final ContactSyncTokenServiceImpl INSTANCE = new ContactSyncTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link ContactSyncTokenServiceImpl} Instance
     * </p>
     *
     * @return {@link ContactSyncTokenServiceImpl}
     */
    public static ContactSyncTokenServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putRefreshToken(final TokenWriteRequest tokenWriteRequest) {
        contactSyncTokenResolver.putRefreshToken(tokenWriteRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    @Override
    public String getRefreshToken(final TokenReadRequest tokenReadRequest) {
        return contactSyncTokenResolver.getRefreshToken(tokenReadRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    @Override
    public String getAccessToken(final TokenReadRequest tokenReadRequest) {
        return contactSyncTokenResolver.getAccessToken(tokenReadRequest);
    }
}