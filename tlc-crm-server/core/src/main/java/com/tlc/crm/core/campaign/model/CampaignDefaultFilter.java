package com.tlc.crm.core.campaign.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;

/**
 * <p>
 * Represents the default campaign filters.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum CampaignDefaultFilter {

    ALL_CAMPAIGNS((short) 1, "i18n.campaign.filter.all.campaigns"),
    ARCHIVED_CAMPAIGNS((short) 2, "i18n.campaign.filter.archived.campaigns"),
    ALL_FOLDERS((short) 3, "i18n.campaign.filter.all.folders");

    private final short id;
    private final String i18nKey;

    CampaignDefaultFilter(final short id, final String i18nKey) {
        this.id = id;
        this.i18nKey = i18nKey;
    }

    public Short getId() {
        return id;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * <p>
     * Gets the {@link CampaignDefaultFilter} by its id.
     * </p>
     *
     * @param id Represents the default filter id.
     * @return The {@link CampaignDefaultFilter}.
     */
    public static CampaignDefaultFilter get(final short id) {
        return switch (id) {
            case 1 -> ALL_CAMPAIGNS;
            case 2 -> ARCHIVED_CAMPAIGNS;
            case 3 -> ALL_FOLDERS;
            default -> throw ErrorCode.get(CampaignErrorCodes.INVALID_CAMPAIGN_DEFAULT_FILTER, "i18n.campaign.invalid.default.filter");
        };
    }
}