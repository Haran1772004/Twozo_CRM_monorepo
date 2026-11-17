package com.tlc.email.server.internal.sync.cache;

import com.tlc.email.common.model.sync.SyncStatusInfo;

import com.tlc.notification.model.sync.SyncStatus;

import java.time.Instant;

/**
 * <p>
 * Provides write services for {@link SyncStatusInfo} related actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncInfoWriteService {

    static SyncInfoWriteService get() {
        return SyncInfoServiceImpl.getInstance();
    }

    /**
     * <p>
     * Update the {@link SyncStatus}.
     * </p>
     *
     * @param syncStatusInfo Represents the {@link SyncStatusInfo}
     */
    default void updateStatus(final SyncStatusInfo syncStatusInfo) {
        final SyncStatus syncStatus = syncStatusInfo.getSyncStatus();

        if (SyncStatus.SYNCED == syncStatus || SyncStatus.FAILED == syncStatus) {
            syncStatusInfo.setLastSyncedTime(Instant.now().toEpochMilli());
        }
        setSyncStatus(syncStatusInfo);
    }

    /**
     * <p>
     * Sets the {@link SyncStatus} to the cache.
     * </p>
     *
     * @param syncInfo Represents the {@link SyncStatusInfo}
     */
    void setSyncStatus(final SyncStatusInfo syncInfo);
}

