package com.tlc.crm.messenger.campaign.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.template.WATemplate;

/**
 * <p>
 * Represents the WhatsApp campaign template status.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum WACampaignTemplateStatus {

    IN_REVIEW((short) 1),
    APPEAL_REQUESTED((short) 2),
    APPROVED((short) 3),
    PAUSED((short) 4),
    DISABLED((short) 5),
    REJECTED((short) 6),
    ;

    private final short id;

    WACampaignTemplateStatus(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link WACampaignTemplateStatus} by its id.
     * </p>
     *
     * @param id Represents the status id.
     * @return The {@link WACampaignTemplateStatus}.
     */
    public static WACampaignTemplateStatus get(final short id) {
        return switch (id) {
            case 1 -> IN_REVIEW;
            case 2 -> APPEAL_REQUESTED;
            case 3 -> APPROVED;
            case 4 -> PAUSED;
            case 5 -> DISABLED;
            case 6 -> REJECTED;
            default -> throw ErrorCode.get(WAErrorCodes.INVALID_WA_CAMPAIGN_TEMPLATE_STATUS, "i18n.whatsapp.campaign.invalid.template.status");
        };
    }

    /**
     * <p>
     * Converts this status to the equivalent {@link WATemplate.Status}.
     * </p>
     *
     * @return The {@link WATemplate.Status}.
     */
    public WATemplate.Status toWATemplateStatus() {
        return switch (this) {
            case IN_REVIEW -> WATemplate.Status.PENDING;
            case APPROVED -> WATemplate.Status.APPROVED;
            case REJECTED -> WATemplate.Status.REJECTED;
            case PAUSED -> WATemplate.Status.PAUSED;
            case DISABLED -> WATemplate.Status.DISABLED;
            case APPEAL_REQUESTED -> WATemplate.Status.IN_APPEAL;
        };
    }

    /**
     * <p>
     * Converts the {@link WATemplate.Status} to the equivalent {@link WACampaignTemplateStatus}.
     * </p>
     *
     * @param status Represents the {@link WATemplate.Status}.
     * @return The {@link WACampaignTemplateStatus}.
     */
    public static WACampaignTemplateStatus fromWATemplateStatus(final WATemplate.Status status) {
        return switch (status) {
            case PENDING -> IN_REVIEW;
            case APPROVED -> APPROVED;
            case REJECTED -> REJECTED;
            case PAUSED -> PAUSED;
            case DISABLED -> DISABLED;
            case IN_APPEAL -> APPEAL_REQUESTED;
            default -> throw ErrorCode.get(WAErrorCodes.INVALID_WA_CAMPAIGN_TEMPLATE_STATUS, "i18n.whatsapp.campaign.invalid.template.status");
        };
    }
}
