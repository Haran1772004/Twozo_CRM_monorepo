package com.tlc.crm.core.features.api.quota.manager;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.api.quota.manager.internal.QuotaValueOverrideManagerImpl;
import com.tlc.crm.core.features.api.quota.model.QuotaValueOverrideLog;
import com.tlc.crm.core.features.api.quota.model.QuotaValueOverrideNote;
import com.tlc.crm.core.features.api.quota.model.QuotaValueOverrideProgress;

import java.util.Collection;
import java.util.List;

/**
 * Manager interface responsible for handling operations related to forecast category override summaries for quota forecasts.
 *
 * @author kishore J
 * @version 1.0
 */
public interface QuotaValueOverrideManager {

    /**
     * <p>
     * Gets the {@link QuotaValueOverrideManager}
     * </p>
     *
     * @return The {@link QuotaValueOverrideManager}
     */
    static QuotaValueOverrideManager getInstance() {
        return QuotaValueOverrideManagerImpl.getInstance();
    }

    /**
     * <p>
     * Updates a quota forecast category override value
     * </p>
     *
     * @param quotaValueOverrideLog the override log containing override details
     */
    void update(final QuotaValueOverrideLog quotaValueOverrideLog);

    /**
     * <p>
     * Fetches the total number of recent override logs for the given summary.
     * </p>
     *
     * @param orgId     The organization ID
     * @param summaryId The override summary ID
     * @return The total count of recent override logs
     */
    long getLogCount(final long orgId, final long summaryId);

    /**
     * <p>
     * Fetches recent override logs for the given summary with pagination.
     * </p>
     *
     * @param orgId       The organization ID
     * @param summaryId   The override summary ID
     * @param limitConfig The pagination configuration containing start and limit
     * @return a collection of recent quota forecast category override logs
     */
    Collection<QuotaValueOverrideLog> getLogs(final long orgId, final long summaryId, final LimitConfig limitConfig);

    /**
     * <p>
     * Adds a note to a quota forecast category override summary.
     * </p>
     *
     * @param quotaForecastCategoryOverrideSummaryNote the note to be added
     */
    void addNote(final QuotaValueOverrideNote quotaForecastCategoryOverrideSummaryNote);

    /**
     * <p>
     * Gets the total number of notes associated with the given override summary.
     * </p>
     *
     * @param orgId     The organization ID
     * @param summaryId The override summary ID
     * @return the total number of notes
     */
    long getNoteCount(final long orgId, final long summaryId);

    /**
     * <p>
     * Gets notes associated with the given override summary with pagination.
     * </p>
     *
     * @param orgId       The organization ID
     * @param summaryId   The override summary ID
     * @param limitConfig The pagination configuration containing start and limit
     * @return a collection of notes related to the override summary
     */
    Collection<QuotaValueOverrideNote> getNotes(final long orgId, final long summaryId, final LimitConfig limitConfig);

    /**
     * <p>
     * Gets the forecast category override progress details for a quota.
     * </p>
     *
     * @param orgId   The organization ID
     * @param quotaId The quota ID
     * @param userId  The user ID (nullable for team quotas)
     * @return a collection containing forecast progress details
     */
    List<QuotaValueOverrideProgress> getProgress(final long orgId, final long quotaId, final Long userId);

    /**
     * <p>
     * Deletes the override summary and its associated data.
     * </p>
     *
     * @param orgId     The organization ID
     * @param summaryId The override summary ID
     */
    void delete(final long orgId, final long summaryId);
}