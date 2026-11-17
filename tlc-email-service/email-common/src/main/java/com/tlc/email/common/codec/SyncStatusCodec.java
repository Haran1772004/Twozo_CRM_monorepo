package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.converter.FolderIntervalConverter;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.config.FolderInterval;
import com.tlc.email.common.model.sync.SyncStatusInfo;

import com.tlc.notification.model.sync.SyncStatus;

import java.util.Collection;
import java.util.Objects;

/**
 * Responsible for encoding and decoding {@link SyncStatusInfo} request data.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractEmailCodec
 */
public final class SyncStatusCodec extends AbstractEmailCodec<SyncStatusInfo> {

    private final FolderIntervalConverter folderIntervalConverter;

    private SyncStatusCodec() {
        this.folderIntervalConverter = FolderIntervalConverter.getInstance();
    }

    /**
     * <p>
     * Holds the single instance of {@link SyncStatusCodec}.
     * </p>
     */
    private static class Instance {
        private static final SyncStatusCodec INSTANCE = new SyncStatusCodec();
    }

    public static SyncStatusCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param syncStatusInfo the {@link SyncStatusInfo} to be encoded
     * @return the encoded bytes
     */
    @Override
    public byte[] encode(final SyncStatusInfo syncStatusInfo) {
        final JsonObject jsonObject = Json.object();
        final Collection<FolderInterval> folderIntervals = syncStatusInfo.getFolderIntervals();

        jsonObject.put(EmailFields.ID, syncStatusInfo.getSyncId());
        jsonObject.put(EmailFields.EMAIL_ADDRESS, syncStatusInfo.getEmailAddress());
        jsonObject.put(EmailFields.SYNC_STATUS, syncStatusInfo.getSyncStatus().getId());
        jsonObject.put(EmailFields.LAST_SYNCED_TIME, syncStatusInfo.getLastSyncedTime());

        if (Objects.nonNull(folderIntervals)) {
            jsonObject.put(EmailFields.DATE_CONFIG, folderIntervalConverter.getDateConfigAsJson(syncStatusInfo.getFolderIntervals()));
        }

        return jsonObject.getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return {@link SyncStatusInfo}
     */
    @Override
    public SyncStatusInfo decode(final byte[] data) {
        final JsonObject syncStatusRequestJson = Json.object(data);
        final SyncStatusInfo syncStatusInfo = new SyncStatusInfo(
                syncStatusRequestJson.getLong(EmailFields.ID),
                syncStatusRequestJson.getString(EmailFields.EMAIL_ADDRESS),
                SyncStatus.getStatus(syncStatusRequestJson.getShort(EmailFields.SYNC_STATUS)));

        syncStatusInfo.setLastSyncedTime(syncStatusRequestJson.getLong(EmailFields.LAST_SYNCED_TIME));

        if (syncStatusRequestJson.containsKey(EmailFields.DATE_CONFIG)) {
            syncStatusInfo.setFolderIntervals(folderIntervalConverter.getFolderIntervals(syncStatusRequestJson.getJsonObject(EmailFields.DATE_CONFIG)));
        }

        return syncStatusInfo;
    }
}
