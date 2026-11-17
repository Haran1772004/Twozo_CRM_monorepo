package com.tlc.crm.email.notification.email.sync.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.email.notification.email.api.model.config.EmailFolderSyncDateConfig;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;

import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.common.sync.SyncSettings;

import java.util.Objects;

/**
 * <p>
 * Represents the email related {@link SyncSettings} fields.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SyncSettings
 */
public class EmailSyncSettings extends SyncSettings {

    private boolean isSynced;
    private ConnectionStatus connectionStatus;
    private EmailSyncConfig emailSyncConfig;
    private EmailFolderSyncDateConfig emailFolderSyncDateConfig;
    private SyncInterval syncInterval;
    private EmailSyncConfig inProgressSyncConfig;

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(final boolean synced) {
        isSynced = synced;
    }

    @Override
    public Object identity() {
        return null;
    }

    public EmailSyncConfig getEmailSyncConfig() {
        return emailSyncConfig;
    }

    public void setEmailSyncConfig(final EmailSyncConfig emailSyncConfig) {
        this.emailSyncConfig = emailSyncConfig;
    }

    public EmailFolderSyncDateConfig getEmailFolderSyncDateConfig() {
        return emailFolderSyncDateConfig;
    }

    public void setEmailFolderSyncDateConfig(final EmailFolderSyncDateConfig emailFolderSyncDateConfig) {
        this.emailFolderSyncDateConfig = emailFolderSyncDateConfig;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(final ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void setInProgressSyncConfig(final EmailSyncConfig inProgressConfig) {
        final JsonObject inProgressConfigAsJson = inProgressConfig.configAsJson();

        this.inProgressSyncConfig = inProgressConfig;
        this.syncInterval = SyncInterval.get(inProgressConfigAsJson.getShort(EmailFields.DATE_INTERVAL.getName()));
    }

    public void clearInProgressConfig() {
        this.inProgressSyncConfig = null;
    }

    /**
     * Retrieve the {@link EmailSyncConfig} depending on the sync status. If the status is "in progress,"
     * it returns the configuration associated with that status; otherwise, it returns the sync configuration.
     *
     * @return {@link EmailSyncConfig}
     */
    public EmailSyncConfig getDisplaySyncConfig() {
        return SyncStatus.IN_PROGRESS.equals(getSyncStatus()) ? inProgressSyncConfig : emailSyncConfig;
    }

    public EmailSyncConfig getInProgressSyncConfig() {
        return inProgressSyncConfig;
    }

    public SyncInterval getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(final SyncInterval syncInterval) {
        this.syncInterval = syncInterval;
    }

    /**
     * <p>
     * Representing the email folder synchronization date interval
     * </p>
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum SyncInterval {

        LAST_ONE_MONTH("Last One Month", (short) 1, (short) 1),
        LAST_THREE_MONTHS("Last Three Months", (short) 2, (short) 3),
        LAST_SIX_MONTHS("Last Six Months", (short) 3, (short) 6);

        private final String name;
        private final short id;
        private final short value;

        SyncInterval(final String name, final short id, final short value) {
            this.name = Objects.requireNonNull(name);
            this.id = id;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public short getId() {
            return id;
        }

        public short getValue() {
            return value;
        }

        /**
         * <p>
         * Gets the {@link SyncInterval} by id
         * </p>
         *
         * @param id Represents the synchronization date interval id
         * @return {@link SyncInterval}
         */
        public static SyncInterval get(final short id) {
            return switch (id) {
                case 1 -> LAST_ONE_MONTH;
                case 2 -> LAST_THREE_MONTHS;
                case 3 -> LAST_SIX_MONTHS;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_DATE);
            };
        }
    }
}
