package com.tlc.email.server.fields;

/**
 * <p>
 * Represents the {@link SyncSettingFields} related fields.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class SyncSettingFields {

    private SyncSettingFields() {
    }

    public static final String ID = "id";
    public static final String SYNCED_ADDRESS = "syncedAddress";
    public static final String LAST_SYNC_TIME = "lastSyncTime";
    public static final String DATE_CONFIG = "dateConfig";
    public static final String IS_SYNCED = "isSynced";
    public static final String CONNECTION_STATUS = "connectionStatus";
    public static final String FOLDER_INTERVALS = "folderIntervals";
    public static final String SYNC_STATUS = "syncStatus";
}
