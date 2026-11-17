package com.tlc.crm.core.campaign.resolver;

import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.resolver.impl.CampaignFilterPreferenceResolverImpl;

/**
 * <p>
 * Resolves {@link CampaignFilterUserPreference} from cache and DB.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface CampaignFilterPreferenceResolver {

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceResolver}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceResolver}.
     */
    static CampaignFilterPreferenceResolver getInstance() {
        return CampaignFilterPreferenceResolverImpl.getInstance();
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
     * Updates the filter preference in DB and cache.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void update(final CampaignFilterUserPreference preference);
}