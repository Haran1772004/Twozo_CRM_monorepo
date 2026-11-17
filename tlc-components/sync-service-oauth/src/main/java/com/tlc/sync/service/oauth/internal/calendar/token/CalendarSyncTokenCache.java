package com.tlc.sync.service.oauth.internal.calendar.token;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.RefreshTokenCache;
import com.tlc.sync.service.oauth.internal.AccessTokenCache;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Provides the calendar cache service to authorize the user and stores the tokens required for authentication
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class CalendarSyncTokenCache implements AccessTokenCache, RefreshTokenCache {

    private final Cache<String, String> accessTokenCache;
    private final Cache<String, String> refreshTokenCache;

    private static class Instance {
        private static final CalendarSyncTokenCache INSTANCE = new CalendarSyncTokenCache();
    }

    private CalendarSyncTokenCache() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<String, String> accessTokenConfig = new CacheConfig<>(SyncServiceFields.CALENDAR_ACCESS_TOKEN);
        final CacheConfig<String, String> refreshTokenConfig = new CacheConfig<>(SyncServiceFields.CALENDAR_REFRESH_TOKEN);

        accessTokenConfig.setTtl((int) TimeUnit.MINUTES.toMillis(45));
        refreshTokenConfig.setTtl((int) TimeUnit.HOURS.toMillis(2));
        accessTokenCache = cacheService.createOrOpenCache(accessTokenConfig);
        refreshTokenCache = cacheService.createOrOpenCache(refreshTokenConfig);
    }

    /**
     * <p>
     * Gets the {@link CalendarSyncTokenCache} Instance
     * </p>
     *
     * @return {@link CalendarSyncTokenCache}
     */
    public static CalendarSyncTokenCache getInstance() {
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

        refreshTokenCache.put(emailTokenWriteRequest.getEmailAddress().getEmailId(), emailTokenWriteRequest.getToken());
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents {@link TokenReadRequest}
     * @return the refresh token
     */
    @Override
    public String getRefreshToken(final TokenReadRequest tokenReadRequest) {
        final EmailTokenReadRequest emailTokenReadRequest = (EmailTokenReadRequest) tokenReadRequest;

        return refreshTokenCache.get(emailTokenReadRequest.getEmailAddress().getEmailId());
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     */
    @Override
    public void clearRefreshToken(final TokenReadRequest tokenReadRequest) {
        final EmailTokenReadRequest emailTokenReadRequest = (EmailTokenReadRequest) tokenReadRequest;

        refreshTokenCache.remove(emailTokenReadRequest.getEmailAddress().getEmailId());
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putAccessToken(final TokenWriteRequest tokenWriteRequest) {
        final EmailTokenWriteRequest emailTokenWriteRequest = (EmailTokenWriteRequest) tokenWriteRequest;

        this.accessTokenCache.put(emailTokenWriteRequest.getEmailAddress().getEmailId(), emailTokenWriteRequest.getToken());
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

        return accessTokenCache.get(emailTokenReadRequest.getEmailAddress().getEmailId());
    }
}
