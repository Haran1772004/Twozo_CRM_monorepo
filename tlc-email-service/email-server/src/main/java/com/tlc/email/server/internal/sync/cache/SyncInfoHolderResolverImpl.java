package com.tlc.email.server.internal.sync.cache;

import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.email.server.internal.manager.SyncSettingsManager;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the services of {@link SyncStatusInfo} that hold the email-related synchronization information.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SyncInfoHolderResolver
 */
final class SyncInfoHolderResolverImpl implements SyncInfoHolderResolver {

    private final SyncInfoHolder syncInfoHolder;
    private final SyncSettingsManager syncSettingsManager;
    private final ReentrantReadWriteLock lock;

    private SyncInfoHolderResolverImpl() {
        this.syncInfoHolder = SyncInfoHolder.getInstance();
        this.syncSettingsManager = SyncSettingsManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final SyncInfoHolderResolverImpl INSTANCE = new SyncInfoHolderResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link SyncInfoHolderResolverImpl}.
     * </p>
     *
     * @return {@link SyncInfoHolderResolverImpl}
     */
    public static SyncInfoHolderResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link SyncStatusInfo}
     */
    @Override
    public SyncStatusInfo resolve(final String emailAddress) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final SyncStatusInfo emailSyncInfo;

        readLock.lock();

        try {
            emailSyncInfo = syncInfoHolder.get(emailAddress);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(emailSyncInfo)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final SyncStatusInfo syncInfo = syncSettingsManager.getSyncInfo(emailAddress);

                if (Objects.nonNull(syncInfo)) {
                    syncInfoHolder.put(emailAddress, syncInfo);
                }

                return syncInfo;
            } finally {
                writeLock.unlock();
            }
        }

        return emailSyncInfo;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncInfo Represents the {@link SyncStatusInfo}
     */
    @Override
    public void setSyncStatus(final SyncStatusInfo syncInfo) {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            syncSettingsManager.updateSyncStatus(syncInfo);
            syncInfoHolder.put(syncInfo.getEmailAddress(), Objects.requireNonNull(syncInfo));
        } finally {
            writeLock.unlock();
        }
    }
}
