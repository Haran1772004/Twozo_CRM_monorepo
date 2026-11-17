package com.tlc.crm.zapier.api.models;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.models.Action;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

/**
 * Represents the {@link ZapEvent} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ZapEvent {

    CONTACT_ADDED("contactAdded", (short) 1),
    CONTACT_UPDATED("contactUpdated", (short) 2),
    COMPANY_ADDED("companyAdded", (short) 3),
    COMPANY_UPDATED("companyUpdated", (short) 4),
    DEAL_ADDED("dealAdded", (short) 5),
    DEAL_UPDATED("dealUpdated", (short) 6),
    UPDATE_DEAL_STAGE("dealStageUpdated", (short) 7),
    ACTIVITY_ADDED("activityAdded", (short) 8),
    ACTIVITY_UPDATED("activityUpdated", (short) 9),
    PRODUCT_ADDED("productAdded", (short) 10),
    PRODUCT_UPDATED("productUpdated", (short) 11),
    ;

    private final String aliasName;
    private final short id;

    ZapEvent(final String aliasName, final short id) {
        this.aliasName = aliasName;
        this.id = id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public short getId() {
        return id;
    }

    /**
     * Gets the {@link ZapEvent} by its id.
     *
     * @param id Refers the zap event id.
     * @return The {@link ZapEvent}.
     */
    public static ZapEvent get(final short id) {
        return switch (id) {
            case 1 -> CONTACT_ADDED;
            case 2 -> CONTACT_UPDATED;
            case 3 -> COMPANY_ADDED;
            case 4 -> COMPANY_UPDATED;
            case 5 -> DEAL_ADDED;
            case 6 -> DEAL_UPDATED;
            case 7 -> UPDATE_DEAL_STAGE;
            case 8 -> ACTIVITY_ADDED;
            case 9 -> ACTIVITY_UPDATED;
            case 10 -> PRODUCT_ADDED;
            case 11 -> PRODUCT_UPDATED;
            default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
        };
    }

    /**
     * Retrieves the corresponding {@link ZapEvent} based on the given source and action.
     *
     * @param source The source entity triggering the event.
     * @param action The action performed on the source.
     * @return The matching {@link ZapEvent}.
     */
    public static ZapEvent getBySource(final Source source, final Action action) {
        return switch (source) {
            case COMPANY -> switch (action) {
                case CREATE -> COMPANY_ADDED;
                case UPDATE, PARTIAL_UPDATE -> COMPANY_UPDATED;
                default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
            };
            case CONTACT -> switch (action) {
                case CREATE -> CONTACT_ADDED;
                case UPDATE, PARTIAL_UPDATE -> CONTACT_UPDATED;
                default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
            };
            case DEAL -> switch (action) {
                case CREATE -> DEAL_ADDED;
                case UPDATE, PARTIAL_UPDATE -> DEAL_UPDATED;
                default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
            };
            case ACTIVITY -> switch (action) {
                case CREATE -> ACTIVITY_ADDED;
                case UPDATE, PARTIAL_UPDATE -> ACTIVITY_UPDATED;
                default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
            };
            case PRODUCT -> switch (action) {
                case CREATE -> PRODUCT_ADDED;
                case UPDATE, PARTIAL_UPDATE -> PRODUCT_UPDATED;
                default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
            };
            default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
        };
    }

    /**
     * Retrieves the corresponding {@link ZapEvent} based on the given type.
     *
     * @param type The type of the event.
     * @return The matching {@link ZapEvent}.
     */
    public static ZapEvent getByType(final String type) {
        return switch (type) {
            case ZapAttributes.UPDATE_STAGE -> UPDATE_DEAL_STAGE;
            default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_EVENT, "i18n.crm.zapier.event.invalid");
        };
    }

}
