package com.tlc.crm.common.sync;

import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import com.tlc.notification.model.sync.SyncStatus;

import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A model to manage the settings of an account to be synchronized
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class SyncSettings implements TlcModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.mail.invalid.user.id")
    private Long userId;

    @NotNull(groups = Group.Create.class, message = "i18n.notifications.mail.invalid.emailAddress")
    private SyncAddressInfo emailAddress;
    private Long lastSyncTime;
    private SyncStatus syncStatus;
    private boolean isSynced;
    private EmailProvider emailProvider;
    private boolean isDefault;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public SyncAddressInfo getAddress() {
        return emailAddress;
    }

    public void setAddress(final SyncAddressInfo emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(final Long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(final SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(final boolean isSynced) {
        this.isSynced = isSynced;
    }

    @Override
    public Object identity() {
        return null;
    }

    public EmailProvider getEmailProvider() {
        return emailProvider;
    }

    public void setEmailProvider(final EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }
}