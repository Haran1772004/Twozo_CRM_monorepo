package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.request.SyncRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Responsible for encoding and decoding receive request data related to email threads or individual emails.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractEmailCodec
 */
public final class SyncRequestCodec extends AbstractEmailCodec<SyncRequest> {

    private SyncRequestCodec() {
    }

    /**
     * <p>
     * Holds the single instance of {@link SyncRequestCodec}.
     * </p>
     */
    private static class Instance {
        private static final SyncRequestCodec INSTANCE = new SyncRequestCodec();
    }

    public static SyncRequestCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param syncRequest the {@link SyncRequest} to be encoded
     * @return bytes of {@link SyncRequest}
     */
    @Override
    public byte[] encode(final SyncRequest syncRequest) {
        final JsonObject syncRequestJson = Json.object();

        syncRequestJson.put(EmailFields.ID, syncRequest.id());
        syncRequestJson.put(EmailFields.CONFIGURATION, getConfigurationJson(syncRequest.getReceiverConfiguration()));
        syncRequestJson.put(EmailFields.STATE_DATA, syncRequest.getStateData().toJson());

        return syncRequestJson.getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return {@link SyncRequest}
     */
    @Override
    public SyncRequest decode(final byte[] data) {

        if (Objects.isNull(data) || data.length == 0) {
            return null;
        }

        final JsonObject syncRequestJson = Json.object(data);
        final SyncRequest syncRequest = new SyncRequest(getConfiguration(syncRequestJson.getJsonObject(EmailFields.CONFIGURATION)));

        if (syncRequestJson.containsKey(EmailFields.ID)) {
            syncRequest.setId(syncRequestJson.getLong(EmailFields.ID));
        }
        syncRequest.setStateData(getStateDataAsJson(syncRequestJson));

        return syncRequest;
    }

    /**
     * <p>
     * Gets the {@link EmailReceiverConfiguration} from the given json.
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject}
     * @return {@link EmailReceiverConfiguration}
     */
    private EmailReceiverConfiguration getConfiguration(final JsonObject jsonObject) {
        final EmailReceiverConfiguration emailConfiguration = new EmailReceiverConfiguration();
        final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(jsonObject.getString(EmailFields.EMAIL_ADDRESS));

        syncAddressInfo.setConsentType(ConsentType.valueOf(jsonObject.getString(EmailFields.CONSENT_TYPE)));
        emailConfiguration.setSyncEmailAddress(syncAddressInfo);

        if (jsonObject.containsKey(EmailFields.INTERVAL)) {
            final Map<FolderType, EmailInterval> intervalMap = new HashMap<>();
            final JsonObject intervalJson = jsonObject.getJsonObject(EmailFields.INTERVAL);

            for (final FolderType folderType : FolderType.values()) {
                final String folderName = folderType.name();

                if (intervalJson.containsKey(folderName)) {
                    final JsonObject folderIntervalJson = intervalJson.getJsonObject(folderName);

                    intervalMap.put(FolderType.valueOf(folderName),
                            new EmailInterval(
                                    folderIntervalJson.getLong(EmailFields.START_TIME),
                                    folderIntervalJson.getLong(EmailFields.END_TIME)
                            )
                    );
                }
            }

            emailConfiguration.setEmailIntervals(intervalMap);
        }

        if (jsonObject.containsKey(EmailFields.FOLDER_TYPES)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(EmailFields.FOLDER_TYPES);
            final Collection<FolderType> folderTypes = new HashSet<>();

            jsonArray.forEach(folderArray -> folderTypes.add(FolderType.get(((Long) folderArray).shortValue())));

            emailConfiguration.setFolderTypes(folderTypes);
        }

        return emailConfiguration;
    }

    /**
     * <p>
     * Gets the {@link EmailReceiverConfiguration} as json.
     * </p>
     *
     * @param configuration Represents the {@link EmailReceiverConfiguration}
     * @return configuration {@link JsonObject}
     */
    private JsonObject getConfigurationJson(final EmailReceiverConfiguration configuration) {
        final JsonObject configurationJson = Json.object();
        final Map<FolderType, EmailInterval> emailIntervalMap = configuration.getEmailIntervals();
        final Collection<FolderType> folderTypes = configuration.getFolderTypes();
        final SyncAddressInfo emailAddress = configuration.getSyncEmailAddress();

        if (Objects.nonNull(emailIntervalMap) && !emailIntervalMap.isEmpty()) {
            final JsonObject folderIntervalsJson = Json.object();

            for (Map.Entry<FolderType, EmailInterval> entry : emailIntervalMap.entrySet()) {
                final FolderType folderType = entry.getKey();
                final EmailInterval interval = entry.getValue();

                if (Objects.nonNull(interval)) {
                    final JsonObject intervalJson = Json.object();

                    intervalJson.put(EmailFields.START_TIME, interval.startTime());
                    intervalJson.put(EmailFields.END_TIME, interval.endTime());

                    folderIntervalsJson.put(folderType.name(), intervalJson);
                }
            }
            configurationJson.put(EmailFields.INTERVAL, folderIntervalsJson);
        }

        if (Objects.nonNull(folderTypes) && !folderTypes.isEmpty()) {
            final JsonArray folderTypeArray = Json.array();

            for (final FolderType folderType : folderTypes) {
                folderTypeArray.put(folderType.getId());
            }
            configurationJson.put(EmailFields.FOLDER_TYPES, folderTypeArray);
        }

        configurationJson.put(EmailFields.CONSENT_TYPE, emailAddress.getConsentType());
        configurationJson.put(EmailFields.EMAIL_ADDRESS, emailAddress.getEmailAddress());

        return configurationJson;
    }
}
