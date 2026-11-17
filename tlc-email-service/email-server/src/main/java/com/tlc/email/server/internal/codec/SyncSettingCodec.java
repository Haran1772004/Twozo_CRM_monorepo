package com.tlc.email.server.internal.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.codec.AbstractEmailCodec;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.config.FolderInterval;

import com.tlc.email.server.fields.FolderIntervalFields;
import com.tlc.email.server.fields.SyncSettingFields;
import com.tlc.email.server.model.SyncSetting;

import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Responsible for encoding and decoding receive request data related to {@link SyncSetting}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class SyncSettingCodec extends AbstractEmailCodec<SyncSetting> {

    private SyncSettingCodec() {
    }

    /**
     * <p>
     * Holds the single instance of {@link SyncSettingCodec}.
     * </p>
     */
    private static class Instance {
        private static final SyncSettingCodec INSTANCE = new SyncSettingCodec();
    }

    public static SyncSettingCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param syncSetting the {@linkSyncSetting} to be encoded
     * @return the bytes of data
     */
    @Override
    public byte[] encode(final SyncSetting syncSetting) {
        final JsonObject jsonObject = Json.object();
        final JsonObject folderIntervalArray = Json.object();

        jsonObject.put(SyncSettingFields.ID, syncSetting.id())
                .put(SyncSettingFields.SYNCED_ADDRESS, getSyncEmailAddressAsJson(syncSetting.getEmailAddress()))
                .put(SyncSettingFields.LAST_SYNC_TIME, syncSetting.getLastSyncTime())
                .put(SyncSettingFields.IS_SYNCED, syncSetting.isSynced())
                .put(SyncSettingFields.CONNECTION_STATUS, syncSetting.getConnectionStatus().getId())
                .put(SyncSettingFields.SYNC_STATUS, syncSetting.getSyncStatus().getId());
        final Collection<FolderInterval> folderIntervals = syncSetting.getFolderIntervals();

        if (Objects.nonNull(folderIntervals)) {

            for (final FolderInterval interval : folderIntervals) {
                final JsonArray intervalJson = Json.array();

                interval.getEmailIntervals().forEach(emailInterval -> {
                    final JsonObject emailIntervalJson = Json.object();

                    emailIntervalJson.put(FolderIntervalFields.START_TIME, emailInterval.startTime())
                            .put(FolderIntervalFields.END_TIME, emailInterval.endTime());

                    intervalJson.put(emailIntervalJson);
                });
                folderIntervalArray.put(interval.getFolder().name(), intervalJson);
            }
        }
        jsonObject.put(SyncSettingFields.FOLDER_INTERVALS, folderIntervalArray);

        return jsonObject.getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return {@link SyncSetting}
     */
    @Override
    public SyncSetting decode(final byte[] data) {
        final JsonObject syncSettingJson = Json.object(data);
        final SyncSetting syncSetting = new SyncSetting();

        syncSetting.setId(syncSettingJson.getLong(SyncSettingFields.ID));
        syncSetting.setEmailAddress(getSyncEmailAddress(syncSettingJson.getJsonObject(SyncSettingFields.SYNCED_ADDRESS)));
        syncSetting.setSyncStatus(SyncStatus.getStatus(syncSettingJson.getShort(SyncSettingFields.SYNC_STATUS)));
        syncSetting.setConnectionStatus(ConnectionStatus.get(syncSettingJson.getShort(SyncSettingFields.CONNECTION_STATUS)));
        syncSetting.setSynced(syncSettingJson.optBoolean(SyncSettingFields.IS_SYNCED));

        if (syncSettingJson.containsKey(SyncSettingFields.LAST_SYNC_TIME)) {
            syncSetting.setLastSyncTime(syncSettingJson.getLong(SyncSettingFields.LAST_SYNC_TIME));
        }
        final JsonObject folderIntervalJson = syncSettingJson.getJsonObject(SyncSettingFields.FOLDER_INTERVALS);
        final Collection<String> folderNames = folderIntervalJson.keySet();
        final Collection<FolderInterval> folderIntervals = new ArrayList<>();

        folderNames.forEach(folderName -> {
            final FolderInterval folderInterval = new FolderInterval();
            final JsonArray jsonArray = folderIntervalJson.getJsonArray(folderName);
            final int intervalSize = jsonArray.size();

            for (int intervalIndex = 0; intervalIndex < intervalSize; intervalIndex++) {
                final JsonObject jsonObject = jsonArray.getJsonObject(intervalIndex);

                folderInterval.addEmailInterval(new EmailInterval(jsonObject.getLong(FolderIntervalFields.START_TIME),
                        jsonObject.getLong(FolderIntervalFields.END_TIME)));
            }
            folderInterval.setFolder(FolderType.valueOf(folderName));

            folderIntervals.add(folderInterval);
        });
        syncSetting.addAll(folderIntervals);

        return syncSetting;
    }
}
