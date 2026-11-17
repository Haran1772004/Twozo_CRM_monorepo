package com.tlc.crm.core.internal.async.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.notification.model.sync.SyncStatus;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Manages the storage and retrieval of contact synchronization {@link SyncStatus} task.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class ContactSyncCache {

    private final Cache<Long, SyncInfo> contactSyncInfoCache;

    private static class Instance {
        private static final ContactSyncCache INSTANCE = new ContactSyncCache();
    }

    private ContactSyncCache() {
        final CacheConfig<Long, SyncInfo> syncInfo = new CacheConfig<>(ContactFields.CONTACT_SYNC_INFO);

        syncInfo.setTtl((int) TimeUnit.MINUTES.toMillis(30));
        this.contactSyncInfoCache = CacheServiceProvider.get().createOrOpenCache(syncInfo);
    }

    /**
     * <p>
     * Gets the instance of {@link ContactSyncCache}
     * </p>
     *
     * @return {@link ContactSyncCache}
     */
    public static ContactSyncCache getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link SyncInfo} from the cache based on the provided synchronization id.
     * </p>
     *
     * @param syncId Represents the synchronization id
     * @return {@link SyncInfo}
     */
    public SyncInfo get(final long syncId) {
        return contactSyncInfoCache.get(syncId);
    }

    /**
     * <p>
     * Puts the {@link SyncInfo} into the cache based on the provided synchronization id.
     * </p>
     *
     * @param syncId Represents the synchronization id
     */
    public void put(final long syncId, final SyncInfo syncInfo) {
        contactSyncInfoCache.put(syncId, syncInfo);
    }

    /**
     * <p>
     * Removes the {@link SyncInfo} associated with the provided synchronization id from the cache.
     * </p>
     *
     * @param syncId Represents the synchronization id
     */
    public void remove(final long syncId) {
        contactSyncInfoCache.remove(syncId);
    }

    /**
     * <p>
     * Clears the {@link SyncInfo} from the cache.
     * </p>
     */
    public void clear() {
        contactSyncInfoCache.clear();
    }
}

