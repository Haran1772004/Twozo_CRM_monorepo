package com.tlc.sync.service.oauth.internal.email.resolver;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.concurrent.LockStore;
import com.tlc.commons.util.ConfHolder;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.email.token.EmailSyncTokenCache;
import com.tlc.sync.service.oauth.internal.manager.impl.EmailSyncManager;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.service.provider.ProviderService;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the services to resolve the tokens used for email synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
class EmailSyncTokenResolverImpl implements EmailSyncTokenResolver {

    private static final byte STRIPES_COUNT = 10;

    private final EmailSyncTokenCache emailSyncTokenCache;
    private final SyncManager syncManager;
    private final LockStore<String> lockStore;
    private final ProviderService emailProviderService;
    private final ConfHolder confHolder;
    private final EmailSyncManager emailSyncManager;
    private final int refreshTokenExpiryDays;

    public EmailSyncTokenResolverImpl() {
        this.emailSyncTokenCache = EmailSyncTokenCache.getInstance();
        this.syncManager = SyncManager.getInstance();
        this.lockStore = new LockStore<>(STRIPES_COUNT);
        this.emailProviderService = ProviderService.getEmailProviderService();
        this.confHolder = ConfHolder.getInstance();
        this.emailSyncManager = EmailSyncManager.getInstance();
        this.refreshTokenExpiryDays = confHolder.getProperties(SyncServiceFields.SYNC_SERVICE)
                .getNestedConfigs(SyncServiceFields.OUTLOOK).getInt(SyncServiceFields.REFRESH_TOKEN_EXPIRY_DAYS);
    }

    private static class Instance {
        private static final EmailSyncTokenResolverImpl INSTANCE = new EmailSyncTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailSyncTokenResolverImpl} instance
     * </p>
     *
     * @return {@link EmailSyncTokenResolverImpl} instance
     */
    public static EmailSyncTokenResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return the token
     */
    @Override
    public String getClientCredentialToken(final EmailAddress emailAddress) {
        final ReentrantReadWriteLock lock = lockStore.getLock(emailAddress.getEmailId());
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final String tenantId = confHolder.getProperties(SyncServiceFields.NOTIFICATION).getConfig(SyncServiceFields.OUTLOOK_TENANT_ID);
        final String accessToken;

        readLock.lock();

        try {
            accessToken = emailSyncTokenCache.getClientCredentialToken(tenantId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(accessToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final EmailProvider emailProvider = emailProviderService.findProvider(emailAddress);
                final Optional<String> newToken = emailProvider.getSyncService().fetchClientCredentialToken();

                if (newToken.isPresent()) {
                    final String token = newToken.get();

                    emailSyncTokenCache.putClientCredentialToken(tenantId, token);

                    return token;
                } else {
                    throw ErrorCode.get(SyncErrorCodes.CLIENT_CREDENTIAL_TOKEN_FETCH_FAILED);
                }
            } finally {
                writeLock.unlock();
            }
        }

        return accessToken;
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
        final EmailProvider emailProvider = emailProviderService.findProvider(emailAddress);

        writeLock.lock();

        try {
            final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getEmailTokenReadRequest(emailAddress, TokenRequest.TokenType.REFRESH);

            emailSyncTokenCache.clearRefreshToken(refreshTokenReadRequest);

            if (EmailProvider.GOOGLE.equals(emailProvider)){
                syncManager.storeNonRefreshableToken(emailTokenWriteRequest);
            } else if (EmailProvider.OUTLOOK.equals(emailProvider)) {
                emailTokenWriteRequest.setNextRefreshedAt(TemporalServiceProvider.get().plus(refreshTokenExpiryDays, DateInterval.DAYS));
                emailSyncManager.storeRefreshableToken(emailTokenWriteRequest);
            }
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
            refreshToken = emailSyncTokenCache.getRefreshToken(emailTokenReadRequest);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(refreshToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final EmailProvider emailProvider = emailProviderService.findProvider(emailAddress);
                final Optional<String> existingToken;

                if (EmailProvider.GOOGLE.equals(emailProvider)) {
                    existingToken = syncManager.getNonRefreshableToken(emailTokenReadRequest);
                } else if (EmailProvider.OUTLOOK.equals(emailProvider)) {
                    existingToken = emailSyncManager.getRefreshableToken(emailTokenReadRequest);
                } else {
                    existingToken = Optional.empty();
                }

                if (existingToken.isPresent()) {
                    final String token = existingToken.get();
                    final EmailTokenWriteRequest refreshTokenWriteRequest = SyncTokenRequestInstanceProvider.getEmailTokenWriteRequest(TokenRequest.TokenType.REFRESH, emailAddress);

                    refreshTokenWriteRequest.setToken(token);
                    emailSyncTokenCache.putRefreshToken(refreshTokenWriteRequest);
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
        final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getEmailTokenReadRequest(emailAddress, TokenRequest.TokenType.REFRESH);
        final String refreshToken = getRefreshToken(refreshTokenReadRequest);

        if (Objects.isNull(refreshToken)) {
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN);
        }
        final ReentrantReadWriteLock lock = lockStore.getLock(emailAddress.getEmailId());
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final String accessToken;

        readLock.lock();

        try {
            accessToken = emailSyncTokenCache.getAccessToken(emailTokenReadRequest);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(accessToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final EmailProvider emailProvider = emailProviderService.findProvider(emailAddress);

                emailTokenReadRequest.setRefreshToken(refreshToken);
                final String newToken = emailProvider.getSyncService().fetchAccessToken(emailTokenReadRequest);
                final EmailTokenWriteRequest accessTokenWriteRequest = SyncTokenRequestInstanceProvider.getEmailTokenWriteRequest(TokenRequest.TokenType.ACCESS, emailAddress);

                accessTokenWriteRequest.setToken(newToken);
                emailSyncTokenCache.putAccessToken(accessTokenWriteRequest);

                return newToken;
            } finally {
                writeLock.unlock();
            }
        }

        return accessToken;
    }
}
