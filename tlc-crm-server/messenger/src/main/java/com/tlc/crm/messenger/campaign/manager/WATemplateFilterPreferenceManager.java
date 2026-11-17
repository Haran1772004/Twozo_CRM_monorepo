package com.tlc.crm.messenger.campaign.manager;

import com.tlc.crm.messenger.campaign.manager.impl.WATemplateFilterPreferenceManagerImpl;
import com.tlc.crm.messenger.campaign.model.WATemplateFilterPreference;

/**
 * <p>
 * Provides the {@link WATemplateFilterPreference} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface WATemplateFilterPreferenceManager {

    /**
     * <p>
     * Gets the instance of {@link WATemplateFilterPreferenceManager}.
     * </p>
     *
     * @return {@link WATemplateFilterPreferenceManager}.
     */
    static WATemplateFilterPreferenceManager getInstance() {
        return WATemplateFilterPreferenceManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the filter preference for the given user.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @return The {@link WATemplateFilterPreference}, or {@code null} when not found.
     */
    WATemplateFilterPreference get(final Long orgId, final Long userId);

    /**
     * <p>
     * Creates the filter preference.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void create(final WATemplateFilterPreference preference);

    /**
     * <p>
     * Updates the filter preference.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void update(final WATemplateFilterPreference preference);
}