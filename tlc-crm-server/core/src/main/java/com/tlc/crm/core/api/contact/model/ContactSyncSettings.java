package com.tlc.crm.core.api.contact.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.models.settings.SyncSettings;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents the contact sync settings
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@Notifiable(types = {AppNotificationEventType.CONTACT_SYNCED}, modelName = CrmModelType.CONTACT_SYNC)
public class ContactSyncSettings implements SyncSettings {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18.common.user.invalid.id")
    private Long userId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.contact.sync.invalid.address")
    private EmailAddress address;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.contact.sync.invalid.config")
    private Config config;
    private boolean syncStatus;
    private ContactSourceDetails addedInfo;
    private ContactSourceDetails updatedInfo;
    private ContactSourceDetails deletedInfo;
    private DateTime lastSyncTime;
    private long totalSyncedContacts;
    private String inProgressConfig;
    private String provider;

    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public EmailAddress getAddress() {
        return address;
    }

    @Override
    public void setAddress(final EmailAddress address) {
        this.address = address;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public void setUserId(final Long userId) {
        this.userId = userId;
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

    public Config getConfig() {
        return config;
    }

    public void setConfig(final String config) {
        this.config = new Config(config);
    }

    public boolean syncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(final boolean syncStatus) {
        this.syncStatus = syncStatus;
    }

    public ContactSourceDetails getAddedInfo() {
        return addedInfo;
    }

    public void setAddedInfo(final ContactSourceDetails addedInfo) {
        this.addedInfo = addedInfo;
    }

    public ContactSourceDetails getUpdatedInfo() {
        return updatedInfo;
    }

    public void setUpdatedInfo(final ContactSourceDetails updatedInfo) {
        this.updatedInfo = updatedInfo;
    }

    public ContactSourceDetails getDeletedInfo() {
        return deletedInfo;
    }

    public void setDeletedInfo(final ContactSourceDetails deletedInfo) {
        this.deletedInfo = deletedInfo;
    }

    public DateTime getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(final DateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public long getTotalSyncedContacts() {
        return totalSyncedContacts;
    }

    public void setTotalSyncedContacts(final long totalSyncedContacts) {
        this.totalSyncedContacts = totalSyncedContacts;
    }

    public Config getInProgressConfig() {
        return Objects.isNull(inProgressConfig) ? config : new Config(inProgressConfig);
    }

    public void setInProgressConfig(final String inProgressConfig) {
        this.inProgressConfig = inProgressConfig;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    /**
     * <p>
     * Inscribes the config opted by the user.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    public static class Config {

        private final JsonObject configAsJson;
        private final String syncWay;
        private final int groupTypeId;

        public Config(final String config) {
            this.configAsJson = Objects.isNull(config) || config.isBlank() ? Json.object() : Json.object(config);
            this.syncWay = configAsJson.optString(ContactFields.SYNC_WAY, null);
            this.groupTypeId = configAsJson.optInt(ContactFields.GROUP_OF_CONTACTS, 1);
        }

        /**
         * <p>
         * Gets the config in Json format.
         * </p>
         *
         * @return config in Json format.
         */
        public JsonObject getConfigAsJson() {
            return configAsJson;
        }

        /**
         * <p>
         * Gets the sync type
         * </p>
         *
         * @return the sync type.
         */
        public String getSyncWay() {
            return syncWay;
        }

        /**
         * <p>
         * Gets the contact group type id.
         * </p>
         *
         * @return contact group type id
         */
        public int getGroupTypeId() {
            return groupTypeId;
        }
    }
}