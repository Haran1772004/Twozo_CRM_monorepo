package com.tlc.crm.core.campaign.cache;

import com.tlc.crm.core.campaign.cache.impl.CampaignFilterPreferenceCacheImpl;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignCategory;

/**
 * <p>
 * Caches {@link CampaignFilterUserPreference} for users.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface CampaignFilterPreferenceCache {

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceCache}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceCache}.
     */
    static CampaignFilterPreferenceCache getInstance() {
        return CampaignFilterPreferenceCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the cached filter preference of the given user for the given category.
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
     * Caches the given filter preference.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void put(final CampaignFilterUserPreference preference);
}