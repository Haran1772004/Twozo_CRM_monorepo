package com.tlc.crm.notifications.api.call.manager;

import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.notifications.api.call.manager.impl.CallHistoryManagerImpl;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallHistory;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Optional;

/**
 * Manages the call history related DB actions.
 *
 * @author Sathish
 * @version 1.0
 */
public interface CallHistoryManager {

    /**
     * Gets the singleton instance of {@link CallHistoryManager}.
     *
     * @return The singleton instance of {@link CallHistoryManagerImpl}.
     */
    static CallHistoryManager getInstance() {
        return CallHistoryManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the call history table
     * </p>
     *
     * @return The call history table
     */
    Table getTable();

    /**
     * Saves new call details to the call history.
     *
     * @param callDetails The {@link CallDetails} object containing call-related information.
     */
    void save(final CallDetails callDetails);

    /**
     * Updates the details of an existing call.
     *
     * @param callDetails The CallDetails object containing updated call information.
     */
    void update(final CallDetails callDetails);

    /**
     * <p>
     * Updates the status of the {@link CallDetails} in the DB.
     * </p>
     *
     * @param externalCallId The unique identifier of the call details.
     * @param callStatus     Represents the status of the call.
     */
    void updateCallStatus(final String externalCallId, final CallStatus callStatus);

    /**
     * Retrieves specific call details based on the call service ID.
     *
     * @param externalCallId The unique identifier of the call details.
     * @return The {@link CallDetails} object containing call information, or null if not found.
     */
    Optional<CallDetails> getCallDetails(final String externalCallId);

    /**
     * Retrieves specific call details based on the organization ID and call details ID.
     *
     * @param orgId         The organization ID to which the call belongs.
     * @param callDetailsId The unique identifier of the call details.
     * @return The {@link CallDetails} object containing call information, or null if not found.
     */
    CallDetails getCallDetails(final long orgId, final Long callDetailsId);

    /**
     * Retrieves call history based on search criteria and pagination limits.
     *
     * @param orgId                 The organization ID for which the call history is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder Holds the {@link CriteriaDetails} defining filtering conditions.
     * @param limitConfig           The {@link LimitConfig} defining pagination limits.
     * @return The {@link CallHistory} object containing the list of call records.
     */
    CallHistory get(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder,
                    final LimitConfig limitConfig);

    /**
     * Retrieves counts of the {@link CallDetails} based on search criteria.
     *
     * @param orgId                 The organization ID for which the call history is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder Holds the {@link CriteriaDetails} defining filtering conditions.
     * @return The count of the {@link CallDetails}.
     */
    long getTotalCount(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder);

    /**
     * <p>
     * Gets the collection of completed {@link CallDetails} based on {@link CallStatus}
     * </p>
     *
     * @return The collection of {@link CallDetails}
     */
    Collection<CallDetails> getCompletedCallDetails();

    /**
     * <p>
     * Creates the note for the given call SID
     * </p>
     *
     * @param callSID Represents the call SID
     * @param note    Represents the {@link Note}
     */
    void createNote(final String callSID, final Note note);

    /**
     * <p>
     * Links the specified call history record to a newly created contact
     * </p>
     *
     * @param callHistoryId Represents the call history id
     * @param contact       Represents the {@link Contact}
     */
    void linkToNewContact(final long callHistoryId, final Contact contact);

    /**
     * <p>
     * Finds the IDs of call detail records associated with the given phone ID
     * that are not currently linked to any contact.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param phoneIds Represents the {@link Collection} of phone ids
     * @return {@link Collection} of unmapped call detail ids
     */
    Collection<Long> getUnmappedCallDetailsIds(final Long orgId, final Collection<Long> phoneIds);

    /**
     * <p>
     * Finds the IDs of call detail records associated with the given contact ID
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the {@link Collection} of phone ids
     * @return {@link Collection} of unmapped call detail ids
     */
    Collection<Long> getContactCallHistoryIds(final Long orgId, final Long contactId);

    /**
     * <p>
     *  Links the specified call detail records to the given contact.
     *  * For completed calls, the corresponding entity conversation and timeline
     *  * entries are also created.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param contact        Represents the {@link Contact} to be linked
     * @param callDetailsIds Represents the {@link Collection} of call detail ids to be linked to the contact
     */
    void linkCallDetailsToContact(final Long orgId, final Contact contact, final Collection<Long> callDetailsIds);

    /**
     * <p>
     *  Links the specified call detail records to the given deal.
     *  * For completed calls, the corresponding entity conversation and timeline
     *  * entries are also created.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param contact        Represents the {@link Contact} to be linked
     * @param deal           Represents the {@link Deal} to be linked
     */
    void linkCallDetailsToDeal(final Long orgId, final Contact contact, final Deal deal);

    /**
     * <p>
     * Checks whether a call history record exists for given call history id
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param callHistoryId Represents the call history id
     * @return true if the call history record exists; false otherwise
     */
    boolean exists(final long orgId, final long callHistoryId);

    /**
     * <p>
     * Checks whether the call of the given service id is loggable or not.
     * </p>
     *
     * @param orgId          Represents the organisation ID.
     * @param externalCallId Represents the external call ID.
     * @return A flag that indicates whether the call is loggable or not.
     */
    boolean isLoggable(final long orgId, final String externalCallId);

    /**
     * <p>
     * Fetches the recent calls of the given user.
     * </p>
     *
     * @param orgId       Represents the organisation ID.
     * @param userId      Represents the user ID.
     * @param limitConfig Holds the details of the pagination.
     * @return Collection of recent calls.
     */
    Collection<CallDetails> getRecentCalls(final Long orgId, final Long userId, final LimitConfig limitConfig);

    /**
     * Converts a row into a fully populated {@link CallDetails} object.
     *
     * @param callDetailsRow the primary row representing a call.
     * @param dataContainer  the container holding all joined rows.
     * @return A {@link CallDetails} object built from the provided data.
     */
    CallDetails convertRowIntoCallDetails(final Row callDetailsRow, final DataContainer dataContainer);

    /**
     * <p>
     * Removes a call record associated with the given organization.
     * </p>
     *
     * @param orgId Represents an organization ID.
     * @param id    Represents the call record id
     */
    void removeRecord(final long orgId, long id);

    /**
     * <p>
     * Fetches a collection of recent call contacts details for a given organization.
     * </p>
     *
     * @param orgId       The organization ID.
     * @param limitConfig The pagination configuration.
     * @return A collection of {@link Contact}.
     */
    Collection<Contact> getRecentCallContacts(final long orgId, final LimitConfig limitConfig);

    /**
     * <p>
     * Creates the note for the given call SID
     * </p>
     *
     * @param orgId         Represents the organisation ID.
     * @param callHistoryId Represents the call history id
     * @param note          Represents the {@link Note}
     */
    void createNote(final long orgId, final long callHistoryId, final Note note);

    /**
     * <p>
     * Gets the total count of notes associated with a specific call history record.
     * </p>
     *
     * @param orgId         Represents the organisation ID.
     * @param callHistoryId Represents the call history id.
     * @return The total count of notes.
     */
    long getNoteCount(final long orgId, final long callHistoryId);

    /**
     * <p>
     * Retrieves a paginated list of notes associated with a specific call history record.
     * </p>
     *
     * @param orgId         Represents the organisation ID.
     * @param callHistoryId Represents the call history id.
     * @param limitConfig   The {@link LimitConfig} defining pagination limits.
     * @return A collection of {@link Note} objects.
     */
    Collection<Note> getNotes(final long orgId, final long callHistoryId, final LimitConfig limitConfig);
}