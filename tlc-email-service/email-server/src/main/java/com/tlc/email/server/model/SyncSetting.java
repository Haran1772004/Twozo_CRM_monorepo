package com.tlc.email.server.model;

import com.tlc.email.common.model.EmailModel;
import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.email.common.model.config.FolderInterval;

import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * Represents the {@link SyncSetting}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailModel
 */
public class SyncSetting implements EmailModel {

    private boolean isSynced;
    private Long id;
    private Long lastSyncTime;
    private ConnectionStatus connectionStatus;
    private SyncStatus syncStatus;
    private SyncAddressInfo emailAddress;
    private Collection<FolderInterval> folderIntervals;

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(final boolean synced) {
        isSynced = synced;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(final Long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(final ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(final SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public SyncAddressInfo getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final SyncAddressInfo emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Collection<FolderInterval> getFolderIntervals() {
        return folderIntervals;
    }

    public void add(final FolderInterval folderInterval) {
        this.folderIntervals = this.folderIntervals == null ? new ArrayList<>() : folderIntervals;

        this.folderIntervals.add(folderInterval);
    }

    public void addAll(final Collection<FolderInterval> folderIntervals) {
        this.folderIntervals = folderIntervals;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return id
     */
    @Override
    public Long id() {
        return id;
    }
}
