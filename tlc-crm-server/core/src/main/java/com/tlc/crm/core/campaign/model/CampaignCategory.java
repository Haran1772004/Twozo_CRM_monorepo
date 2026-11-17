package com.tlc.crm.core.campaign.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;

/**
 * <p>
 * Represents the campaign folder category.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum CampaignCategory {

    WHATSAPP((short) 1),
    EMAIL((short) 2),
    ;

    private final short id;

    CampaignCategory(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link CampaignCategory} by its value.
     * </p>
     *
     * @param id Represents the campaign category id.
     * @return The {@link CampaignCategory}.
     */
    public static CampaignCategory get(final short id) {
        return switch (id) {
            case 1 -> WHATSAPP;
            case 2 -> EMAIL;
            default -> throw ErrorCode.get(CampaignErrorCodes.INVALID_CAMPAIGN_CATEGORY, "i18n.campaign.invalid.category");
        };
    }
}