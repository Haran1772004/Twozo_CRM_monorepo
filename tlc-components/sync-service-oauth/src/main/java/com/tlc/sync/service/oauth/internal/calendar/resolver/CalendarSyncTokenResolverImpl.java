package com.tlc.sync.service.oauth.internal.calendar.resolver;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.concurrent.LockStore;
import com.tlc.sync.service.oauth.internal.calendar.token.CalendarSyncTokenCache;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.provider.Provider;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.service.sync.SyncService;
import com.tlc.sync.service.oauth.service.provider.ProviderService;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the services to resolve the tokens used for calendar synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
class CalendarSyncTokenResolverImpl implements CalendarSyncTokenResolver {

    private static final byte STRIPES_COUNT = 10;

    private final CalendarSyncTokenCache calendarSyncTokenCache;
    private final SyncManager syncManager;
    private final LockStore<String> lockStore;
    private final ProviderService calendarProviderService;

    public CalendarSyncTokenResolverImpl() {
        this.lockStore = new LockStore<>(STRIPES_COUNT);
        this.calendarSyncTokenCache = CalendarSyncTokenCache.getInstance();
        this.syncManager = SyncManager.getInstance();
        this.calendarProviderService = ProviderService.getCalendarProviderService();
    }

    private static class Instance {
        private static final CalendarSyncTokenResolverImpl INSTANCE = new CalendarSyncTokenResolverImpl();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link CalendarSyncTokenResolverImpl}
     */
    public static CalendarSyncTokenResolverImpl getInstance() {
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
            final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(emailAddress, TokenRequest.TokenType.REFRESH);

            calendarSyncTokenCache.clearRefreshToken(refreshTokenReadRequest);

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
            refreshToken = calendarSyncTokenCache.getRefreshToken(emailTokenReadRequest);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(refreshToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Optional<String> existingRefreshToken = syncManager.getNonRefreshableToken(emailTokenReadRequest);

                if (existingRefreshToken.isPresent()) {
                    final String token = existingRefreshToken.get();
                    final EmailTokenWriteRequest refreshTokenWriteRequest = SyncTokenRequestInstanceProvider.getCalendarTokenWriteRequest(TokenRequest.TokenType.REFRESH, emailAddress);

                    refreshTokenWriteRequest.setToken(token);
                    calendarSyncTokenCache.putRefreshToken(refreshTokenWriteRequest);

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
        final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(emailAddress, TokenRequest.TokenType.REFRESH);
        final String refreshToken = getRefreshToken(refreshTokenReadRequest);

        if (Objects.isNull(refreshToken)) {
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN);
        }
        final ReentrantReadWriteLock lock = lockStore.getLock(emailAddress.getEmailId());
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final String accessToken;

        readLock.lock();

        try {
            accessToken = calendarSyncTokenCache.getAccessToken(emailTokenReadRequest);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(accessToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Provider calendarProvider = calendarProviderService.findProvider(emailAddress);
                final SyncService calendarSyncService = calendarProvider.getSyncService();

                emailTokenReadRequest.setRefreshToken(refreshToken);
                final String newToken = calendarSyncService.fetchAccessToken(emailTokenReadRequest);
                final EmailTokenWriteRequest accessTokenWriteRequest = SyncTokenRequestInstanceProvider.getCalendarTokenWriteRequest(TokenRequest.TokenType.ACCESS, emailAddress);

                accessTokenWriteRequest.setToken(newToken);
                calendarSyncTokenCache.putAccessToken(accessTokenWriteRequest);

                return newToken;
            } finally {
                writeLock.unlock();
            }
        }

        return accessToken;
    }
}
