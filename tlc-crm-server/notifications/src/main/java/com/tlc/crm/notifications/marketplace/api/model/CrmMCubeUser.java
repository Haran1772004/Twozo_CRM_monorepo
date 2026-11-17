package com.tlc.crm.notifications.marketplace.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.mcube.MCubeUser;

import java.util.Objects;

/**
 * Represents an agent in the MCube telephony system within the CRM.
 *
 * @author Sathish
 * @version 1.0
 */
public class CrmMCubeUser extends MCubeUser {

    /** Organization ID this MCube user belongs to. */
    private Long orgId;

    /** Synchronization status of this user with the MCube provider. */
    private SyncStatus syncStatus;

    /** The CRM user associated with this MCube agent. */
    private CrmUser crmUser;

    /** Marketplace application ID for the MCube integration. */
    private Long marketplaceAppId;

    public CrmMCubeUser() {
    }

    /**
     * Constructs a CRM MCube user from a JSON configuration object.
     *
     * @param userJson the JSON object containing user configuration (id, extension, phone, sync status)
     */
    public CrmMCubeUser(final JsonObject userJson) {
        syncStatus = userJson.containsKey(TelephoneFields.SYNC_STATUS)
                ? CrmMCubeUser.SyncStatus.getSyncStatusById(userJson.getInt(TelephoneFields.SYNC_STATUS)) : CrmMCubeUser.SyncStatus.FAILED;

        setId(userJson.optNullableValue(TelephoneFields.ID, null));
        setEmail(userJson.optNullableValue(TelephoneFields.EMAIL, null));
        setPhoneNumber(userJson.getString(TelephoneFields.PHONE_NUMBER));
    }

    /**
     * Serializes the MCube user configuration into a JSON object for storage.
     *
     * @return JSON representation of the user's MCube-specific fields
     */
    public JsonObject constructJson() {
        final JsonObject userJson = Json.object();

        userJson.put(TelephoneFields.ID, getId());
        userJson.put(TelephoneFields.EMAIL, getEmail());
        userJson.put(TelephoneFields.PHONE_NUMBER, getPhoneNumber());
        userJson.put(TelephoneFields.SYNC_STATUS, Objects.isNull(syncStatus) ? null : syncStatus.getId());

        return userJson;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(final SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public Long getMarketplaceAppId() {
        return marketplaceAppId;
    }

    public void setMarketplaceAppId(final Long marketplaceAppId) {
        this.marketplaceAppId = marketplaceAppId;
    }

    /**
     * Represents the synchronization status of an MCube user with the provider.
     */
    public enum SyncStatus {

        CONNECTED("Connected", 1),
        FAILED("Failed", 2),
        ;

        private final String name;
        private final int id;

        SyncStatus(final String name, final int id) {
            this.name = name;
            this.id = id;
        }

        public static SyncStatus getSyncStatusByName(final String name) {
            return switch (name.toLowerCase()) {
                case "connected" -> CONNECTED;
                case "failed" -> FAILED;
                default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_USER_SYNC_STATUS);
            };
        }

        public static SyncStatus getSyncStatusById(final int id) {
            return switch (id) {
                case 1 -> CONNECTED;
                case 2 -> FAILED;
                default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_USER_SYNC_STATUS);
            };
        }

        public boolean isConnected() {
            return this == CONNECTED;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
