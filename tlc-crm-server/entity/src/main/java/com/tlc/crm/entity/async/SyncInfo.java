package com.tlc.crm.entity.async;

import com.tlc.notification.model.sync.SyncStatus;

import java.io.Serializable;

/**
 * <p>
 * Holds the synchronization status with syncTime (timestamp) and {@link SyncStatus}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see Serializable
 */
public record SyncInfo(long syncId, Long syncTime, SyncStatus syncStatus, String inProgressConfig) implements Serializable {

    public SyncInfo(final long syncId, final SyncStatus syncStatus) {
        this(syncId, null, syncStatus, null);
    }

    public SyncInfo(final long syncId, final SyncStatus syncStatus, final String inProgressConfig) {
        this(syncId, null, syncStatus, inProgressConfig);
    }

    @Override
    public String inProgressConfig() {
        return SyncStatus.IN_PROGRESS.equals(syncStatus) ? inProgressConfig : null;
    }
}

