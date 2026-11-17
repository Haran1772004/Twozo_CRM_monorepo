package com.tlc.crm.core.campaign.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;

/**
 * <p>
 * Represents the dispatch status of a recipient.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum CampaignRecipientStatus {

    NEW((short) 1),
    IN_PROGRESS((short) 2),
    SENT((short) 3),
    FAILED((short) 4),
    DROPPED((short) 5),
    RETRY_PENDING((short) 6),
    ;

    private final short id;

    CampaignRecipientStatus(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link CampaignRecipientStatus} by its id.
     * </p>
     *
     * @param id Represents the status id.
     * @return The {@link CampaignRecipientStatus}.
     */
    public static CampaignRecipientStatus get(final short id) {
        return switch (id) {
            case 1 -> NEW;
            case 2 -> IN_PROGRESS;
            case 3 -> SENT;
            case 4 -> FAILED;
            case 5 -> DROPPED;
            case 6 -> RETRY_PENDING;
            default -> throw ErrorCode.get(CampaignErrorCodes.INVALID_CAMPAIGN_RECIPIENT_STATUS, "i18n.campaign.invalid.recipient.status");
        };
    }

    public boolean isNew() {
        return this == NEW;
    }

    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean isSent() {
        return this == SENT;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isDropped() {
        return this == DROPPED;
    }

    public boolean isRetryPending() {
        return this == RETRY_PENDING;
    }

    public boolean isTerminal() {
        return this == SENT || this == FAILED || this == DROPPED;
    }
}