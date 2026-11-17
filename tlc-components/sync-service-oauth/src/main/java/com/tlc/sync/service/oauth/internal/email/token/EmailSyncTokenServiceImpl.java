package com.tlc.sync.service.oauth.internal.email.token;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.service.token.EmailSyncTokenService;
import com.tlc.sync.service.oauth.internal.email.resolver.EmailSyncTokenResolver;

/**
 * <p>
 * Provides the authorization service that are required for getting consent
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailSyncTokenServiceImpl implements EmailSyncTokenService {

    private final EmailSyncTokenResolver emailSyncTokenResolver;

    private EmailSyncTokenServiceImpl() {
        emailSyncTokenResolver = EmailSyncTokenResolver.getInstance();
    }

    private static class Instance {
        private static final EmailSyncTokenServiceImpl INSTANCE = new EmailSyncTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailSyncTokenServiceImpl} Instance
     * </p>
     *
     * @return {@link EmailSyncTokenServiceImpl} Instance
     */
    public static EmailSyncTokenServiceImpl getInstance() {
        return EmailSyncTokenServiceImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return the token
     */
    @Override
    public String getClientCredentialToken(final EmailAddress emailAddress) {
        return emailSyncTokenResolver.getClientCredentialToken(emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putRefreshToken(final TokenWriteRequest tokenWriteRequest) {
        emailSyncTokenResolver.putRefreshToken(tokenWriteRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    @Override
    public String getRefreshToken(final TokenReadRequest tokenReadRequest) {
        return emailSyncTokenResolver.getRefreshToken(tokenReadRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    public String getAccessToken(final TokenReadRequest tokenReadRequest) {
        return emailSyncTokenResolver.getAccessToken(tokenReadRequest);
    }
}
