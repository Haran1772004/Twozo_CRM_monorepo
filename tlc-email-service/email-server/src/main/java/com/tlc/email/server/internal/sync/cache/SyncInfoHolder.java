package com.tlc.email.server.internal.sync.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.email.common.cache.RemoteCache;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.email.server.fields.EmailFields;

import com.tlc.notification.model.sync.SyncStatus;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Manages the storage and retrieval of email synchronization {@link SyncStatus} task.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
final class SyncInfoHolder implements RemoteCache {

    private final Cache<String, SyncStatusInfo> emailSyncInfoCache;

    private SyncInfoHolder() {
        final CacheConfig<String, SyncStatusInfo> syncInfo = new CacheConfig<>(EmailFields.EMAIL_SYNC_INFO);

        syncInfo.setTtl((int) TimeUnit.MINUTES.toMillis(15));
        this.emailSyncInfoCache = CacheServiceProvider.get().createOrOpenCache(syncInfo);
    }

    private static class Instance {
        private static final SyncInfoHolder INSTANCE = new SyncInfoHolder();
    }

    /**
     * <p>
     * Gets the instance of {@link SyncInfoHolder}
     * </p>
     *
     * @return {@link SyncInfoHolder}
     */
    public static SyncInfoHolder getInstance() {
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
     * Gets the {@link SyncStatusInfo} from the cache based on the provided {@link EmailAddress}.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link SyncStatusInfo}
     */
    public SyncStatusInfo get(final String emailAddress) {
        return emailSyncInfoCache.get(emailAddress);
    }

    /**
     * <p>
     * Puts the {@link SyncStatusInfo} into the cache based on the provided {@link EmailAddress}.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     */
    public void put(final String emailAddress, final SyncStatusInfo syncStatusInfo) {
        emailSyncInfoCache.put(emailAddress, syncStatusInfo);
    }

    /**
     * <p>
     * Removes the {@link SyncStatusInfo} associated with the provided {@link EmailAddress} from the cache.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     */
    public void remove(final String emailAddress) {
        emailSyncInfoCache.remove(emailAddress);
    }

    /**
     * <p>
     * Clears the {@link SyncStatusInfo} from the cache.
     * </p>
     */
    public void clear() {
        emailSyncInfoCache.clear();
    }
}
