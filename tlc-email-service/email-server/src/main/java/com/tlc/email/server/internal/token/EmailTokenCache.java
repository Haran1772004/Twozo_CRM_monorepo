package com.tlc.email.server.internal.token;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.email.common.cache.RemoteCache;

import com.tlc.email.server.sync.EmailSyncTokenKey;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.model.EmailAddress;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Provides the cache services for email tokens
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailTokenCache implements RemoteCache {

    private final Cache<EmailSyncTokenKey, String> syncTokenCache;

    private EmailTokenCache() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<EmailSyncTokenKey, String> syncTokenCacheConfig = new CacheConfig<>(SyncServiceFields.SYNC_TOKEN_CACHE);

        syncTokenCacheConfig.setTtl((int) TimeUnit.DAYS.toMillis(6));
        syncTokenCache = cacheService.createOrOpenCache(syncTokenCacheConfig);
    }

    private static class Instance {
        private static final EmailTokenCache INSTANCE = new EmailTokenCache();
    }

    /**
     * <p>
     * Gets the {@link EmailTokenCache} instance
     * </p>
     *
     * @return {@link EmailTokenCache} instance
     */
    public static EmailTokenCache getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {

    }

    /**
     * <p>
     * Gets the sync token for respective email address
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return Sync token stored for every sync
     */
    public String getSyncToken(final EmailAddress emailAddress, final String folder) {
        return syncTokenCache.get(new EmailSyncTokenKey(emailAddress.orgId(), emailAddress.getEmailId(), folder));
    }

    /**
     * <p>
     * Stores the sync token in cache
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the sync token
     */
    public void putSyncToken(final EmailAddress emailAddress, final String token, final String folder) {
        final EmailSyncTokenKey syncTokenKey = new EmailSyncTokenKey(emailAddress.orgId(), emailAddress.getEmailId(), folder);

        syncTokenCache.put(syncTokenKey, token);
    }

    /**
     * <p>
     * Clears the sync token stored in cache
     * </p>
     */
    public void clearSyncToken() {
        syncTokenCache.clear();
    }
}
