package com.tlc.crm.core.campaign.manager;

import com.tlc.crm.core.campaign.manager.impl.CampaignFilterPreferenceManagerImpl;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * Provides the {@link CampaignFilterUserPreference} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface CampaignFilterPreferenceManager {


    /**
     * <p>
     * Returns the table.
     * </p>
     *
     * @return Campaign filter preference table.
     */
    Table getTable();

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceManager}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceManager}.
     */
    static CampaignFilterPreferenceManager getInstance() {
        return CampaignFilterPreferenceManagerImpl.getInstance();
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
     * Updates the filter preference in DB.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void update(final CampaignFilterUserPreference preference);

    /**
     * <p>
     * Creates the filter preference in DB.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void create(final CampaignFilterUserPreference preference);
}