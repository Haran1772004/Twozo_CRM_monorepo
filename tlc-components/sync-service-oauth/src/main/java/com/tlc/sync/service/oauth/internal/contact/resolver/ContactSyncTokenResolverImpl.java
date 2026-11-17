package com.tlc.sync.service.oauth.internal.contact.resolver;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.concurrent.LockStore;
import com.tlc.sync.service.oauth.internal.contact.token.ContactSyncTokenCache;
import com.tlc.sync.service.oauth.internal.provider.contact.ContactProviderService;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.internal.contact.ContactSyncService;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the services to resolve the tokens used for contact synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
class ContactSyncTokenResolverImpl implements ContactSyncTokenResolver {

    private static final byte STRIPES_COUNT = 10;

    private final ContactSyncTokenCache contactSyncTokenCache;
    private final SyncManager syncManager;
    private final LockStore<String> lockStore;
    private final ContactProviderService contactProviderService;

    public ContactSyncTokenResolverImpl() {
        this.contactSyncTokenCache = ContactSyncTokenCache.getInstance();
        this.syncManager = SyncManager.getInstance();
        this.lockStore = new LockStore<>(STRIPES_COUNT);
        this.contactProviderService = ContactProviderService.getInstance();
    }

    private static class Instance {
        private static final ContactSyncTokenResolverImpl INSTANCE = new ContactSyncTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link ContactSyncTokenResolverImpl} instance
     * </p>
     *
     * @return {@link ContactSyncTokenResolverImpl} instance
     */
    public static ContactSyncTokenResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putRefreshToken(final TokenWriteRequest tokenWriteRequest) {
        final EmailTokenWriteRequest emailTokenWriteRequest = (EmailTokenWriteRequest) tokenWriteRequest;
        final EmailAddress emailAddress = emailTokenWriteRequest.getEmailAddress();
        final ReentrantReadWriteLock lock = lockStore.getLock(emailAddress.getEmailId());
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getContactTokenReadRequest(emailAddress, TokenRequest.TokenType.REFRESH);

            contactSyncTokenCache.clearRefreshToken(refreshTokenReadRequest);
            syncManager.storeNonRefreshableToken(emailTokenWriteRequest);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    @Override
    public String getRefreshToken(final TokenReadRequest tokenReadRequest) {
        final EmailTokenReadRequest emailTokenReadRequest = (EmailTokenReadRequest) tokenReadRequest;
        final EmailAddress emailAddress = emailTokenReadRequest.getEmailAddress();
        final ReentrantReadWriteLock lock = lockStore.getLock(emailAddress.getEmailId());
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final String refreshToken;

        readLock.lock();

        try {
            refreshToken = contactSyncTokenCache.getRefreshToken(emailTokenReadRequest);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(refreshToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Optional<String> newRefreshToken = syncManager.getNonRefreshableToken(emailTokenReadRequest);

                if (newRefreshToken.isPresent()) {
                    final String token = newRefreshToken.get();
                    final EmailTokenWriteRequest refreshTokenWriteRequest = SyncTokenRequestInstanceProvider.getContactTokenWriteRequest(TokenRequest.TokenType.REFRESH, emailAddress);

                    refreshTokenWriteRequest.setToken(token);
                    contactSyncTokenCache.putRefreshToken(refreshTokenWriteRequest);

                    return token;
                }
            } finally {
                writeLock.unlock();
            }
        }

        return refreshToken;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    @Override
    public String getAccessToken(final TokenReadRequest tokenReadRequest) {
        final EmailTokenReadRequest emailTokenReadRequest = (EmailTokenReadRequest) tokenReadRequest;
        final EmailAddress emailAddress = emailTokenReadRequest.getEmailAddress();
        final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getContactTokenReadRequest(emailAddress, TokenRequest.TokenType.REFRESH);
        final String refreshToken = getRefreshToken(refreshTokenReadRequest);

        if (Objects.isNull(refreshToken)) {
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN);
        }
        final ReentrantReadWriteLock lock = lockStore.getLock(emailAddress.getEmailId());
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final String accessToken;

        readLock.lock();

        try {
            accessToken = contactSyncTokenCache.getAccessToken(emailTokenReadRequest);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(accessToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final ContactSyncService oauthService = contactProviderService.findProvider(emailAddress).getSyncService();

                emailTokenReadRequest.setRefreshToken(refreshToken);
                final String newToken = oauthService.fetchAccessToken(emailTokenReadRequest);
                final EmailTokenWriteRequest accessTokenWriteRequest = SyncTokenRequestInstanceProvider.getContactTokenWriteRequest(TokenRequest.TokenType.ACCESS, emailAddress);

                accessTokenWriteRequest.setToken(newToken);
                contactSyncTokenCache.putAccessToken(accessTokenWriteRequest);

                return newToken;
            } finally {
                writeLock.unlock();
            }
        }

        return accessToken;
    }
}
