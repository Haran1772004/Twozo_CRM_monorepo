package com.tlc.crm.notifications.calendar.api.model;

import com.tlc.crm.entity.models.settings.SyncSettings;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notifications.calendar.api.model.internal.CalendarSyncConfig;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A model to manage the settings of an calendar to be synchronized
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
@Notifiable(types = AppNotificationEventType.CALENDAR_SYNCED, modelName = CrmModelType.CALENDAR_SYNC)
public class CalendarSyncSettings implements TlcModel, SyncSettings {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.calendar.invalid.user.id")
    private Long userId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notifications.calendar.sync.invalid.config")
    private CalendarSyncConfig calendarSyncConfig;

    private EmailAddress address;
    private boolean isSyncEnabled;
    private SyncStatus syncStatus;
    private Long lastSyncTime;
    private String provider;
    private CalendarSyncConfig inProgressSyncConfig;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public EmailAddress getAddress() {
        return address;
    }

    public void setAddress(final EmailAddress address) {
        this.address = address;
    }

    public boolean isSyncEnabled() {
        return isSyncEnabled;
    }

    public void setSyncEnabled(final boolean syncEnabled) {
        this.isSyncEnabled = syncEnabled;
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

    public CalendarSyncConfig getCalendarSyncConfig() {
        return calendarSyncConfig;
    }

    public void setCalendarSyncConfig(final CalendarSyncConfig calendarSyncConfig) {
        this.calendarSyncConfig = calendarSyncConfig;
    }

    public Long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(final Long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public void setInProgressSyncConfig(final CalendarSyncConfig inProgressSyncConfig) {
        this.inProgressSyncConfig = inProgressSyncConfig;
    }

    /**
     * <p>
     * Retrieves the {@link CalendarSyncConfig} depending on the sync status. If the status is "in progress,"
     * it returns the configuration associated with that status; otherwise, it returns the sync configuration.
     * </p>
     *
     * @return {@link CalendarSyncConfig}
     */
    public CalendarSyncConfig getDisplaySyncConfig() {
        return SyncStatus.IN_PROGRESS.equals(syncStatus) ? inProgressSyncConfig : calendarSyncConfig;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
