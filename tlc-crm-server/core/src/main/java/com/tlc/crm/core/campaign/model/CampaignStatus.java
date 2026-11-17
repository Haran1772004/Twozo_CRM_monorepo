package com.tlc.crm.core.campaign.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;

/**
 * <p>
 * Represents the status of a {@link Campaign}.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum CampaignStatus {

    DRAFT((short) 1, "i18n.campaign.status.draft"),
    SCHEDULED((short) 2, "i18n.campaign.status.scheduled"),
    IN_PROGRESS((short) 3, "i18n.campaign.status.in.progress"),
    PAUSED((short) 4, "i18n.campaign.status.paused"),
    STOPPED((short) 5, "i18n.campaign.status.stopped"),
    SENT((short) 6, "i18n.campaign.status.sent"),
    FAILED((short) 7, "i18n.campaign.status.failed"),
    ;

    private final short id;
    private final String i18nKey;

    CampaignStatus(final short id, final String i18nKey) {
        this.id = id;
        this.i18nKey = i18nKey;
    }

    public short getId() {
        return id;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * <p>
     * Gets the {@link CampaignStatus} by its id.
     * </p>
     *
     * @param id Represents the status id.
     * @return The {@link CampaignStatus}.
     */
    public static CampaignStatus get(final short id) {
        return switch (id) {
            case 1 -> DRAFT;
            case 2 -> SCHEDULED;
            case 3 -> IN_PROGRESS;
            case 4 -> PAUSED;
            case 5 -> STOPPED;
            case 6 -> SENT;
            case 7 -> FAILED;
            default -> throw ErrorCode.get(CampaignErrorCodes.INVALID_CAMPAIGN_STATUS, "i18n.campaign.invalid.status");
        };
    }

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isScheduled() {
        return this == SCHEDULED;
    }

    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean isPaused() {
        return this == PAUSED;
    }

    public boolean isStopped() {
        return this == STOPPED;
    }

    public boolean isSent() {
        return this == SENT;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * Checks if the campaign status allows deletion.
     * Deletable statuses: DRAFT, SCHEDULED, PAUSED, FAILED
     */
    public boolean isDeletable() {
        return switch (this) {
            case DRAFT, SCHEDULED, PAUSED, FAILED -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks if the campaign is in progress or completed
     * </p>
     *
     * @return true if the campaign is in progress
     */
    public boolean isInProgressOrCompleted() {
        return switch (this) {
            case IN_PROGRESS, STOPPED, SENT, FAILED -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks if the campaign is in progress or completed
     * </p>
     *
     * @return true if the campaign is in progress
     */
    public boolean isStarted() {
        return switch (this) {
            case IN_PROGRESS, STOPPED, SENT -> true;
            default -> false;
        };
    }
}