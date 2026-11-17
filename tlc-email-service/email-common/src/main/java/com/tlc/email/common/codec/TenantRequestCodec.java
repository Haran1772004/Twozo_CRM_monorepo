package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.config.TenantConfiguration;
import com.tlc.email.common.request.TenantSyncRequest;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Responsible for encoding and decoding receive request data related to email threads or individual emails.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see AbstractEmailCodec
 */
public class TenantRequestCodec extends AbstractEmailCodec<TenantSyncRequest> {

    private TenantRequestCodec() {
    }

    /**
     * <p>
     * Holds the single instance of {@link ThreadCodec}.
     * </p>
     */
    private static class Instance {
        private static final TenantRequestCodec INSTANCE = new TenantRequestCodec();
    }

    public static TenantRequestCodec getInstance() {
        return TenantRequestCodec.Instance.INSTANCE;
    }


    /**
     * Encodes the provided {@link TenantSyncRequest} into bytes
     * <p>
     * <strong>Caution:</strong> When encoding data for publication via the messaging queue, it is advisable to keep the data size minimal to efficiently handle large amounts of data transmission.
     * </p>
     *
     * @param tenantSyncRequest the {@link TenantSyncRequest} to be encoded
     * @return the resulting encoded bytes
     */
    @Override
    public byte[] encode(final TenantSyncRequest tenantSyncRequest) {
        final JsonObject syncRequestJson = Json.object();

        syncRequestJson.put(EmailFields.ID, tenantSyncRequest.id());
        syncRequestJson.put(EmailFields.CONFIGURATION, getConfigurationJson(tenantSyncRequest.getTenantConfiguration()));
        syncRequestJson.put(EmailFields.STATE_DATA, tenantSyncRequest.getStateData());

        return syncRequestJson.getBytes();
    }

    /**
     * Decodes the given bytes into a {@link TenantSyncRequest}
     *
     * @param data the bytes to decode
     * @return the {@link TenantSyncRequest}
     */
    @Override
    public TenantSyncRequest decode(byte[] data) {
        if (Objects.isNull(data) || data.length == 0) {
            return null;
        }

        final JsonObject syncRequestJson = Json.object(data);
        final TenantSyncRequest syncRequest = new TenantSyncRequest(getTenantConfiguration(syncRequestJson.getJsonObject(EmailFields.CONFIGURATION)));

        if (syncRequestJson.containsKey(EmailFields.ID)) {
            syncRequest.setId(syncRequestJson.getLong(EmailFields.ID));
        }
        syncRequest.setStateData(getStateDataAsJson(syncRequestJson));

        return syncRequest;
    }

    /**
     * <p>
     * Gets the {@link EmailReceiverConfiguration} as json.
     * </p>
     *
     * @param configuration Represents the {@link EmailReceiverConfiguration}
     * @return configuration {@link JsonObject}
     */
    private JsonObject getConfigurationJson(final TenantConfiguration configuration) {
        final JsonObject configurationJson = Json.object();
        final SyncAddressInfo emailAddress = configuration.getSyncEmailAddress();
        final Collection<FolderType> folderTypes = configuration.getFolderTypes();

        if (Objects.nonNull(folderTypes) && !folderTypes.isEmpty()) {
            final JsonArray folderTypeArray = Json.array();

            for (final FolderType folderType : folderTypes) {
                folderTypeArray.put(folderType.getId());
            }
            configurationJson.put(EmailFields.FOLDER_TYPES, folderTypeArray);
        }
        configurationJson.put(EmailFields.EMAIL_USER_ID, configuration.getEmailUserId());
        configurationJson.put(EmailFields.CONSENT_TYPE, emailAddress.getConsentType());
        configurationJson.put(EmailFields.EMAIL_ADDRESS, emailAddress.getEmailAddress());

        return configurationJson;
    }

    /**
     * <p>
     * Gets the {@link TenantConfiguration} from the given json.
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject}
     * @return {@link TenantConfiguration}
     */
    private TenantConfiguration getTenantConfiguration(final JsonObject jsonObject) {
        final TenantConfiguration tenantConfiguration = new TenantConfiguration();
        final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(jsonObject.getString(EmailFields.EMAIL_ADDRESS));

        syncAddressInfo.setConsentType(ConsentType.valueOf(jsonObject.getString(EmailFields.CONSENT_TYPE)));
        tenantConfiguration.setSyncEmailAddress(syncAddressInfo);

        if (jsonObject.containsKey(EmailFields.FOLDER_TYPES)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(EmailFields.FOLDER_TYPES);
            final Collection<FolderType> folderTypes = new HashSet<>();

            jsonArray.forEach(folderArray -> folderTypes.add(FolderType.get(((Long) folderArray).shortValue())));

            tenantConfiguration.setFolderTypes(folderTypes);
        }
        tenantConfiguration.setEmailUserId(jsonObject.getString(EmailFields.EMAIL_USER_ID));

        return tenantConfiguration;
    }
}