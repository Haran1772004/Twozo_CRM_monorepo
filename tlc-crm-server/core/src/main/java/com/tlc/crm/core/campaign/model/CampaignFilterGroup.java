package com.tlc.crm.core.campaign.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;

/**
 * <p>
 * Represents the campaign filter preference groups.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum CampaignFilterGroup {

    DEFAULT_FILTERS((short) 1, "i18n.campaign.filter.group.default.filters"),
    STARRED_FOLDERS((short) 2, "i18n.campaign.filter.group.starred.folders"),
    FOLDERS((short) 3, "i18n.campaign.filter.group.folders");

    private final short id;
    private final String i18nKey;

    CampaignFilterGroup(final short id, final String i18nKey) {
        this.id = id;
        this.i18nKey = i18nKey;
    }

    public int getId() {
        return id;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * <p>
     * Gets the {@link CampaignFilterGroup} by its id.
     * </p>
     *
     * @param id Represents the filter group id.
     * @return The {@link CampaignFilterGroup}.
     */
    public static CampaignFilterGroup get(final short id) {
        return switch (id) {
            case 1 -> DEFAULT_FILTERS;
            case 2 -> STARRED_FOLDERS;
            case 3 -> FOLDERS;
            default -> throw ErrorCode.get(CampaignErrorCodes.INVALID_CAMPAIGN_FILTER_GROUP, "i18n.campaign.invalid.filter.group");
        };
    }

    public boolean isDefaultFilterGroup() {
        return this == DEFAULT_FILTERS;
    }

    public boolean isStarredFolderGroup() {
        return this == STARRED_FOLDERS;
    }
}