package com.tlc.crm.fields.manager;

import com.tlc.crm.fields.models.SummaryFieldPreference;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * Interface for managing Summary Field User Preference operations.
 * Provides methods for retrieving and updating user preferences for summary field display configurations.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface SummaryFieldPreferenceManager {

    /**
     * Gets the singleton instance of SummaryFieldPreferenceManager
     *
     * @return the {@link SummaryFieldPreferenceManager} instance
     */
    static SummaryFieldPreferenceManager getInstance() {
        return SummaryFieldPreferenceManagerImpl.getInstance();
    }

    /**
     * Gets the summary field user preference table
     *
     * @return the {@link Table} for summary field user preference
     */
    Table getTable();

    /**
     * Retrieves summary field preference for a specific user and source
     *
     * @param orgId   the organization ID
     * @param userId  the user ID
     * @param sourceId the source ID
     * @return the {@link SummaryFieldPreference} if found
     */
    SummaryFieldPreference get(final Long orgId, final Long userId, final int sourceId);

    /**
     * Updates an existing summary field preference or creates a new one if it doesn't exist
     *
     * @param orgId      the organization ID
     * @param preference the {@link SummaryFieldPreference} to update or create
     */
    void update(final Long orgId, final SummaryFieldPreference preference);
}