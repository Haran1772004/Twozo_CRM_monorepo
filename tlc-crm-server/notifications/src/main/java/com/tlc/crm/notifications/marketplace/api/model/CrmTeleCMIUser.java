package com.tlc.crm.notifications.marketplace.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.telecmi.TeleCMIUser;

import java.util.Objects;

/**
 * <p>
 * Represents an agent in the TeleCMI telephony
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public class CrmTeleCMIUser extends TeleCMIUser {

    private Long orgId;
    private SyncStatus syncStatus;
    private CrmUser crmUser;
    private Long marketplaceAppId;

    public CrmTeleCMIUser() {
    }

    public CrmTeleCMIUser(final JsonObject userJson) {
        final Integer syncStatusId = userJson.optNullableValue(TelephoneFields.SYNC_STATUS, null);

        this.syncStatus = Objects.isNull(syncStatusId) ? null : SyncStatus.getSyncStatusById(syncStatusId);

        setId(userJson.optNullableValue(TelephoneFields.ID, null));
        setExtension(userJson.optNullableValue(TelephoneFields.EXTENSION, null));
        setPassword(userJson.getString(TelephoneFields.PASSWORD));
        setPhoneNumber(userJson.getString(TelephoneFields.PHONE_NUMBER));
    }

    public JsonObject constructJson() {
        final JsonObject userJson = Json.object();

        userJson.put(TelephoneFields.ID, getId());
        userJson.put(TelephoneFields.PASSWORD, getPassword());
        userJson.put(TelephoneFields.PHONE_NUMBER, getPhoneNumber());
        userJson.put(TelephoneFields.SYNC_STATUS, Objects.isNull(syncStatus) ? null : syncStatus.getId());
        userJson.put(TelephoneFields.EXTENSION, getExtension());

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

        /**
         * <p>
         * Gets the {@link SyncStatus}
         * </p>
         *
         * @param name Represents the sync status name
         * @return {@link SyncStatus}
         */
        public static SyncStatus getSyncStatusByName(final String name) {
            return switch (name.toLowerCase()) {
                case "connected" -> CONNECTED;
                case "failed" -> FAILED;
                default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_USER_SYNC_STATUS);
            };
        }

        /**
         * <p>
         * Gets the {@link SyncStatus}
         * </p>
         *
         * @param id Represents the id
         * @return @link SyncStatus}
         */
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