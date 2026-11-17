package com.tlc.email.common.model.sync;

import com.tlc.email.common.model.config.FolderInterval;

import com.tlc.notification.model.sync.SyncStatus;

import java.io.Serializable;

import java.util.Collection;

/**
 * Represents sync status information for an email address.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class SyncStatusInfo implements Serializable {

    private final Long syncId;
    private final String emailAddress;
    private final SyncStatus syncStatus;
    private Long lastSyncedTime;
    private Collection<FolderInterval> folderIntervals;

    public SyncStatusInfo(final Long syncId, final String emailAddress, final SyncStatus syncStatus) {
        this.syncId = syncId;
        this.emailAddress = emailAddress;
        this.syncStatus = syncStatus;
    }

    public Long getSyncId() {
        return syncId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setLastSyncedTime(final Long lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
    }

    public Long getLastSyncedTime() {
        return lastSyncedTime;
    }

    public void setFolderIntervals(final Collection<FolderInterval> folderIntervals) {
        this.folderIntervals = folderIntervals;
    }

    public Collection<FolderInterval> getFolderIntervals() {
        return folderIntervals;
    }
}