package com.tlc.crm.core.internal.async.cache;

import com.tlc.crm.core.internal.contact.sync.ContactSyncService;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.crm.entity.async.cache.SyncInfoHolderResolver;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the services of {@link SyncInfo} that hold the contact-related synchronization information.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see SyncInfoHolderResolver
 */
public final class ContactSyncInfoResolver implements SyncInfoHolderResolver {

    private final ContactSyncCache contactSyncCache;
    private final ReentrantReadWriteLock lock;
    private final ContactSyncService contactSyncService;

    private ContactSyncInfoResolver() {
        this.contactSyncCache = ContactSyncCache.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.contactSyncService = ContactSyncService.getInstance();
    }

    private static class Instance {
        private static final ContactSyncInfoResolver INSTANCE = new ContactSyncInfoResolver();
    }

    /**
     * <p>
     * Gets the instance of {@link ContactSyncInfoResolver}.
     * </p>
     *
     * @return {@link ContactSyncInfoResolver}
     */
    public static ContactSyncInfoResolver getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the synchronization id
     * @return {@link SyncInfo}
     */
    @Override
    public SyncInfo resolve(final long orgId, final Long syncId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final SyncInfo contactSyncInfo;

        readLock.lock();

        try {
            contactSyncInfo = contactSyncCache.get(syncId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(contactSyncInfo)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final SyncInfo syncInfo = contactSyncService.getContactSyncInfo(orgId, syncId);

                if (Objects.nonNull(syncInfo)) {
                    contactSyncCache.put(syncId, syncInfo);
                }

                return syncInfo;
            } finally {
                writeLock.unlock();
            }
        }

        return contactSyncInfo;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    @Override
    public void setSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo) {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            contactSyncService.updateContactSyncStatus(orgId, userId, syncInfo);
            contactSyncCache.put(syncInfo.syncId(), Objects.requireNonNull(syncInfo));
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return true, if the contact sync is in progress
     */
    @Override
    public boolean isSyncInProgress(final long orgId, final Long syncId) {
        final SyncInfo syncInfo = resolve(orgId, syncId);

        return syncInfo.syncStatus() == SyncStatus.IN_PROGRESS;
    }
}

