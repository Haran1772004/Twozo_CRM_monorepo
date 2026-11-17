package com.tlc.crm.core.features.action.call.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.action.call.manager.impl.CallLogManagerImpl;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.features.action.call.model.Outcome;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;

import java.util.Collection;

/**
 * Manages the call log related DB actions.
 *
 * @author Sathish
 * @version 1.0
 */
public interface CallLogManager extends ConfigManager<CallLog> {

    static CallLogManager getInstance() {
        return CallLogManagerImpl.getInstance();
    }

    /**
     * Retrieves call log based on search criteria and pagination limits.
     *
     * @param orgId                 The organization ID for which the call log is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder Holds the {@link CriteriaDetails} defining filtering conditions.
     * @param limitConfig           The {@link LimitConfig} defining pagination limits.
     * @return The {@link CallLog} containing the list of call records.
     */
    Collection<CallLog> get(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder,
                            final LimitConfig limitConfig);

    /**
     * Retrieves counts of the {@link CallLog} based on search criteria.
     *
     * @param orgId                 The organization ID for which the call log is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder Holds the {@link CriteriaDetails} defining filtering conditions.
     * @return The count of the {@link CallLog}.
     */
    long getTotalCount(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder);

    /**
     * Retrieves a collection of {@link Outcome} options for the given organization,
     * based on the provided filtering criteria and result limits.
     *
     * @param criteria the filtering criteria to apply (e.g., search term, conditions)
     * @param limit    the maximum number of results to return and paging info
     * @return a collection of Outcome entities matching the criteria
     */
    Collection<Outcome> getOutcomes(final DropdownCriteria criteria, final DropdownLimit limit);

    /**
     * Returns the total number of {@link Outcome} records that match the specified criteria.
     *
     * @param criteria filtering conditions to apply (e.g., search terms, constraints)
     * @return the total count of matching outcomes
     */
    long getTotalOutcomesCount(final DropdownCriteria criteria);

    /**
     * <p>
     * Updates the start time of the {@link CallLog} by using call history ID.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param id        Represents the call log ID.
     * @param startTime Represents the start time of the call.
     */
    void updateStartTime(final long orgId, final Long id, final DateTime startTime);

    /**
     * Converts a {@link Row} from the call log table into a {@link CallLog}.
     *
     * @param callLogRow    the row from the call log table
     * @param dataContainer the full container holding all joined tables and related data
     * @return a fully populated {@link CallLog}
     */
    CallLog convertRowToCallLog(final Row callLogRow, final DataContainer dataContainer);
}
