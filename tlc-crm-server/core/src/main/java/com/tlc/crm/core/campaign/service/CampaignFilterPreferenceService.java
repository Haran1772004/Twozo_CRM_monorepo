package com.tlc.crm.core.campaign.service;

import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.service.impl.CampaignFilterPreferenceServiceImpl;

/**
 * <p>
 * Provides an abstract way to deal with {@link CampaignFilterUserPreference} definitions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface CampaignFilterPreferenceService {

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceService}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceService}.
     */
    static CampaignFilterPreferenceService getInstance() {
        return CampaignFilterPreferenceServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the filter preference of the given user for the given category.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param category Represents the campaign category.
     * @return {@link CampaignFilterUserPreference}.
     */
    CampaignFilterUserPreference get(final Long orgId, final Long userId, final CampaignCategory category);

    /**
     * <p>
     * Creates or updates the user's campaign filter preference based on the
     * existing preference for the given category.
     * </p>
     *
     * @param selectedFilterPreference Represents the selected filter preference.
     */
    void upsert(final CampaignFilterUserPreference selectedFilterPreference);
}