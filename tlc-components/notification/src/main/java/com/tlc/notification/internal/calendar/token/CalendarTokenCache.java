package com.tlc.notification.internal.calendar.token;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.notification.internal.cache.TokenCache;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Provides the cache services for calendar tokens
 * </p>
 *
 * @author Ajith
 * @version 1.0
 */
public class CalendarTokenCache implements TokenCache {

    private final Cache<String, String> syncToken;

    private CalendarTokenCache() {
        final CacheConfig<String, String> syncTokenConfig = new CacheConfig<>(SyncServiceFields.CALENDAR_SYNC_TOKEN);

        // Note: Sync tokens for Google and Outlook expire in 7 days.
        // Setting TTL to 5 days ensures timely refresh and avoids expiration issues.
        syncTokenConfig.setTtl((int) TimeUnit.DAYS.toMillis(5));
        syncToken = CacheServiceProvider.get().createOrOpenCache(syncTokenConfig);
    }

    private static class Instance {
        private static final CalendarTokenCache INSTANCE = new CalendarTokenCache();
    }

    /**
     * <p>
     * Gets the {@link CalendarTokenCache} instance
     * </p>
     *
     * @return {@link CalendarTokenCache} instance
     */
    public static CalendarTokenCache getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public void clearSyncToken() {
        syncToken.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return Sync token stored for every sync
     */
    @Override
    public String getSyncToken(final String emailAddress) {
        return syncToken.get(emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the sync token
     */
    @Override
    public void putSyncToken(final String emailAddress, final String token) {
        syncToken.put(emailAddress, token);
    }
}
