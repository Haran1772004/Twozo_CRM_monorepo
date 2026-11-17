package com.tlc.crm.core.features.api.quota.manager;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.quota.manager.internal.QuotaListViewUserFilterManagerImpl;
import com.tlc.crm.core.features.api.quota.model.ForecastType;
import com.tlc.crm.core.features.api.quota.model.QuotaDateInterval;
import com.tlc.crm.core.features.api.quota.model.QuotaListViewUserFilter;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;

import java.util.Optional;

/**
 * Manages the retrieval of various quota list view filter settings for users.
 *
 * @author kishore J
 * @version 1.0
 */
public interface QuotaListViewUserFilterManager {

    /**
     * <p>
     * Gets the singleton instance of the QuotaListViewUserFilterManager.
     * </p>
     *
     * @return The {@link QuotaListViewUserFilterManager} instance.
     */
    static QuotaListViewUserFilterManager getInstance() {
        return QuotaListViewUserFilterManagerImpl.getInstance();
    }

    /**
     * <p>
     * Updates the recently applied filter criteria for the given user within the organization.
     * </p>
     *
     * @param orgId              The organization id
     * @param userId             The user id
     * @param filterCriteriaJson the {@link com.tlc.commons.json.JsonObject} containing the filter configuration to persist
     */
    void updateRecentlyAppliedFilter(final long orgId, final long userId, final JsonObject filterCriteriaJson);

    /**
     * <p>
     * Gets the list view filter configuration for a given organization and user.
     * </p>
     *
     * @param orgId  The organization id
     * @param userId The user id
     * @return An {@link Optional} containing the {@link QuotaListViewUserFilter} if found.
     */
    Optional<QuotaListViewUserFilter> get(final long orgId, final long userId);

    /**
     * <p>
     * Gets the requested quota date interval for a given organization and user.
     * </p>
     *
     * @param orgId  The organization id
     * @param userId The user id
     * @return An {@link Optional} containing the {@link QuotaDateInterval} if found.
     */
    Optional<QuotaDateInterval> getQuotaDateInterval(final long orgId, final long userId);

    /**
     * <p>
     * Gets the quota metric configuration mapping for a given organization and user.
     * </p>
     *
     * @param orgId  The organization id
     * @param userId The user id
     * @return An {@link Optional} containing the {@link QuotaMetric} if found.
     */
    Optional<QuotaMetric> getQuotaMetric(final long orgId, final long userId);

    /**
     * <p>
     * Gets the selected forecast type mapping for a given organization and user.
     * </p>
     *
     * @param orgId  The organization id
     * @param userId The user id
     * @return An {@link Optional} containing the {@link ForecastType} if found.
     */
    Optional<ForecastType> getForecastType(final long orgId, final long userId);
}
