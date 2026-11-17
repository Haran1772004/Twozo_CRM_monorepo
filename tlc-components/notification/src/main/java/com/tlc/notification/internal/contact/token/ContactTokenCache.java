package com.tlc.notification.internal.contact.token;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.notification.internal.cache.TokenCache;
import com.tlc.notification.model.TokenType;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Provides the cache services for contact tokens
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactTokenCache implements TokenCache {

    private final Cache<String, String> syncTokenCache;
    private final Cache<String, String> pageTokenCache;

    private static class Instance {
        private static final ContactTokenCache INSTANCE = new ContactTokenCache();
    }

    /**
     * <p>
     * Gets the {@link ContactTokenCache} instance
     * </p>
     *
     * @return {@link ContactTokenCache} instance
     */
    public static ContactTokenCache getInstance() {
        return Instance.INSTANCE;
    }

    private ContactTokenCache() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<String, String> syncTokenConfig = new CacheConfig<>(SyncServiceFields.CONTACT_SYNC_TOKEN);
        final CacheConfig<String, String> pageTokenConfig = new CacheConfig<>(SyncServiceFields.CONTACT_PAGE_TOKEN);

        // Note: Sync tokens for Google and Outlook expire in 7 days.
        // Setting TTL to 5 days ensures timely refresh and avoids expiration issues.
        syncTokenConfig.setTtl((int) TimeUnit.DAYS.toMillis(5));
        pageTokenConfig.setTtl((int) TimeUnit.HOURS.toMillis(23));
        this.syncTokenCache = cacheService.createOrOpenCache(syncTokenConfig);
        this.pageTokenCache = cacheService.createOrOpenCache(pageTokenConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return Sync token stored for every sync
     */
    @Override
    public String getSyncToken(final String emailAddress) {
        return syncTokenCache.get(emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the sync token
     */
    @Override
    public void putSyncToken(final String emailAddress, final String token) {
        syncTokenCache.put(emailAddress, token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSyncToken() {
        syncTokenCache.clear();
    }

    /**
     * <p>
     * Gets the page token for respective email address
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return Page token stored for every page
     */
    public String getPageToken(final String emailAddress) {
        return pageTokenCache.get(emailAddress);
    }

    /**
     * <p>
     * Stores the sync token in cache
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the page token
     */
    public void putPageToken(final String emailAddress, final String token) {
        pageTokenCache.put(emailAddress, token);
    }

    /**
     * <p>
     * Clears the token stored in the cache based on the key provided.
     * </p>
     *
     * @param emailAddress Represents the email address.
     * @param tokenName    Represents the name of the token to be removed.
     */
    public void clearToken(final String emailAddress, final TokenType tokenName) {
        final Cache<String, String> tokenCache = getTokenCache(tokenName);

        if (Objects.nonNull(tokenCache)) {
            tokenCache.remove(emailAddress);
        }
    }

    /**
     * <p>
     * Gets the token cache based on the key.
     * </p>
     *
     * @param tokenType Represents the token type.
     * @return {@link Cache}
     */
    private Cache<String, String> getTokenCache(final TokenType tokenType) {

        return switch (tokenType) {
            case SYNC_TOKEN -> syncTokenCache;
            case PAGE_TOKEN -> pageTokenCache;
            default -> null;
        };
    }
}
