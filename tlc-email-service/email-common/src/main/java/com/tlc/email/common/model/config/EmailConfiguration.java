package com.tlc.email.common.model.config;

import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.notification.model.NotificationConfiguration;

import java.util.Collection;

/**
 * <p>
 * An entity that holds the basic configuration for the email to be processed further
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public abstract class EmailConfiguration implements NotificationConfiguration {

    private SyncAddressInfo syncAddressInfo;

    private Collection<FolderType> folderTypes;
    private boolean includeMessageIds;

    public SyncAddressInfo getSyncEmailAddress() {
        return syncAddressInfo;
    }

    public void setSyncEmailAddress(final SyncAddressInfo syncAddressInfo) {
        this.syncAddressInfo = syncAddressInfo;
    }

    /**
     * <p>
     * Gets the instance of {@link EmailConfiguration}.
     * </p>
     *
     * @return the object which extends the {@link EmailConfiguration}
     */
    public final <T extends EmailConfiguration> T get() {
        return (T) this;
    }

    public Collection<FolderType> getFolderTypes() {
        return folderTypes;
    }

    public void setFolderTypes(Collection<FolderType> folderTypes) {
        this.folderTypes = folderTypes;
    }

    public boolean isIncludeMessageIds() {
        return includeMessageIds;
    }

    public void setIncludeMessageIds(final boolean includeMessageIds) {
        this.includeMessageIds = includeMessageIds;
    }
}