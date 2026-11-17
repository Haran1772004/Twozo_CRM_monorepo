package com.tlc.crm.notifications.api.call.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallForwardDetails;
import com.tlc.crm.notifications.api.call.model.CallHistory;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.fields.CallHistoryFields;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.crm.notifications.sql.resource.MNCCALLFORWARDHISTORY;
import com.tlc.crm.notifications.sql.resource.MNCCALLNOTE;
import com.tlc.crm.notifications.sql.resource.MNCCALLPARTICIPANT;
import com.tlc.crm.notifications.sql.resource.MNCCALLRECORDDETAILS;
import com.tlc.crm.notifications.sql.resource.MNCDEALCALL;
import com.tlc.crm.notifications.sql.resource.OMECCALLCONVERSATIONS;
import com.tlc.crm.notifications.sql.resource.OMNCALLHISTORY;
import com.tlc.crm.notifications.sql.resource.OMNCCALLSUMMARY;
import com.tlc.crm.notifications.sql.resource.OMNCPHONENUMBER;
import com.tlc.crm.notifications.sql.resource.OMTLCALLTIMELINE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.number.PhoneNumberUtil;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the call history related DB actions.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallHistoryManagerImpl implements CallHistoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallHistoryManagerImpl.class);
    private static final String PLUS = "+";

    private final MonetaryManager monetaryManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final PipelineStageManager pipelineStageManager;
    private final NoteManager noteManager;
    private final PhoneManager phoneManager;
    private final PhoneNumberManager phoneNumberManager;
    private final UserService userService;
    private final FieldService fieldService;
    private final AdminDataStore adminDataStore;
    private final CommonTimelineManager commonTimelineManager;

    private CallHistoryManagerImpl() {
        monetaryManager = MonetaryManager.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        dealManager = DealManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        noteManager = NoteManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        phoneNumberManager = PhoneNumberManager.getInstance();
        userService = UserService.getInstance();
        fieldService = FieldService.getInstance();
        adminDataStore = DataStoreProvider.getAdminDataStore();
        commonTimelineManager = CommonTimelineManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final CallHistoryManagerImpl CALL_HISTORY_MANAGER = new CallHistoryManagerImpl();
    }

    public static CallHistoryManagerImpl getInstance() {
        return InstanceHolder.CALL_HISTORY_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @return The call history table
     */
    @Override
    public Table getTable() {
        return Table.get(OMNCALLHISTORY.TABLE);
    }

    private Table getSummaryTable() {
        return Table.get(OMNCCALLSUMMARY.TABLE);
    }

    private Table getRecordingsTable() {
        return Table.get(MNCCALLRECORDDETAILS.TABLE);
    }

    private Table getForwardHistoryTable() {
        return Table.get(MNCCALLFORWARDHISTORY.TABLE);
    }

    private Table getCallNoteTable() {
        return Table.get(MNCCALLNOTE.TABLE);
    }

    private Table getCallParticipantTable() {
        return Table.get(MNCCALLPARTICIPANT.TABLE);
    }

    private Table getDealCallTable() {
        return Table.get(MNCDEALCALL.TABLE);
    }

    /**
     * <p>
     * Gets the contact alias table
     * </p>
     *
     * @return the contact alias table
     */
    private Table getContactAliasTable() {
        return Table.get(contactManager.getTable().getName(), NotificationFields.CONTACT.getName());
    }

    /**
     * <p>
     * Gets the company alias table
     * </p>
     *
     * @return the company alias table
     */
    private Table getCompanyAliasTable() {
        return Table.get(companyManager.getTable().getName(), NotificationFields.COMPANY.getName());
    }

    /**
     * <p>
     * Gets the call-time company alias table
     * </p>
     *
     * @return the call-time company alias table
     */
    private Table getConversationCompanyAliasTable() {
        return Table.get(companyManager.getTable().getName(), NotificationFields.CALL_COMPANY.getName());
    }

    /**
     * <p>
     * Gets the call conversation table
     * </p>
     *
     * @return The call conversation table
     */
    private Table getCallConversationTable() {
        return Table.get(OMECCALLCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the call timeline table
     * </p>
     *
     * @return The call timeline table
     */
    private Table getCallTimelineTable() {
        return Table.get(OMTLCALLTIMELINE.TABLE);
    }

    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    private OrgDataStore secureOrgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param callDetails The {@link CallDetails} object containing call-related information.
     */
    @Override
    public void save(final CallDetails callDetails) {
        final DataContainer dataContainer = DataContainer.create();

        LOGGER.info("Call History [TRACE]: save() - ExternalCallId: {}, OrgId: {}, Status: {}", callDetails.getExternalId(), callDetails.orgId(), callDetails.getStatus());
        loadCallDetailsIntoContainer(true, callDetails, null, dataContainer);
        orgDataStore(callDetails.orgId()).commitChanges(dataContainer);
        LOGGER.info("Call History [TRACE]: save() commit SUCCESS - ExternalCallId: {}, CallDetailsId: {}", callDetails.getExternalId(), callDetails.id());
    }

    /**
     * {@inheritDoc}
     *
     * @param callDetails The CallDetails object containing updated call information.
     */
    @Override
    public void update(final CallDetails callDetails) {
        final OrgDataStore orgDataStore = orgDataStore(callDetails.orgId());
        final Row callDetailsRow = orgDataStore.get(getTable(), callDetails.id());
        final DataContainer targetContainer = DataContainer.create();

        LOGGER.info("Call History [TRACE]: update() - CallDetailsId: {}, ExternalCallId: {}, OrgId: {}, Status: {}", callDetails.id(), callDetails.getExternalId(), callDetails.orgId(), callDetails.getStatus());
        loadCallDetailsIntoContainer(false, callDetails, callDetailsRow, targetContainer);
        orgDataStore.commitChanges(targetContainer);
        LOGGER.info("Call History [TRACE]: update() commit SUCCESS - CallDetailsId: {}", callDetails.id());
    }

    /**
     * <p>
     * Updates the status of the {@link CallDetails} in the DB.
     * </p>
     *
     * @param externalCallId The unique identifier of the call details.
     * @param callStatus     Represents the status of the call.
     */
    @Override
    public void updateCallStatus(final String externalCallId, final CallStatus callStatus) {
        final Table callDetailsTable = getTable();
        final Table callParticipantTable = getCallParticipantTable();
        final DataContainer targetContainer = DataContainer.create();
        final SelectQuery selectQuery = SelectQuery.get(callDetailsTable);
        final Column callDetailsPKColumn = callDetailsTable.getPKColumn();

        selectQuery.addSelectClause(callParticipantTable, callDetailsTable);
        selectQuery.addJoinClause(new JoinClause(callDetailsPKColumn, callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID),
                JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(callDetailsTable.getColumn(OMNCALLHISTORY.EXTERNAL_CALL_ID), externalCallId)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Row callHistoryRow = dataContainer.getRow(callDetailsTable);

        if (Objects.nonNull(callHistoryRow)) {
            final CallDetails callDetails = new CallDetails();
            final Long callHistoryId = callHistoryRow.getPKValue();
            final Row callParticipantRow = dataContainer.getRow(callParticipantTable,
                    new WhereClause(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID), callHistoryId)));
            final Long orgId = callHistoryRow.getOrgId();

            if (Objects.nonNull(callParticipantRow)) {
                final Contact contact = new Contact();

                callDetails.setOrgId(orgId);
                callDetails.setId(callHistoryId);
                callDetails.setStatus(callStatus);
                callDetails.setContact(contact);
                callDetails.setType(CallType.get((short) callHistoryRow.get(OMNCALLHISTORY.CALL_TYPE)));
                callDetails.setParticipantUser(userService.getUser(orgId, callHistoryRow.get(OMNCALLHISTORY.USER_ID)));

                contact.setId(callParticipantRow.get(MNCCALLPARTICIPANT.CONTACT_ID));
                loadCallHistoryIntoEntityConversations(callDetails, targetContainer);
                loadCallDetailsToTimeline(callDetails, targetContainer);
            }
            callHistoryRow.set(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS), callStatus.getId());
            targetContainer.updateRow(callHistoryRow);
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param externalCallId The unique identifier of the call details.
     * @return The {@link CallDetails} object containing call information, or null if not found.
     */
    @Override
    public Optional<CallDetails> getCallDetails(final String externalCallId) {
        final Table callDetailsTable = getTable();
        final Table dealCallTable = getDealCallTable();
        final Table userNumberTable = phoneNumberManager.getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(callDetailsTable.getColumn(OMNCALLHISTORY.EXTERNAL_CALL_ID), externalCallId));
        final SelectQuery selectQuery = buildCallParticipantsSelectQuery(whereClause);

        selectQuery.addSelectClause(dealCallTable);
        selectQuery.addJoinClause(new JoinClause(callDetailsTable.getPKColumn(), dealCallTable.getColumn(MNCDEALCALL.CALL_HISTORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(callDetailsTable, userNumberTable);
        selectQuery.addJoinClause(new JoinClause(callDetailsTable.getColumn(OMNCALLHISTORY.NUMBER_ID), userNumberTable.getPKColumn()));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> callDetailsRows = dataContainer.getRows(callDetailsTable);

        return callDetailsRows.findFirst().map(callDetailsRow -> {
            final CallDetails callDetails = convertRowIntoCallDetails(callDetailsRow, dataContainer);
            final Row dealCallRow = dataContainer.getRow(dealCallTable);

            if (Objects.nonNull(dealCallRow)) {
                final Deal deal = Objects.requireNonNullElse(callDetails.getDeal(), new Deal());

                deal.setId(dealCallRow.get(MNCDEALCALL.DEAL_ID));
                callDetails.setDeal(deal);
            }
            return callDetails;
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         The organization ID to which the call belongs.
     * @param callDetailsId The unique identifier of the call details.
     * @return The {@link CallDetails} object containing call information, or null if not found.
     */
    @Override
    public CallDetails getCallDetails(final long orgId, final Long callDetailsId) {
        final Table callDetailsTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(callDetailsTable.getPKColumn(), callDetailsId));
        final DataContainer dataContainer = fetchCallDetailsContainer(orgId, getParentCallCriteria(false).and(whereClause));
        final Row callDetailsRow = dataContainer.getRow(callDetailsTable);

        return convertRowIntoCallDetails(callDetailsRow, dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 The organization ID for which the call history is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder Holds the {@link CriteriaDetails} defining filtering conditions.
     * @param limitConfig           The {@link LimitConfig} defining pagination limits.
     * @return The {@link CallHistory} object containing the list of call records.
     */
    @Override
    public CallHistory get(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder,
                           final LimitConfig limitConfig) {
        final Table callDetailsTable = getTable();
        final Column pkColumn = callDetailsTable.getPKColumn();
        final Optional<WhereClause> optionalSearchCriteria = buildSearchCriteria(searchText);
        final Optional<WhereClause> filterCriteria = criteriaDetailsHolder.getCriteria(this::getColumn);
        final Optional<WhereClause> optionalWhereClause = filterCriteria.map(criteria ->
                optionalSearchCriteria.map(criteria::and).orElse(criteria)).or(() -> optionalSearchCriteria);
        final WhereClause parentCallCriteria = getParentCallCriteria(false);
        final SelectQuery innerQuery = SelectQuery.get(callDetailsTable);

        if (Objects.nonNull(searchText)) {
            loadSearchCriteriaJoins(innerQuery);
        }
        innerQuery.addSelectClause(pkColumn);
        innerQuery.setWhereClause(optionalWhereClause.map(parentCallCriteria::and).orElseGet(() -> parentCallCriteria).and(Criteria.in(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS),
                CallStatus.getCompletedStatuses())));
        innerQuery.addOrderByClause(new OrderByClause(callDetailsTable.getColumn(OMNCALLHISTORY.LOGGED_TIME), OrderByClause.OrderType.DESCENDING));
        innerQuery.setLimitClause(new LimitClause(limitConfig.start(), limitConfig.limit()));
        final DataContainer dataContainer = fetchCallDetailsContainer(orgId, parentCallCriteria.and(Criteria.in(pkColumn, innerQuery)));
        final Stream<Row> callDetailsRows = dataContainer.getRows(callDetailsTable);
        final CallHistory callHistory = new CallHistory();

        callDetailsRows.forEach(callDetailsRow -> callHistory.addCallDetails(convertRowIntoCallDetails(callDetailsRow, dataContainer)));
        return callHistory;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 The organization ID for which the call history is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder Holds the {@link CriteriaDetails} defining filtering conditions.
     * @return The count of the {@link CallDetails}.
     */
    @Override
    public long getTotalCount(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder) {
        final Table callDetailsTable = getTable();
        final Optional<WhereClause> optionalSearchCriteria = buildSearchCriteria(searchText);
        final Optional<WhereClause> filterCriteria = criteriaDetailsHolder.getCriteria(this::getColumn);
        final Optional<WhereClause> optionalWhereClause = filterCriteria.map(criteria ->
                optionalSearchCriteria.map(criteria::and).orElse(criteria)).or(() -> optionalSearchCriteria);
        final WhereClause parentCallCriteria = getParentCallCriteria(false);
        final CountQuery countQuery = CountQuery.get(callDetailsTable);

        if (Objects.nonNull(searchText)) {
            loadSearchCriteriaJoins(countQuery);
        }
        countQuery.setWhereClause(optionalWhereClause.map(parentCallCriteria::and).orElseGet(() -> parentCallCriteria));
        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link CallDetails}
     */
    @Override
    public Collection<CallDetails> getCompletedCallDetails() {
        final Table callDetailsTable = getTable();
        final Table summaryTable = getSummaryTable();
        final Table userNumberTable = phoneNumberManager.getTable();
        final Column callHistoryIdColumn = summaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID);
        final WhereClause whereClause = new WhereClause(Criteria.in(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS),
                Set.of(CallStatus.COMPLETED.getId(), CallStatus.CANCELED.getId(), CallStatus.NO_ANSWER.getId(),
                        CallStatus.BUSY.getId(), CallStatus.FAILED.getId())))
                .and(new WhereClause(Criteria.eq(callHistoryIdColumn, null))).and(Criteria.eq(userNumberTable.getColumn(OMNCPHONENUMBER.PROVIDER),
                        TelephoneProvider.getDefaultProvider().getId())).and(Criteria.eq(userNumberTable.getColumn(OMNCPHONENUMBER.IS_ACTIVE), true));
        final SelectQuery selectQuery = buildCallParticipantsSelectQuery(whereClause);

        selectQuery.addSelectClause(callDetailsTable, userNumberTable);
        selectQuery.addJoinClause(List.of(new JoinClause(callDetailsTable.getPKColumn(), callHistoryIdColumn, JoinClause.JoinType.LEFT),
                new JoinClause(callDetailsTable.getColumn(OMNCALLHISTORY.NUMBER_ID), userNumberTable.getPKColumn())));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return dataContainer.getRows(callDetailsTable).map(callDetailsRow -> convertRowIntoCallDetails(callDetailsRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param callSID Represents the call SID
     * @param note    Represents the {@link Note}
     */
    @Override
    public void createNote(final String callSID, final Note note) {
        final Table callDetailsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(callDetailsTable);
        final Long orgId = note.orgId();

        selectQuery.addSelectClause(callDetailsTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(callDetailsTable.getColumn(OMNCALLHISTORY.EXTERNAL_CALL_ID), callSID)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row callDetailsRow = dataContainer.getRow(callDetailsTable);

        if (Objects.nonNull(callDetailsRow)) {
            final DataContainer targetContainer = DataContainer.create();
            final Table callNoteTable = getCallNoteTable();

            noteManager.loadRowIntoContainer(true, note, null, targetContainer);
            final Row callNoteRow = new Row(callNoteTable);

            callNoteRow.set(MNCCALLNOTE.CALL_HISTORY_ID, callDetailsRow.getPKValue());
            callNoteRow.set(MNCCALLNOTE.NOTE_ID, note.id());

            targetContainer.addNewRow(callNoteRow);
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param callHistoryId Represents the call history id
     * @param contact       Represents the {@link Contact}
     */
    @Override
    public void linkToNewContact(final long callHistoryId, final Contact contact) {
        final Long orgId = contact.orgId();
        final Table callParticipantTable = getCallParticipantTable();
        final Table callDetailsTable = getTable();
        final Table callSummaryTable = getSummaryTable();
        final Column callHistoryIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID);
        final Column callHistoryPKColumn = callDetailsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(callParticipantTable);

        selectQuery.addSelectClause(callParticipantTable);
        selectQuery.addSelectClause(List.of(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS),
                callDetailsTable.getColumn(OMNCALLHISTORY.CALL_TYPE), callDetailsTable.getColumn(OMNCALLHISTORY.USER_ID), callHistoryPKColumn));
        selectQuery.addSelectClause(callSummaryTable);
        selectQuery.addJoinClause(List.of(new JoinClause(callHistoryIdColumn, callHistoryPKColumn),
                new JoinClause(callHistoryIdColumn, callSummaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID), JoinClause.JoinType.LEFT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(callHistoryIdColumn, callHistoryId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row callParticipantRow = dataContainer.getRow(callParticipantTable);

        if (Objects.nonNull(callParticipantRow)) {
            final DataContainer targetContainer = DataContainer.create();

            contactManager.loadContactIntoContainer(true, contact, targetContainer);
            callParticipantRow.set(MNCCALLPARTICIPANT.CONTACT_ID, contact.id());
            targetContainer.updateRow(callParticipantRow);

            final Row callDetailsRow = dataContainer.getRow(callDetailsTable, new WhereClause(Criteria.eq(callHistoryPKColumn, callHistoryId)));

            if (Objects.nonNull(callDetailsRow)) {
                final CallStatus callStatus = CallStatus.get((short) callDetailsRow.get(OMNCALLHISTORY.CALL_STATUS));
                final Long userId = callDetailsRow.get(OMNCALLHISTORY.USER_ID);

                if (callStatus.isCompletedStatus() && Objects.nonNull(userId)) {
                    final CallDetails callDetails = new CallDetails();
                    final Row callSummaryRow = dataContainer.getRow(callSummaryTable,
                            new WhereClause(Criteria.eq(callSummaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID), callHistoryId)));

                    callDetails.setId(callHistoryId);
                    callDetails.setOrgId(orgId);
                    callDetails.setStatus(callStatus);
                    callDetails.setType(CallType.get((short) callDetailsRow.get(OMNCALLHISTORY.CALL_TYPE)));
                    callDetails.setContact(contact);
                    callDetails.setParticipantUser(userService.getUser(orgId, userId));

                    if (Objects.nonNull(callSummaryRow)) {
                        final CallSummary callSummary = new CallSummary();

                        callSummary.setOrgId(orgId);
                        callSummary.setStartTime(callSummaryRow.get(OMNCCALLSUMMARY.START_TIME));
                        callSummary.setEndTime(callSummaryRow.get(OMNCCALLSUMMARY.END_TIME));
                        callDetails.setSummary(callSummary);
                    }
                    loadCallHistoryIntoEntityConversations(callDetails, targetContainer);
                }
            }
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param phoneIds Represents the phone ids
     * @return {@link Collection} of call detail ids that are not yet linked to any contact
     */
    @Override
    public Collection<Long> getUnmappedCallDetailsIds(final Long orgId, final Collection<Long> phoneIds) {
        if (Objects.isNull(orgId) || Objects.isNull(phoneIds) || phoneIds.isEmpty()) {
            return Collections.emptyList();
        }

        final Table callParticipantTable = getCallParticipantTable();
        final Column callHistoryIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID);
        final SelectQuery selectQuery = SelectQuery.get(callParticipantTable);

        selectQuery.addSelectClause(callHistoryIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(callParticipantTable.getColumn(MNCCALLPARTICIPANT.PHONE_ID),
                getPhoneIdsWithNumberVariants(orgId, phoneIds)))
                .and(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), null)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(callParticipantTable).map(row -> (Long) row.get(callHistoryIdColumn))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Resolves the given phone IDs and IDs of phone records containing the same numbers in different formats,
     * ensuring earlier call history records linked to those phone records are also associated with the contact.
     *
     * @param orgId    The organization ID
     * @param phoneIds The collection of phone IDs
     * @return The given phone IDs and IDs of matching phone records stored in different formats
     */
    private Collection<Long> getPhoneIdsWithNumberVariants(final Long orgId, final Collection<Long> phoneIds) {
        final Collection<Long> resolvedPhoneIds = new HashSet<>(phoneIds);
        final Table phoneTable = phoneManager.getTable();
        final Column phonePKColumn = phoneTable.getPKColumn();

        for (final Phone phone : phoneManager.get(orgId, phoneIds)) {
            final String number = phone.getNumber();

            if (Objects.isNull(number) || number.isBlank()) {
                continue;
            }
            String nationalNumber = number;
            Country country = phone.getCountry();
            String countryCode = Objects.isNull(country) ? null : country.getCode();

            if (number.startsWith(PLUS)) {
                try {
                    final com.tlc.l10n.model.number.PhoneNumber parsedNumber = PhoneNumberUtil.parse(number);

                    nationalNumber = parsedNumber.getNumber();

                    final Country parsedCountry = parsedNumber.getCountry();
                    final String parsedCountryCode = parsedNumber.getCountryCode();

                    country = Objects.isNull(parsedCountry) ? country : parsedCountry;
                    countryCode = Objects.isNull(parsedCountryCode) ? countryCode : PLUS.concat(parsedCountryCode);
                } catch (final Exception exception) {
                    LOGGER.debug("Unable to parse the phone number while resolving call history, Org ID : {}, Phone ID : {}", orgId, phone.id());
                    continue;
                }
            }

            if (Objects.isNull(countryCode)) {
                continue;
            }
            final SelectQuery phoneQuery = SelectQuery.get(phoneTable);
            final String prefixedNumber = countryCode.concat(nationalNumber);

            phoneQuery.addSelectClause(phonePKColumn);
            WhereClause phoneCriteria = new WhereClause(Criteria.eq(phoneTable.getColumn(OMETPHONE.NUMBER), prefixedNumber));

            if (Objects.nonNull(country)) {
                phoneCriteria = phoneCriteria.or(new WhereClause(Criteria.eq(phoneTable.getColumn(OMETPHONE.NUMBER), nationalNumber))
                        .and(Criteria.eq(phoneTable.getColumn(OMETPHONE.COUNTRY_ID), country.getId())));
            }
            phoneQuery.setWhereClause(phoneCriteria);
            orgDataStore(orgId).get(phoneQuery).getRows(phoneTable).map(Row::getPKValue).filter(Objects::nonNull).forEach(resolvedPhoneIds::add);
        }
        return resolvedPhoneIds;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @return {@link Collection} of call detail ids that are not yet linked to any contact
     */
    @Override
    public Collection<Long> getContactCallHistoryIds(final Long orgId, final Long contactId) {
        if (Objects.isNull(orgId)) {
            return Collections.emptyList();
        }

        final Table callParticipantTable = getCallParticipantTable();
        final Column callHistoryIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID);
        final SelectQuery selectQuery = SelectQuery.get(callParticipantTable);

        selectQuery.addSelectClause(callHistoryIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), contactId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(callParticipantTable).map(row -> (Long) row.get(callHistoryIdColumn))
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param contact        Represents the {@link Contact} to be linked
     * @param callDetailsIds Represents the {@link Collection} of call detail ids to be linked to the contact
     */
    @Override
    public void linkCallDetailsToContact(final Long orgId, final Contact contact, final Collection<Long> callDetailsIds) {
        if (Objects.isNull(orgId) || Objects.isNull(contact) || Objects.isNull(contact.id())
                || Objects.isNull(callDetailsIds) || callDetailsIds.isEmpty()) {
            return;
        }

        final Long contactId = contact.id();
        final Table callDetailsTable = getTable();
        final Table callParticipantTable = getCallParticipantTable();
        final Table callSummaryTable = getSummaryTable();
        final Column callHistoryPKColumn = callDetailsTable.getPKColumn();
        final Column callHistoryIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID);
        final SelectQuery selectQuery = SelectQuery.get(callParticipantTable);

        selectQuery.addSelectClause(callParticipantTable);
        selectQuery.addSelectClause(List.of(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS),
                callDetailsTable.getColumn(OMNCALLHISTORY.CALL_TYPE), callDetailsTable.getColumn(OMNCALLHISTORY.USER_ID), callHistoryPKColumn));
        selectQuery.addSelectClause(callSummaryTable);
        selectQuery.addJoinClause(List.of(new JoinClause(callHistoryIdColumn, callHistoryPKColumn),
                new JoinClause(callHistoryIdColumn, callSummaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(callHistoryIdColumn, callDetailsIds))
                .and(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), null))
                .or(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), contactId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Row> callParticipantRows = dataContainer.getRows(callParticipantTable).toList();

        if (callParticipantRows.isEmpty()) {
            return;
        }

        final DataContainer targetContainer = DataContainer.create();

        for (final Row callParticipantRow : callParticipantRows) {
            final Long callHistoryId = callParticipantRow.get(callHistoryIdColumn);
            final Row callDetailsRow = dataContainer.getRow(callDetailsTable, new WhereClause(Criteria.eq(callHistoryPKColumn, callHistoryId)));
            final Row callSummaryRow = dataContainer.getRow(callSummaryTable, new WhereClause(Criteria.eq(callSummaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID), callHistoryId)));

            callParticipantRow.set(MNCCALLPARTICIPANT.CONTACT_ID, contactId);
            targetContainer.updateRow(callParticipantRow);

            if (Objects.nonNull(callDetailsRow)) {
                final CallStatus callStatus = CallStatus.get((short) callDetailsRow.get(OMNCALLHISTORY.CALL_STATUS));
                final Long userId = callDetailsRow.get(OMNCALLHISTORY.USER_ID);

                if (callStatus.isCompletedStatus() && Objects.nonNull(userId)) {
                    final CallDetails callDetails = new CallDetails();
                    final CallSummary callSummary = new CallSummary();

                    callDetails.setId(callHistoryId);
                    callDetails.setOrgId(orgId);
                    callDetails.setStatus(callStatus);
                    callDetails.setType(CallType.get((short) callDetailsRow.get(OMNCALLHISTORY.CALL_TYPE)));
                    callDetails.setContact(contact);
                    callDetails.setParticipantUser(userService.getUser(orgId, userId));

                    callSummary.setOrgId(orgId);
                    callSummary.setStartTime(callSummaryRow.get(OMNCCALLSUMMARY.START_TIME));
                    callSummary.setEndTime(callSummaryRow.get(OMNCCALLSUMMARY.END_TIME));
                    callDetails.setSummary(callSummary);
                    loadCallHistoryIntoEntityConversations(callDetails, targetContainer);
                }
            }
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param contact        Represents the {@link Contact} to be linked
     * @param deal           Represents the {@link Deal} to be linked
     */
    @Override
    public void linkCallDetailsToDeal(final Long orgId, final Contact contact, final Deal deal) {
        final Table callDetailsTable = getTable();
        final Table callParticipantTable = getCallParticipantTable();
        final Table callSummaryTable = getSummaryTable();
        final Column callHistoryPKColumn = callDetailsTable.getPKColumn();
        final Column callHistoryIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID);
        final SelectQuery selectQuery = SelectQuery.get(callParticipantTable);

        selectQuery.addSelectClause(callParticipantTable);
        selectQuery.addSelectClause(List.of(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS),
                callDetailsTable.getColumn(OMNCALLHISTORY.CALL_TYPE), callDetailsTable.getColumn(OMNCALLHISTORY.USER_ID), callHistoryPKColumn));
        selectQuery.addSelectClause(callSummaryTable);
        selectQuery.addJoinClause(List.of(new JoinClause(callHistoryIdColumn, callHistoryPKColumn),
                new JoinClause(callHistoryIdColumn, callSummaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), contact.id())));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Row> callParticipantRows = dataContainer.getRows(callParticipantTable).toList();

        if (callParticipantRows.isEmpty()) {
            return;
        }

        final DataContainer targetContainer = DataContainer.create();
        final Collection<Long> callHistoryIds = callParticipantRows.stream().map(row -> (Long) row.get(callHistoryIdColumn)).filter(Objects::nonNull).toList();
        final Table dealCallTable = getDealCallTable();
        final Set<Long> dealLinkedCallHistoryIds = orgDataStore(orgId).get(dealCallTable,
                        new WhereClause(Criteria.in(dealCallTable.getColumn(MNCDEALCALL.CALL_HISTORY_ID), callHistoryIds))).getRows(dealCallTable)
                .map(dealCallRow -> (Long) dealCallRow.get(MNCDEALCALL.CALL_HISTORY_ID)).collect(Collectors.toSet());

        for (final Long callHistoryId : callHistoryIds) {
            if (!dealLinkedCallHistoryIds.contains(callHistoryId)) {
                loadDealIntoContainer(callHistoryId, deal, targetContainer);
            }
            final Row callDetailsRow = dataContainer.getRow(callDetailsTable, new WhereClause(Criteria.eq(callHistoryPKColumn, callHistoryId)));
            final Row callSummaryRow = dataContainer.getRow(callSummaryTable, new WhereClause(Criteria.eq(callSummaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID), callHistoryId)));

            if (Objects.nonNull(callDetailsRow)) {
                final CallStatus callStatus = CallStatus.get((short) callDetailsRow.get(OMNCALLHISTORY.CALL_STATUS));
                final Long userId = callDetailsRow.get(OMNCALLHISTORY.USER_ID);

                if (callStatus.isCompletedStatus() && Objects.nonNull(userId)) {
                    final CallDetails callDetails = new CallDetails();
                    final CallSummary callSummary = new CallSummary();

                    callDetails.setId(callHistoryId);
                    callDetails.setOrgId(orgId);
                    callDetails.setStatus(callStatus);
                    callDetails.setType(CallType.get((short) callDetailsRow.get(OMNCALLHISTORY.CALL_TYPE)));
                    callDetails.setContact(contact);
                    callDetails.setDeal(deal);
                    callDetails.setParticipantUser(userService.getUser(orgId, userId));
                    callDetails.setLoggedTime(callDetailsRow.get(OMNCALLHISTORY.LOGGED_TIME));
                    callSummary.setOrgId(orgId);
                    callSummary.setStartTime(callSummaryRow.get(OMNCCALLSUMMARY.START_TIME));
                    callSummary.setEndTime(callSummaryRow.get(OMNCCALLSUMMARY.END_TIME));
                    callDetails.setSummary(callSummary);
                    loadCallHistoryIntoEntityConversations(callDetails, targetContainer);
                }
            }
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param callHistoryId Represents the call history id
     * @return true if the call history record exists; false otherwise
     */
    @Override
    public boolean exists(final long orgId, final long callHistoryId) {
        final Table callDetailsTable = getTable();
        final CountQuery countQuery = CountQuery.get(callDetailsTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(callDetailsTable.getPKColumn(), callHistoryId)));
        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents the organisation ID.
     * @param externalCallId Represents the external call ID.
     * @return A flag that indicates whether the call is loggable or not.
     */
    @Override
    public boolean isLoggable(final long orgId, final String externalCallId) {
        final Table callDetailsTable = getTable();
        final Table callParticipantTable = getCallParticipantTable();
        final CountQuery countQuery = CountQuery.get(callDetailsTable);

        countQuery.addJoinClause(new JoinClause(callDetailsTable.getPKColumn(), callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(callDetailsTable.getColumn(OMNCALLHISTORY.EXTERNAL_CALL_ID), externalCallId))
                .and(Criteria.notEq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), null)));
        return 0 != orgDataStore(orgId).get(countQuery);
    }

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
    @Override
    public Collection<CallDetails> getRecentCalls(final Long orgId, final Long userId, final LimitConfig limitConfig) {
        final Table callDetailsTable = getTable();
        final Table callParticipantTable = getCallParticipantTable();
        final Table contactTable = contactManager.getTable();
        final Table phoneTable = phoneManager.getTable();

        final Column participantHistoryIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID);
        final SelectQuery selectQuery = SelectQuery.get(callDetailsTable);

        selectQuery.addSelectClause(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_TYPE));
        selectQuery.addSelectClause(callDetailsTable.getColumn(OMNCALLHISTORY.LOGGED_TIME));
        selectQuery.addSelectClause(callParticipantTable, contactTable, phoneTable);

        selectQuery.addJoinClause(new JoinClause(callDetailsTable.getPKColumn(), participantHistoryIdColumn));
        selectQuery.addJoinClause(new JoinClause(callParticipantTable.getColumn(MNCCALLPARTICIPANT.PHONE_ID), phoneTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), contactTable.getPKColumn(), JoinClause.JoinType.LEFT));
        addCallConversationCompanyJoins(callDetailsTable, selectQuery);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(callDetailsTable.getColumn(OMNCALLHISTORY.USER_ID), userId))
                .and(Criteria.in(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS), CallStatus.getCompletedStatuses())));
        selectQuery.addOrderByClause(new OrderByClause(callDetailsTable.getColumn(OMNCALLHISTORY.LOGGED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(limitConfig.getLimitClause());

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> callDetailsRows = dataContainer.getRows(callDetailsTable);

        return callDetailsRows.map(callDetailsRow -> {
            final Long callDetailsId = callDetailsRow.getPKValue();
            final Row participantRow = dataContainer.getRow(callParticipantTable, new WhereClause(Criteria.eq(participantHistoryIdColumn, callDetailsId)));
            final Long contactId = participantRow.get(MNCCALLPARTICIPANT.CONTACT_ID);
            final CallDetails callDetails = new CallDetails();

            callDetails.setId(callDetailsId);
            callDetails.setLoggedTime(callDetailsRow.get(OMNCALLHISTORY.LOGGED_TIME));
            callDetails.setType(CallType.get((short) callDetailsRow.get(OMNCALLHISTORY.CALL_TYPE)));
            callDetails.setContactNumber(convertRowIntoContactNumber(participantRow, dataContainer));

            if (Objects.nonNull(contactId)) {
                callDetails.setContact(convertRowIntoContact(contactId, callDetailsId, dataContainer));
            }
            return callDetails;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organisation ID.
     * @param id    Represents the call record id
     */
    @Override
    public void removeRecord(final long orgId, final long id) {
        final Table recordingsTable = getRecordingsTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(recordingsTable);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(recordingsTable.getPKColumn(), id)));
        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The organization ID.
     * @param limitConfig The pagination configuration.
     * @return A collection of {@link Contact}.
     */
    @Override
    public Collection<Contact> getRecentCallContacts(final long orgId, final LimitConfig limitConfig) {
        final Table callDetailsTable = getTable();
        final Table callParticipantTable = getCallParticipantTable();
        final Table contactTable = contactManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Column participantContactIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID);
        final Column participantHistoryIdColumn = callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID);
        final Column loggedTimeColumn = callDetailsTable.getColumn(OMNCALLHISTORY.LOGGED_TIME);
        final SelectQuery derivedCallTable = SelectQuery.get(callDetailsTable);
        final Column orgIdColumn = callDetailsTable.getColumn(OMNCALLHISTORY.ORG_ID);

        derivedCallTable.addSelectClause(List.of(orgIdColumn, participantContactIdColumn, loggedTimeColumn.max()));
        derivedCallTable.addJoinClause(new JoinClause(callDetailsTable.getPKColumn(), participantHistoryIdColumn));
        derivedCallTable.setWhereClause(new WhereClause(Criteria.in(callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS), CallStatus.getCompletedStatuses()))
                .and(Criteria.notEq(participantContactIdColumn, null)));
        derivedCallTable.addGroupByClause(List.of(new GroupByClause(participantContactIdColumn),
                new GroupByClause(orgIdColumn)));
        final DerivedTable latestCallTable = DerivedTable.get(callDetailsTable.getName(), NotificationFields.CALLS.getName(), derivedCallTable);
        final SelectQuery selectQuery = SelectQuery.get(callDetailsTable);
        final Column contactPKColumn = contactTable.getPKColumn();

        selectQuery.addSelectClause(List.of(
                callDetailsTable.getPKColumn(),
                participantContactIdColumn,
                phoneTable.getColumn(OMETPHONE.COUNTRY_ID),
                phoneTable.getColumn(OMETPHONE.NUMBER),
                phoneTable.getColumn(OMETPHONE.TYPE),
                callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                loggedTimeColumn));

        selectQuery.addJoinClause(new JoinClause(callDetailsTable.getPKColumn(), participantHistoryIdColumn));
        selectQuery.addJoinClause(new JoinClause(participantContactIdColumn, contactPKColumn));
        selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(callDetailsTable, latestCallTable, List.of(
                new JoinClause.Relation(participantContactIdColumn, latestCallTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID)),
                new JoinClause.Relation(loggedTimeColumn, latestCallTable.getColumn(OMNCALLHISTORY.LOGGED_TIME))
        ), JoinClause.JoinType.INNER));

        selectQuery.addOrderByClause(new OrderByClause(loggedTimeColumn, OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(limitConfig.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(callDetailsTable);

        return rows.map(row -> {
            final Long callHistoryId = row.getPKValue();

            if (Objects.isNull(callHistoryId)) {
                return null;
            }
            final Row callparticipantRow = dataContainer.getRow(callParticipantTable,
                    new WhereClause(Criteria.eq(participantHistoryIdColumn, callHistoryId)));

            if (Objects.isNull(callparticipantRow)) {
                return null;
            }
            final Long contactId = callparticipantRow.get(MNCCALLPARTICIPANT.CONTACT_ID);
            final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactPKColumn, contactId)));

            if (Objects.isNull(contactRow)) {
                return null;
            }
            final Contact contact = new Contact();

            contact.setId(contactId);
            contact.setOrgId(orgId);
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            final Long phoneId = contactRow.get(MCOCONTACT.PRIMARY_PHONE);

            if (Objects.nonNull(phoneId)) {
                final Row phoneRow = dataContainer.getRow(phoneTable, phoneId);

                if (Objects.nonNull(phoneRow)) {
                    final Phone phone = new Phone();
                    final Integer countryId = phoneRow.get(OMETPHONE.COUNTRY_ID);
                    final Integer type = phoneRow.get(OMETPHONE.TYPE);

                    phone.setId(phoneId);
                    phone.setNumber(phoneRow.get(OMETPHONE.NUMBER));

                    if (Objects.nonNull(countryId)) {
                        phone.setCountry(Country.getCountryById(countryId));
                    }

                    if (Objects.nonNull(type)) {
                        phone.setType(Phone.Type.valueOf(type));
                    }
                    contact.setPrimaryPhone(phone);
                }
            }
            return contact;
        }).filter(Objects::nonNull).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents the organisation ID.
     * @param callHistoryId  Represents the call history id
     * @param note           Represents the {@link Note}
     */
    @Override
    public void createNote(final long orgId, final long callHistoryId, final Note note) {
        final DataContainer targetContainer = DataContainer.create();
        final Table callNoteTable = getCallNoteTable();

        noteManager.loadRowIntoContainer(true, note, null, targetContainer);
        final Row callNoteRow = new Row(callNoteTable);

        callNoteRow.set(MNCCALLNOTE.CALL_HISTORY_ID, callHistoryId);
        callNoteRow.set(MNCCALLNOTE.NOTE_ID, note.id());

        targetContainer.addNewRow(callNoteRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organisation ID.
     * @param callHistoryId Represents the call history id.
     * @return The total count of notes.
     */
    @Override
    public long getNoteCount(final long orgId, final long callHistoryId) {
        final Table callNoteTable = getCallNoteTable();
        final CountQuery countQuery = CountQuery.get(callNoteTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(callNoteTable.getColumn(MNCCALLNOTE.CALL_HISTORY_ID), callHistoryId)));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organisation ID.
     * @param callHistoryId Represents the call history id.
     * @param limitConfig   The {@link LimitConfig} defining pagination limits.
     * @return A collection of {@link Note} objects.
     */
    @Override
    public Collection<Note> getNotes(final long orgId, final long callHistoryId, final LimitConfig limitConfig) {
        final Table callNoteTable = getCallNoteTable();
        final Table noteTable = noteManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(callNoteTable);

        selectQuery.addSelectClause(noteTable);
        selectQuery.addJoinClause(new JoinClause(callNoteTable.getColumn(MNCCALLNOTE.NOTE_ID), noteTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(callNoteTable.getColumn(MNCCALLNOTE.CALL_HISTORY_ID), callHistoryId)));
        selectQuery.addOrderByClause(new OrderByClause(noteTable.getColumn(OMNTNOTE.CREATED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(limitConfig.getLimitClause());

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(noteTable).map(noteRow -> noteManager.convertRowToModel(noteRow, dataContainer)).toList();
    }

    /**
     * Loads a {@link CallDetails} object and its associated data into the provided {@link DataContainer}.
     *
     * @param isCreateEvent     Indicates whether this operation is for a new creation (true) or an update (false).
     * @param callDetails       The {@link CallDetails} object containing all information about the call.
     * @param oldCallDetailsRow The existing row representing the call in the container. If null, a new row is created.
     * @param targetContainer   The {@link DataContainer} into which the data should be loaded.
     */
    private void loadCallDetailsIntoContainer(final boolean isCreateEvent, final CallDetails callDetails,
                                              final Row oldCallDetailsRow, final DataContainer targetContainer) {
        final Table callDetailsTable = getTable();
        final Long id = callDetails.id();
        final Row callDetailsRow = isCreateEvent ? new Row(callDetailsTable) : oldCallDetailsRow;

        if (Objects.isNull(callDetailsRow)) {
            throw ErrorCode.get(NotificationErrorCodes.CALL_DETAILS_NOT_FOUND, String.format("No call details found for the given id - %d", id));
        }
        final Long callDetailsId = callDetailsRow.getPKValue();
        final Phone contactNumber = callDetails.getContactNumber();
        final Contact contact = callDetails.getContact();
        final Long orgId = callDetails.orgId();

        if (callDetailsRow.isNewRow()) {
            final CallForwardDetails forwardDetails = callDetails.getForwardDetails();
            final PhoneNumber userNumber = callDetails.getUserNumber();
            Long userNumberId = userNumber.id();

            callDetails.setId(callDetailsId);
            callDetailsRow.setOrgId(orgId);
            callDetailsRow.set(OMNCALLHISTORY.CALL_TYPE, callDetails.getType().getId());
            callDetailsRow.set(OMNCALLHISTORY.IS_PARENT_CALL, callDetails.isParentCall());
            callDetailsRow.set(OMNCALLHISTORY.LOGGED_TIME, TemporalServiceProvider.get().getUtcDateTime());
            callDetailsRow.set(OMNCALLHISTORY.EXTERNAL_CALL_ID, callDetails.getExternalId());

            if (Objects.isNull(userNumberId)) {
                final Optional<PhoneNumber> optionPhoneNumber = phoneNumberManager.getNumber(orgId, userNumber.getNumber());

                if (optionPhoneNumber.isPresent()) {
                    userNumberId = optionPhoneNumber.get().id();
                } else {
                    final Table numberTable = phoneNumberManager.getTable();
                    final WhereClause phoneNumberCriteria = new WhereClause(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.NUMBER), userNumber.getNumber()))
                            .and(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.PROVIDER), userNumber.getTelephoneProvider().getId()))
                            .and(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.IS_ACTIVE), false));
                    final Row existingPhoneNumberRow = secureOrgDataStore(orgId).get(numberTable, phoneNumberCriteria).getRow(numberTable);
                    final boolean isPhoneNumberCreateEvent = Objects.isNull(existingPhoneNumberRow);
                    final Row phoneNumberRow = isPhoneNumberCreateEvent ? new Row(numberTable) : existingPhoneNumberRow;

                    phoneNumberManager.loadPhoneNumberIntoRow(isPhoneNumberCreateEvent, userNumber, phoneNumberRow);
                    targetContainer.updateRow(phoneNumberRow);
                    userNumberId = userNumber.id();
                }
                callDetailsRow.set(OMNCALLHISTORY.NUMBER_ID, userNumberId);
            }
            callDetailsRow.set(OMNCALLHISTORY.NUMBER_ID, userNumberId);

            if (Objects.nonNull(forwardDetails)) {
                loadForwardDetailsIntoContainer(callDetailsId, forwardDetails, targetContainer);

                if (Objects.nonNull(contact)) {
                    loadForwardDetailsToTimeline(orgId, contact.id(), forwardDetails, targetContainer);
                }
            }
            final Deal deal = callDetails.getDeal();

            if (Objects.nonNull(deal)) {
                loadDealIntoContainer(callDetailsId, deal, targetContainer);
            }
        }
        final Collection<CallRecord> callRecordings = callDetails.getCallRecordings();
        final CallSummary summary = callDetails.getSummary();
        final CrmUser participantUser = callDetails.getParticipantUser();

        callDetailsRow.set(OMNCALLHISTORY.IS_CALL_LOGGED, callDetails.isStoredAsCallLog());
        callDetailsRow.set(OMNCALLHISTORY.CALL_STATUS, callDetails.getStatus().getId());
        callDetailsRow.set(OMNCALLHISTORY.CALL_LOG_ID, callDetails.getCallLogId());

        if (Objects.nonNull(summary)) {
            loadSummaryIntoContainer(callDetailsId, summary, targetContainer);
        }

        if (Objects.nonNull(callRecordings) && !callRecordings.isEmpty()) {
            loadCallRecordingsIntoContainer(callDetailsId, callRecordings, targetContainer);
        }

        if (Objects.nonNull(contactNumber)) {
            loadContactIntoContainer(isCreateEvent, callDetailsId, contact, contactNumber, targetContainer);
        }

        if (!isCreateEvent) {
            loadDealIntoContainerIfNotLinked(orgId, callDetailsId, callDetails.getDeal(), contact, targetContainer);
        }

        if (Objects.nonNull(participantUser)) {
            callDetailsRow.set(OMNCALLHISTORY.USER_ID, participantUser.id());
            loadCallHistoryIntoEntityConversations(callDetails, targetContainer);
            loadCallDetailsToTimeline(callDetails, targetContainer);
        }
        targetContainer.updateRow(callDetailsRow);
    }

    /**
     * Loads a {@link CallSummary} and its associated Monetary cost object into the {@link DataContainer}.
     *
     * @param callDetailsId   The call ID to associate the summary.
     * @param summary         The {@link CallSummary} to persist.
     * @param targetContainer The {@link DataContainer} to insert the data into DB.
     */
    private void loadSummaryIntoContainer(final long callDetailsId, final CallSummary summary, final DataContainer targetContainer) {
        final Table summaryTable = getSummaryTable();
        final Long orgId = summary.orgId();
        final Row existingSummaryRow = orgDataStore(orgId).get(summaryTable,
                new WhereClause(Criteria.eq(summaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID), callDetailsId))).getRow(summaryTable);
        final Row summaryRow = Objects.isNull(existingSummaryRow) ? new Row(summaryTable) : existingSummaryRow;
        final Monetary callCost = summary.getCost();

        if (summaryRow.isNewRow()) {
            summaryRow.setOrgId(orgId);
            summaryRow.set(OMNCCALLSUMMARY.CALL_HISTORY_ID, callDetailsId);
        }
        summaryRow.set(OMNCCALLSUMMARY.START_TIME, summary.getStartTime());
        summaryRow.set(OMNCCALLSUMMARY.END_TIME, summary.getEndTime());
        summaryRow.set(OMNCCALLSUMMARY.DURATION, summary.getDuration());
        summaryRow.set(OMNCCALLSUMMARY.BILLING_DURATION, summary.getBillingDuration());

        if (Objects.nonNull(callCost)) {
            monetaryManager.loadMonetaryIntoContainer(true, callCost, targetContainer);
            summaryRow.set(OMNCCALLSUMMARY.COST, callCost.id());
        }
        targetContainer.updateRow(summaryRow);
    }

    /**
     * Persists a list of {@link CallRecord} objects into the {@link DataContainer}.
     * Each recording is stored as a separate row linked to the call ID.
     *
     * @param callDetailsId   The ID of the call record.
     * @param callRecordings  The list of {@link CallRecord} objects to store.
     * @param targetContainer The {@link DataContainer} to insert the recording rows into DB.
     */
    private void loadCallRecordingsIntoContainer(final long callDetailsId, final Collection<CallRecord> callRecordings,
                                                 final DataContainer targetContainer) {
        final Table recordingsTable = getRecordingsTable();

        for (final CallRecord callRecord : callRecordings) {
            final Row recordDetailsRow = new Row(recordingsTable);

            recordDetailsRow.set(MNCCALLRECORDDETAILS.EXTERNAL_RECORD_ID, callRecord.getServiceId());
            recordDetailsRow.set(MNCCALLRECORDDETAILS.URI, callRecord.getUri());
            recordDetailsRow.set(MNCCALLRECORDDETAILS.CALL_HISTORY_ID, callDetailsId);
            targetContainer.addNewRow(recordDetailsRow);
        }
    }

    /**
     * Loads a {@link Contact} into the {@link DataContainer}.
     *
     * @param isCreateEvent   Indicates whether this operation is for a new creation (true) or an update (false).
     * @param callDetailsId   The ID of the call record.
     * @param contact         The {@link Contact} object to persist.
     * @param phone           The {@link Phone} object to persist.
     * @param targetContainer The {@link DataContainer} to update.
     */
    private void loadContactIntoContainer(final boolean isCreateEvent, final long callDetailsId, final Contact contact, final Phone phone,
                                          final DataContainer targetContainer) {
        final Table callParticipantTable = getCallParticipantTable();
        final Row callParticipantRow = isCreateEvent ? new Row(callParticipantTable) :
                adminDataStore.get(callParticipantTable, new WhereClause(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID), callDetailsId))).getRow(callParticipantTable);

        if (Objects.nonNull(contact)) {
            callParticipantRow.set(MNCCALLPARTICIPANT.CONTACT_ID, contact.id());
        }
        callParticipantRow.set(MNCCALLPARTICIPANT.PHONE_ID, phone.id());
        callParticipantRow.set(MNCCALLPARTICIPANT.CALL_HISTORY_ID, callDetailsId);
        targetContainer.updateRow(callParticipantRow);
    }

    /**
     * Links a deal to an existing call if it is not already linked. Since the deal association is created only
     * when the call is created, calls linked to a contact or deal created later may otherwise miss the deal in
     * phone history.
     *
     * @param orgId           The organization ID
     * @param callDetailsId   The call details ID
     * @param deal            The deal associated with the call
     * @param contact         The contact used to resolve the deal
     * @param targetContainer The {@link DataContainer} to update
     */
    private void loadDealIntoContainerIfNotLinked(final Long orgId, final long callDetailsId, final Deal deal, final Contact contact,
                                                  final DataContainer targetContainer) {
        final Table dealCallTable = getDealCallTable();
        final boolean isDealLinked = orgDataStore(orgId).get(dealCallTable,
                new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALL.CALL_HISTORY_ID), callDetailsId))).getRow(dealCallTable) != null;

        if (isDealLinked) {
            return;
        }
        final Deal callDeal = Objects.nonNull(deal) && Objects.nonNull(deal.id()) ? deal
                : Objects.nonNull(contact) && Objects.nonNull(contact.id()) ? dealManager.findSingleDealByContact(orgId, contact.id()).orElse(null) : null;

        if (Objects.nonNull(callDeal) && Objects.nonNull(callDeal.id())) {
            loadDealIntoContainer(callDetailsId, callDeal, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the {@link Deal} into the {@link DataContainer}
     * </p>
     *
     * @param callDetailsId   Represents the call detail id
     * @param deal            Represents the {@link Deal}
     * @param targetContainer The {@link DataContainer} to update
     */
    private void loadDealIntoContainer(final long callDetailsId, final Deal deal,
                                       final DataContainer targetContainer) {
        final Row dealCallRow = new Row(getDealCallTable());

        dealCallRow.set(MNCDEALCALL.DEAL_ID, deal.id());
        dealCallRow.set(MNCDEALCALL.CALL_HISTORY_ID, callDetailsId);
        targetContainer.addNewRow(dealCallRow);
    }

    /**
     * Loads a {@link CallForwardDetails} into the {@link DataContainer}.
     *
     * @param callDetailsId   The ID of the call record.
     * @param forwardDetails  The {@link CallForwardDetails} object to persist.
     * @param targetContainer The {@link DataContainer} to update.
     */
    private void loadForwardDetailsIntoContainer(final long callDetailsId, final CallForwardDetails forwardDetails,
                                                 final DataContainer targetContainer) {
        final Row forwardDetailsRow = new Row(getForwardHistoryTable());

        forwardDetailsRow.set(MNCCALLFORWARDHISTORY.FORWARDED_BY, forwardDetails.getForwardedBy().id());
        forwardDetailsRow.set(MNCCALLFORWARDHISTORY.FORWARDED_TO, forwardDetails.getForwardedTo().id());
        forwardDetailsRow.set(MNCCALLFORWARDHISTORY.IS_RING_BACK_ENABLED, forwardDetails.isRingBackEnabled());
        forwardDetailsRow.set(MNCCALLFORWARDHISTORY.CALL_HISTORY_ID, callDetailsId);
        targetContainer.addNewRow(forwardDetailsRow);
    }

    /**
     * Adds the necessary JOIN clauses to the given {@link MultiQuery} object to link
     * {@link CallDetails} with related Contact, Phone, and Call association tables.
     *
     * @param multiQuery the {@link MultiQuery} object to which join clauses are added.
     */
    private void loadSearchCriteriaJoins(final MultiQuery multiQuery) {
        final Table phoneTable = phoneManager.getTable();
        final Table contactTable = contactManager.getTable();
        final Table callParticipantTable = getCallParticipantTable();

        multiQuery.addJoinClause(new JoinClause(getTable().getPKColumn(), callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID)));
        multiQuery.addJoinClause(new JoinClause(callParticipantTable.getColumn(MNCCALLPARTICIPANT.PHONE_ID), phoneTable.getPKColumn()));
        multiQuery.addJoinClause(new JoinClause(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), contactTable.getPKColumn(), JoinClause.JoinType.LEFT));
    }

    /**
     * Constructs a where clause that filters for parent or child calls.
     *
     * @param includeParentCall true to include only parent calls, false for child calls.
     * @return a WhereClause to be applied to the query.
     */
    private WhereClause getParentCallCriteria(final boolean includeParentCall) {
        return new WhereClause(Criteria.eq(getTable().getColumn(OMNCALLHISTORY.IS_PARENT_CALL), includeParentCall));
    }

    /**
     * Fetches a {@link DataContainer} which contains all associated data for {@link CallDetails}.
     *
     * @param orgId       Represents the organisation id.
     * @param whereClause the filtering condition to apply to the base {@link CallDetails} table.
     * @return the {@link DataContainer}.
     */
    private DataContainer fetchCallDetailsContainer(final Long orgId, final WhereClause whereClause) {
        final ReadableDataStore readableDataStore = Objects.isNull(orgId) ? adminDataStore : secureOrgDataStore(orgId);
        final SelectQuery selectQuery = buildCallBasicDetailsSelectQuery(whereClause);
        final DataContainer basicDetailsContainer = readableDataStore.get(selectQuery);
        final Collection<Long> callDetailIds = basicDetailsContainer.getRows(getTable()).map(Row::getPKValue).collect(Collectors.toSet());

        if (!callDetailIds.isEmpty()) {
            final WhereClause callIdCriteria = new WhereClause(Criteria.in(getTable().getPKColumn(), callDetailIds));
            final DataContainer participantsContainer = readableDataStore.get(buildCallParticipantsSelectQuery(callIdCriteria));
            final DataContainer extraDetailsContainer = readableDataStore.get(buildCallExtraDetailsSelectQuery(callIdCriteria));

            basicDetailsContainer.append(participantsContainer);
            basicDetailsContainer.append(extraDetailsContainer);
        }
        return basicDetailsContainer;
    }

    /**
     * Constructs a {@link SelectQuery} to fetch basic data for {@link CallDetails}.
     *
     * @param whereClause the filtering condition to apply to the base {@link CallDetails} table.
     * @return the constructed {@link SelectQuery} with appropriate joins.
     */
    private SelectQuery buildCallBasicDetailsSelectQuery(final WhereClause whereClause) {
        final Table callDetailsTable = getTable();
        final Table summaryTable = getSummaryTable();
        final Table recordingsTable = getRecordingsTable();
        final Table forwardHistoryTable = getForwardHistoryTable();
        final Table userNumberTable = phoneNumberManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();

        final Column callDetailsPkColumn = callDetailsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(callDetailsTable);

        selectQuery.addJoinClause(new JoinClause(callDetailsPkColumn, summaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(summaryTable.getColumn(OMNCCALLSUMMARY.COST), monetaryTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(callDetailsTable.getColumn(OMNCALLHISTORY.NUMBER_ID), userNumberTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(callDetailsPkColumn, recordingsTable.getColumn(MNCCALLRECORDDETAILS.CALL_HISTORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(callDetailsPkColumn, forwardHistoryTable.getColumn(MNCCALLFORWARDHISTORY.CALL_HISTORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addOrderByClause(new OrderByClause(callDetailsTable.getColumn(OMNCALLHISTORY.LOGGED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * Constructs a {@link SelectQuery} to fetch the participant details for {@link CallDetails}.
     *
     * @param whereClause the filtering condition to apply to the base {@link CallDetails} table.
     * @return the constructed {@link SelectQuery} with appropriate joins.
     */
    private SelectQuery buildCallParticipantsSelectQuery(final WhereClause whereClause) {
        final Table callDetailsTable = getTable();
        final Table callParticipantTable = getCallParticipantTable();
        final Table phoneTable = phoneManager.getTable();
        final Table contactTable = contactManager.getTable();

        final Column contactPkColumn = contactTable.getPKColumn();
        final Column callDetailsPkColumn = callDetailsTable.getPKColumn();
        final Optional<FieldInfo> optionalCompanyField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, NotificationFields.COMPANY.getName());
        final Optional<FieldInfo> optionalPhoneField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.PHONES);
        final SelectQuery selectQuery = SelectQuery.get(callDetailsTable);
        final Column phonePkColumn = phoneTable.getPKColumn();

        selectQuery.addSelectClause(callParticipantTable, phoneTable);
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.ORG_ID));
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.FIRST_NAME));
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.LAST_NAME));
        selectQuery.addJoinClause(new JoinClause(callDetailsPkColumn, callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID)));
        selectQuery.addJoinClause(new JoinClause(callParticipantTable.getColumn(MNCCALLPARTICIPANT.PHONE_ID), phonePkColumn));
        selectQuery.addJoinClause(new JoinClause(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), contactPkColumn, JoinClause.JoinType.LEFT));
        addCallConversationCompanyJoins(callDetailsTable, selectQuery);

        if (optionalPhoneField.isPresent()) {
            final Table contactPhoneTable = Table.get(MCONPHONEFIELDS.TABLE);
            final Table contactAliasTable = getContactAliasTable();
            final Column contactAliasTablePkColumn = contactAliasTable.getPKColumn();
            final SelectQuery contactPhoneSelectQuery = SelectQuery.get(contactPhoneTable);
            final DerivedTable derivedContactPhoneTable = DerivedTable.get(contactPhoneTable.getName(), contactPhoneTable.getAlias(), contactPhoneSelectQuery);

            contactPhoneSelectQuery.addSelectClause(contactPhoneTable);
            contactPhoneSelectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), optionalPhoneField.get().id())));

            selectQuery.addSelectClause(derivedContactPhoneTable);
            selectQuery.addSelectClause(contactAliasTable.getColumn(MCOCONTACT.ORG_ID));
            selectQuery.addSelectClause(contactAliasTable.getColumn(MCOCONTACT.FIRST_NAME));
            selectQuery.addSelectClause(contactAliasTable.getColumn(MCOCONTACT.LAST_NAME));

            selectQuery.addJoinClause(new JoinClause(phonePkColumn, derivedContactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), JoinClause.JoinType.LEFT));
            selectQuery.addJoinClause(new JoinClause(derivedContactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactAliasTablePkColumn,
                    JoinClause.JoinType.LEFT));

            if (optionalCompanyField.isPresent()) {
                final Table companyTable = getCompanyAliasTable();
                final Table contactCompanyTable = contactManager.getContactCompanyTable();
                final DerivedTable derivedContactCompanyTable = buildDerivedTable(contactCompanyTable,
                        contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), optionalCompanyField.get().id(), NotificationFields.CONTACT_COMPANY.getName());

                selectQuery.addSelectClause(derivedContactCompanyTable);
                selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
                selectQuery.addJoinClause(new JoinClause(contactAliasTablePkColumn, derivedContactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT));
                selectQuery.addJoinClause(new JoinClause(derivedContactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), JoinClause.JoinType.LEFT));
            }
        }
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * Constructs a {@link SelectQuery} to fetch the extra details (Like - note, deal) for {@link CallDetails}.
     *
     * @param whereClause the filtering condition to apply to the base {@link CallDetails} table.
     * @return the constructed {@link SelectQuery} with appropriate joins.
     */
    private SelectQuery buildCallExtraDetailsSelectQuery(final WhereClause whereClause) {
        final Table callDetailsTable = getTable();
        final Table dealCallTable = getDealCallTable();
        final Table callNoteTable = getCallNoteTable();
        final Table dealTable = dealManager.getTable();
        final Table noteTable = noteManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();

        final Column dealPipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column dealMonetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column callDetailsPkColumn = callDetailsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(callDetailsTable);

        selectQuery.addSelectClause(dealCallTable, callNoteTable, monetaryTable);
        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE),
                dealMonetaryIdColumn, dealPipelineStageIdColumn, noteTable.getColumn(OMNTNOTE.CONTENT), noteTable.getColumn(OMNTNOTE.CREATED_TIME),
                noteTable.getColumn(OMNTNOTE.CREATED_BY), noteTable.getColumn(OMNTNOTE.ORG_ID), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        selectQuery.addJoinClause(new JoinClause(callDetailsPkColumn, dealCallTable.getColumn(MNCDEALCALL.CALL_HISTORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(dealCallTable.getColumn(MNCDEALCALL.DEAL_ID), dealTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(callDetailsPkColumn, callNoteTable.getColumn(MNCCALLNOTE.CALL_HISTORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(callNoteTable.getColumn(MNCCALLNOTE.NOTE_ID), noteTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(dealPipelineStageIdColumn, pipelineStageTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(dealMonetaryIdColumn, monetaryTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * {@inheritDoc}
     *
     * @param callDetailsRow the primary row representing a call.
     * @param dataContainer  the container holding all joined rows.
     * @return A {@link CallDetails} object built from the provided data.
     */
    @Override
    public CallDetails convertRowIntoCallDetails(final Row callDetailsRow, final DataContainer dataContainer) {
        final Table summaryTable = getSummaryTable();
        final Table phoneNumberTable = phoneNumberManager.getTable();
        final Table recordingsTable = getRecordingsTable();
        final Table forwardHistoryTable = getForwardHistoryTable();
        final Table callParticipantTable = getCallParticipantTable();
        final Table dealCallTable = getDealCallTable();
        final Table callNoteTable = getCallNoteTable();
        final Long callDetailsId = callDetailsRow.getPKValue();
        final CallDetails callDetails = new CallDetails();
        final Long orgId = callDetailsRow.getOrgId();
        final Long userId = callDetailsRow.get(OMNCALLHISTORY.USER_ID);
        final Short callStatus = callDetailsRow.get(OMNCALLHISTORY.CALL_STATUS);
        final Short callType = callDetailsRow.get(OMNCALLHISTORY.CALL_TYPE);
        final Boolean isParentCall = callDetailsRow.get(OMNCALLHISTORY.IS_PARENT_CALL);
        final Boolean isStoredAsCallLog = callDetailsRow.get(OMNCALLHISTORY.IS_CALL_LOGGED);
        final Row summaryRow = dataContainer.getRow(summaryTable,
                new WhereClause(Criteria.eq(summaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID), callDetailsId)));
        final Row userNumberRow = dataContainer.getRow(phoneNumberTable,
                new WhereClause(Criteria.eq(phoneNumberTable.getPKColumn(), callDetailsRow.get(OMNCALLHISTORY.NUMBER_ID))));
        final Row callParticipantRow = dataContainer.getRow(callParticipantTable,
                new WhereClause(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID), callDetailsId)));
        final Row dealCallRow = dataContainer.getRow(dealCallTable,
                new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALL.CALL_HISTORY_ID), callDetailsId)));
        final Row forwardHistoryRow = dataContainer.getRow(forwardHistoryTable,
                new WhereClause(Criteria.eq(forwardHistoryTable.getColumn(MNCCALLFORWARDHISTORY.CALL_HISTORY_ID), callDetailsId)));
        final Stream<Row> callNoteRows = dataContainer.getRows(callNoteTable,
                new WhereClause(Criteria.eq(callNoteTable.getColumn(MNCCALLNOTE.CALL_HISTORY_ID), callDetailsId)));
        final Stream<Row> callRecordRows = dataContainer.getRows(recordingsTable,
                new WhereClause(Criteria.eq(recordingsTable.getColumn(MNCCALLRECORDDETAILS.CALL_HISTORY_ID), callDetailsId)));

        callDetails.setId(callDetailsId);
        callDetails.setOrgId(orgId);
        callDetails.setExternalId(callDetailsRow.get(OMNCALLHISTORY.EXTERNAL_CALL_ID));
        callDetails.setLoggedTime(callDetailsRow.get(OMNCALLHISTORY.LOGGED_TIME));
        callDetails.setCallLogId(callDetailsRow.get(OMNCALLHISTORY.CALL_LOG_ID));

        if (Objects.nonNull(isParentCall)) {
            callDetails.setParentCall(isParentCall);
        }

        if (Objects.nonNull(isStoredAsCallLog)) {
            callDetails.setStoredAsCallLog(isStoredAsCallLog);
        }

        if (Objects.nonNull(callStatus)) {
            callDetails.setStatus(CallStatus.get(callStatus));
        }

        if (Objects.nonNull(callType)) {
            callDetails.setType(CallType.get(callType));
        }

        if (Objects.nonNull(userId)) {
            callDetails.setParticipantUser(userService.getUser(orgId, userId));
        }

        if (Objects.nonNull(summaryRow)) {
            callDetails.setSummary(convertRowIntoSummary(summaryRow, dataContainer));
        }

        if (Objects.nonNull(callParticipantRow)) {
            final Long contactId = callParticipantRow.get(MNCCALLPARTICIPANT.CONTACT_ID);

            if (Objects.nonNull(contactId)) {
                callDetails.setContact(convertRowIntoContact(contactId, callDetailsId, dataContainer));
            } else {
                final Long phoneId = callParticipantRow.get(MNCCALLPARTICIPANT.PHONE_ID);

                if (Objects.nonNull(phoneId)) {
                    final Table contactPhoneTable = Table.get(MCONPHONEFIELDS.TABLE);
                    final Row contactPhoneRow = dataContainer.getRow(contactPhoneTable,
                            new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneId)));

                    if (Objects.nonNull(contactPhoneRow)) {
                        final Table contactAliasTable = getContactAliasTable();
                        final Row contactRow = dataContainer.getRow(contactAliasTable, new WhereClause(Criteria.eq(contactAliasTable.getPKColumn(),
                                contactPhoneRow.get(MCONPHONEFIELDS.ENTITY_ID))));

                        if (Objects.nonNull(contactRow)) {
                            final Contact contact = contactManager.convertRowToPartialModel(contactRow);
                            final Company callTimeCompany = getCompanyFromCallConversation(callDetailsId, dataContainer);

                            if (Objects.nonNull(callTimeCompany)) {
                                contact.setCompany(callTimeCompany);
                            }
                            callDetails.setContact(contact);
                        }
                    }
                }
            }
            callDetails.setContactNumber(convertRowIntoContactNumber(callParticipantRow, dataContainer));
        }

        if (Objects.nonNull(dealCallRow)) {
            callDetails.setDeal(convertRowIntoDeal(dealCallRow, dataContainer));
        }

        if (Objects.nonNull(userNumberRow)) {
            callDetails.setUserNumber(phoneNumberManager.convertRowToModel(userNumberRow, dataContainer));
        }

        if (Objects.nonNull(forwardHistoryRow)) {
            callDetails.setForwardDetails(convertRowIntoCallForwardDetails(orgId, forwardHistoryRow));
        }
        callDetails.setCallRecordings(callRecordRows.map(this::convertRowIntoCallRecord).toList());
        callDetails.setNotes(callNoteRows.map(callNoteRow -> convertRowIntoNote(callNoteRow, dataContainer)).toList());

        return callDetails;
    }

    /**
     * Converts a call recording row into a {@link CallRecord} model.
     *
     * @param callRecordRow the row containing call recording details
     * @return a populated {@link CallRecord} object
     */
    private CallRecord convertRowIntoCallRecord(final Row callRecordRow) {
        final CallRecord callRecord = new CallRecord();

        callRecord.setId(callRecordRow.getPKValue());
        callRecord.setServiceId(callRecordRow.get(MNCCALLRECORDDETAILS.EXTERNAL_RECORD_ID));
        callRecord.setUri(callRecordRow.get(MNCCALLRECORDDETAILS.URI));

        return callRecord;
    }

    /**
     * Converts a row into {@link CallForwardDetails}, retrieving forwarding users.
     *
     * @param orgId                 the organization ID to fetch users.
     * @param callForwardDetailsRow the row containing forwarding information.
     * @return a {@link CallForwardDetails}.
     */
    private CallForwardDetails convertRowIntoCallForwardDetails(final long orgId, final Row callForwardDetailsRow) {
        final CallForwardDetails callForwardDetails = new CallForwardDetails();

        callForwardDetails.setId(callForwardDetailsRow.getPKValue());
        callForwardDetails.setForwardedBy(userService.getUser(orgId, callForwardDetailsRow.get(MNCCALLFORWARDHISTORY.FORWARDED_BY)));
        callForwardDetails.setForwardedTo(userService.getUser(orgId, callForwardDetailsRow.get(MNCCALLFORWARDHISTORY.FORWARDED_TO)));
        callForwardDetails.setRingBackEnabled(callForwardDetailsRow.get(MNCCALLFORWARDHISTORY.IS_RING_BACK_ENABLED));

        return callForwardDetails;
    }

    /**
     * Converts a call summary row into a {@link CallSummary} model.
     *
     * @param summaryRow    the row containing call summary data
     * @param dataContainer the container to retrieve dependent rows like monetary cost
     * @return a {@link CallSummary} model with relevant call summary info
     */
    private CallSummary convertRowIntoSummary(final Row summaryRow, final DataContainer dataContainer) {
        final CallSummary callSummary = new CallSummary();
        final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), (Long) summaryRow.get(OMNCCALLSUMMARY.COST));

        callSummary.setId(summaryRow.getPKValue());
        callSummary.setOrgId(summaryRow.getOrgId());
        callSummary.setStartTime(summaryRow.get(OMNCCALLSUMMARY.START_TIME));
        callSummary.setEndTime(summaryRow.get(OMNCCALLSUMMARY.END_TIME));
        callSummary.setDuration(summaryRow.get(OMNCCALLSUMMARY.DURATION));
        callSummary.setBillingDuration(summaryRow.get(OMNCCALLSUMMARY.BILLING_DURATION));

        if (Objects.nonNull(monetaryRow)) {
            callSummary.setCost(monetaryManager.convertRowToModel(monetaryRow));
        }

        return callSummary;
    }

    /**
     * Converts a deal call row into a {@link Deal} model.
     *
     * @param dealCallRow   the row that holds the deal details.
     * @param dataContainer the container used to fetch the deal row
     * @return a {@link Deal}.
     */
    private Deal convertRowIntoDeal(final Row dealCallRow, final DataContainer dataContainer) {
        final Long dealId = dealCallRow.get(MNCDEALCALL.DEAL_ID);

        if (Objects.isNull(dealId)) {
            return null;
        }
        final Row dealRow = dataContainer.getRow(dealManager.getTable(), dealId);

        if (Objects.isNull(dealRow)) {
            return null;
        }
        final Deal deal = dealManager.convertRowToPartialModel(dealRow);
        final Long monetaryId = dealRow.get(MDODEAL.MONETARY_ID);
        final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);

        if (Objects.nonNull(monetaryId)) {
            final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), monetaryId);

            deal.setDealValue(new DealValue(monetaryManager.convertRowToModel(monetaryRow)));
        }

        if (Objects.nonNull(pipelineStageId)) {
            final Row pipelineStageRow = dataContainer.getRow(pipelineStageManager.getTable(), pipelineStageId);

            deal.setPipelineStage(pipelineStageManager.convertPartialRowToModel(pipelineStageRow));
        }
        deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));

        return deal;
    }

    /**
     * Converts a contact call row into a {@link Contact} model.
     *
     * @param contactId     the row that holds the contact id.
     * @param callDetailsId the call history id used to resolve the call-time company snapshot.
     * @param dataContainer the container used to fetch the contact row.
     * @return a {@link Contact}.
     */
    private Contact convertRowIntoContact(final Long contactId, final Long callDetailsId, final DataContainer dataContainer) {
        final Row contactRow = dataContainer.getRow(contactManager.getTable(), contactId);

        if (Objects.isNull(contactRow)) {
            return null;
        }
        final Contact contact = contactManager.convertRowToPartialModel(contactRow);
        final Company callTimeCompany = getCompanyFromCallConversation(callDetailsId, dataContainer);

        if (Objects.nonNull(callTimeCompany)) {
            contact.setCompany(callTimeCompany);
        }
        return contact;
    }

    /**
     * Resolves the company associated with a call from the call-time stored in entity conversations.
     *
     * @param callDetailsId the call history id
     * @param dataContainer the container holding joined call conversation rows
     * @return the {@link Company} at call time, or null if no exists
     */
    private Company getCompanyFromCallConversation(final Long callDetailsId, final DataContainer dataContainer) {
        final Table callConversationTable = getCallConversationTable();
        final Table companyConversationTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
        final Table conversationCompanyTable = getConversationCompanyAliasTable();
        final Row callConversationRow = dataContainer.getRow(callConversationTable,
                new WhereClause(Criteria.eq(callConversationTable.getColumn(OMECCALLCONVERSATIONS.CALL_HISTORY_ID), callDetailsId)));

        if (Objects.isNull(callConversationRow)) {
            return null;
        }
        final Long entityConversationId = callConversationRow.get(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Row companyConversationRow = dataContainer.getRow(companyConversationTable,
                new WhereClause(Criteria.eq(companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationId)));

        if (Objects.isNull(companyConversationRow)) {
            return null;
        }
        final Long companyId = companyConversationRow.get(OMECCOMPANYCONVERSATIONS.COMPANY_ID);
        final Row companyRow = dataContainer.getRow(conversationCompanyTable, companyId);

        if (Objects.nonNull(companyRow)) {
            return companyManager.convertRowToPartialModel(companyRow);
        }
        return null;
    }

    /**
     * Adds joins to resolve the company stored at call completed.
     *
     * @param callDetailsTable the call history table
     * @param selectQuery  the query to augment with call-time company joins
     */
    private void addCallConversationCompanyJoins(final Table callDetailsTable, final SelectQuery selectQuery) {
        final Table callConversationTable = getCallConversationTable();
        final Table companyConversationTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
        final Table conversationCompanyTable = getConversationCompanyAliasTable();
        final Column callDetailsPkColumn = callDetailsTable.getPKColumn();

        selectQuery.addSelectClause(callConversationTable);
        selectQuery.addSelectClause(companyConversationTable);
        selectQuery.addSelectClause(conversationCompanyTable.getColumn(MCOCOMPANY.NAME));
        selectQuery.addJoinClause(new JoinClause(callDetailsPkColumn, callConversationTable.getColumn(OMECCALLCONVERSATIONS.CALL_HISTORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(callConversationTable.getColumn(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID),
                companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID),
                conversationCompanyTable.getPKColumn(), JoinClause.JoinType.LEFT));
    }

    /**
     * Converts a contact number row into a {@link Phone} model.
     *
     * @param contactNumberRow the row linking contact to phone number
     * @param dataContainer    the container used to fetch the phone row
     * @return a {@link Phone}.
     */
    private Phone convertRowIntoContactNumber(final Row contactNumberRow, final DataContainer dataContainer) {
        final Row phoneRow = dataContainer.getRow(phoneManager.getTable(), (Long) contactNumberRow.get(MNCCALLPARTICIPANT.PHONE_ID));

        if (Objects.isNull(phoneRow)) {
            return null;
        }
        return phoneManager.convertRowToModel(phoneRow);
    }

    /**
     * Converts a call note row into a {@link Note} model.
     *
     * @param callNoteRow   the row linking note to a call
     * @param dataContainer the container used to fetch the full note row
     * @return a {@link Note}
     */
    private Note convertRowIntoNote(final Row callNoteRow, final DataContainer dataContainer) {
        final Row noteRow = dataContainer.getRow(noteManager.getTable(), (Long) callNoteRow.get(MNCCALLNOTE.NOTE_ID));

        return noteManager.convertRowToModel(noteRow, dataContainer);
    }

    /**
     * Resolves a field name string into a corresponding {@link Column} object.
     *
     * @param field the string field name (e.g., "status", "type")
     * @return the corresponding Column for filtering
     */
    private Column getColumn(final String field) {
        final Table callDetailsTable = getTable();

        return switch (field.toLowerCase()) {
            case CallHistoryFields.SALES_OWNER -> callDetailsTable.getColumn(OMNCALLHISTORY.USER_ID);
            case CallHistoryFields.PHONE_NUMBER -> callDetailsTable.getColumn(OMNCALLHISTORY.NUMBER_ID);
            case CallHistoryFields.TYPE -> callDetailsTable.getColumn(OMNCALLHISTORY.CALL_TYPE);
            case CallHistoryFields.STATUS -> callDetailsTable.getColumn(OMNCALLHISTORY.CALL_STATUS);
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_FILTER_FIELD);
        };
    }

    /**
     * Builds a {@link WhereClause} for search text matching first name, last name, or phone number.
     *
     * @param searchText the input text to match against contact or phone number fields
     * @return an {@link Optional} containing the {@link WhereClause}, if searchText is not null
     */
    private Optional<WhereClause> buildSearchCriteria(final String searchText) {
        if (Objects.isNull(searchText)) {
            return Optional.empty();
        }
        final Table contactTable = contactManager.getTable();
        final Table phoneTable = phoneManager.getTable();

        return Optional.of(new WhereClause(Criteria.contains(contactTable.getColumn(MCOCONTACT.FIRST_NAME), searchText))
                .or(Criteria.contains(contactTable.getColumn(MCOCONTACT.LAST_NAME), searchText))
                .or(Criteria.contains(phoneTable.getColumn(OMETPHONE.NUMBER), searchText)));
    }

    /**
     * <p>
     * Loads the call details into entity conversations
     * </p>
     *
     * @param callDetails     Represents the {@link CallDetails}
     * @param targetContainer The {@link DataContainer} into which the data should be loaded
     */
    private void loadCallHistoryIntoEntityConversations(final CallDetails callDetails, final DataContainer targetContainer) {
        final CallStatus callStatus = callDetails.getStatus();
        final CrmUser participantUser = callDetails.getParticipantUser();

        LOGGER.info("TeleCMI Webhook [TRACE]: loadCallHistoryIntoEntityConversations - CallId: {}, callStatus: {}, isCompletedStatus: {}, participantUser: {}, summary: {}",
                callDetails.id(), callStatus, Objects.nonNull(callStatus) && callStatus.isCompletedStatus(), Objects.nonNull(participantUser) ? participantUser.id() : null, callDetails.getSummary());

        if (callStatus.isCompletedStatus() && Objects.nonNull(participantUser)) {
            final Contact contact = callDetails.getContact();
            final Long orgId = callDetails.orgId();
            final Deal deal = callDetails.getDeal();
            final Long callId = callDetails.id();
            final UserRegionalPreference regionalPreference = participantUser.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
            final CallSummary callSummary = callDetails.getSummary();
            final long currentTime = Instant.now().toEpochMilli();
            final DateTime summaryStartTime = Objects.isNull(callSummary) ? null : callSummary.getStartTime();
            final DateTime loggedDateTime = callDetails.getLoggedTime();
            final boolean isSummaryStartTimeUnavailable = Objects.isNull(summaryStartTime);
            final Long startTimestamp = isSummaryStartTimeUnavailable ? Objects.isNull(loggedDateTime) ? currentTime :loggedDateTime.getTimeStamp()
                    : summaryStartTime.getTimeStamp();

            LOGGER.info("TeleCMI Webhook [TRACE]: loadCallHistoryIntoEntityConversations - CallId: {}, isSummaryStartTimeUnavailable: {}, loggedDateTime: {}, startTimestamp: {}",
                    callId, isSummaryStartTimeUnavailable, loggedDateTime, startTimestamp);

            if (Objects.nonNull(contact)) {
                final Long contactId = contact.id();

                if (Objects.isNull(contactId)) {
                    return;
                }
                final Long userId = callDetails.getParticipantUser().id();

                if (isContactLinkedWithCall(orgId, contactId, callId)) {
                    final Company contactCompany = contact.getCompany();
                    final Long companyId = Objects.isNull(contactCompany) ? contactManager.getRelatedCompanyId(orgId, contactId).orElse(null) : contactCompany.id();

                    if (Objects.nonNull(companyId)) {
                        final Collection<Long> entityConversationIds = getEntityConversationIdForContact(orgId, contactId, callId);

                        for (final Long entityConversationId : entityConversationIds) {
                            loadCompanyConversation(orgId, companyId, entityConversationId, callId, targetContainer);
                        }
                    }
                } else {
                    final Table callConversationTable = getCallConversationTable();
                    final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
                    final Table contactEntityConversationsTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);
                    final Row entityConversationRow = new Row(entityConversationTable);
                    final Row contactEntityConversationRow = new Row(contactEntityConversationsTable);
                    final Row callConversationRow = new Row(callConversationTable);
                    final Long entityConversationPKValue = entityConversationRow.getPKValue();

                    entityConversationRow.setOrgId(orgId);
                    entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, EntityConversation.Type.CALL.getId());
                    entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, startTimestamp);
                    entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, userId);

                    callConversationRow.setOrgId(orgId);
                    callConversationRow.set(OMECCALLCONVERSATIONS.CALL_HISTORY_ID, callId);
                    callConversationRow.set(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);

                    contactEntityConversationRow.set(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);
                    contactEntityConversationRow.set(OMECCONTACTCONVERSATIONS.CONTACT_ID, contactId);

                    targetContainer.addNewRow(contactEntityConversationRow);
                    targetContainer.addNewRow(callConversationRow);
                    targetContainer.addNewRow(entityConversationRow);

                    final Company contactCompany = contact.getCompany();
                    final Long companyId = Objects.isNull(contactCompany) ? contactManager.getRelatedCompanyId(orgId, contactId).orElse(null) : contactCompany.id();

                    if (Objects.nonNull(companyId)) {
                        loadCompanyConversation(orgId, companyId, entityConversationPKValue, callId, targetContainer);
                    }
                }
            }

            if (Objects.nonNull(deal)) {
                final Long dealId = deal.id();

                if (!isDealLinkedWithCall(orgId, dealId, callId)) {
                    final Table callConversationTable = getCallConversationTable();
                    final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
                    final Table dealEntityConversationsTable = Table.get(OMECDEALCONVERSATIONS.TABLE);
                    final Row entityConversationRow = new Row(entityConversationTable);
                    final Row dealEntityConversationRow = new Row(dealEntityConversationsTable);
                    final Row callConversationRow = new Row(callConversationTable);
                    final Long entityConversationPKValue = entityConversationRow.getPKValue();

                    entityConversationRow.setOrgId(orgId);
                    entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, EntityConversation.Type.CALL.getId());
                    entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, startTimestamp);
                    entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, callDetails.getParticipantUser().id());

                    callConversationRow.setOrgId(orgId);
                    callConversationRow.set(OMECCALLCONVERSATIONS.CALL_HISTORY_ID, callId);
                    callConversationRow.set(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);

                    dealEntityConversationRow.set(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);
                    dealEntityConversationRow.set(OMECDEALCONVERSATIONS.DEAL_ID, dealId);

                    targetContainer.addNewRow(dealEntityConversationRow);
                    targetContainer.addNewRow(callConversationRow);
                    targetContainer.addNewRow(entityConversationRow);
                    final Long primaryContactId = dealManager.getPrimaryContactId(orgId, dealId);

                    if (Objects.nonNull(primaryContactId)) {
                        contactManager.getRelatedCompanyId(orgId, primaryContactId).ifPresent(companyId ->
                                loadCompanyConversation(orgId, companyId, entityConversationPKValue, callId, targetContainer));
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Fetched the conversation id for the given contact and call history id if it exists
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param contactId            Represents the company id
     * @param callId               Represents the call id
     */
    private Collection<Long> getEntityConversationIdForContact(final Long orgId, final Long contactId, final Long callId) {
        final Table contactEntityConversationsTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);
        final Column entityConversationPKColumn = Table.get(OMECENTITYCONVERSATIONS.TABLE).getPKColumn();
        final Table callConversationTable = getCallConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(contactEntityConversationsTable);

        selectQuery.addJoinClause(List.of(new JoinClause(contactEntityConversationsTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPKColumn),
                new JoinClause(entityConversationPKColumn, callConversationTable.getColumn(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactEntityConversationsTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID), contactId))
                .and(Criteria.eq(callConversationTable.getColumn(OMECCALLCONVERSATIONS.CALL_HISTORY_ID), callId)));

        return orgDataStore(orgId).get(selectQuery).getRows(contactEntityConversationsTable)
                .map(row -> (Long) row.get(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID))
                .toList();
    }

    /**
     * <p>
     * Loads and creates company conversation mappings for a given call if they do not already exist
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param companyId            Represents the company id
     * @param entityConversationId Represents the entity conversation id
     * @param callId               Represents the call id
     * @param targetContainer      The {@link DataContainer} into which the data should be loaded
     */
    private void loadCompanyConversation(final long orgId, final long companyId, final Long entityConversationId,
                                         final long callId, final DataContainer targetContainer) {
        if (isCompanyLinkedWithCall(orgId, companyId, callId) || isCompanyConversationAlreadyPresent(companyId, callId, targetContainer)) {
            return;
        }
        final Table companyEntityConversationsTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
        final Row companyEntityConversationRow = new Row(companyEntityConversationsTable);

        companyEntityConversationRow.set(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationId);
        companyEntityConversationRow.set(OMECCOMPANYCONVERSATIONS.COMPANY_ID, companyId);

        targetContainer.addNewRow(companyEntityConversationRow);
    }

    /**
     * <p>
     * Checks whether a company conversation mapping already exists for the given
     * company id and call id within the provided data container.
     * </p>
     *
     * @param companyId       Represents the company id
     * @param callId          Represents the call id
     * @param targetContainer The {@link DataContainer} into which the data should be loaded
     * @return true if a company conversation mapping already exists for the given
     * company and call, otherwise false
     */
    private boolean isCompanyConversationAlreadyPresent(final long companyId, final long callId, final DataContainer targetContainer) {
        final Table callConversationTable = getCallConversationTable();
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table companyConversationTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);

        return targetContainer.getRows(callConversationTable,
                        new WhereClause(Criteria.eq(callConversationTable.getColumn(OMECCALLCONVERSATIONS.CALL_HISTORY_ID), callId)))
                .anyMatch(callConversationRow -> {
                    final Row entityConversationRow = targetContainer.getRow(entityConversationTable,
                            (Long) callConversationRow.get(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID));

                    if (Objects.isNull(entityConversationRow)) {
                        return false;
                    }
                    final Row companyConversationRow = targetContainer.getRow(companyConversationTable,
                            new WhereClause(Criteria.eq(companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationRow.getPKValue()))
                                    .and(Criteria.eq(companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID), companyId)));

                    return Objects.nonNull(companyConversationRow);
                });
    }

    /**
     * <p>
     * Checks whether a specific contact is associated with a given call history entry within the context of an organization
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @param callId    Represents the call id
     * @return true if the contact is linked to the call history; false otherwise
     */
    private boolean isContactLinkedWithCall(final long orgId, final long contactId, final long callId) {
        final Table contactEntityConversationsTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);
        final Column entityConversationPKColumn = Table.get(OMECENTITYCONVERSATIONS.TABLE).getPKColumn();
        final Table callConversationTable = getCallConversationTable();
        final CountQuery countQuery = CountQuery.get(contactEntityConversationsTable);

        countQuery.addJoinClause(List.of(new JoinClause(contactEntityConversationsTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPKColumn),
                new JoinClause(entityConversationPKColumn, callConversationTable.getColumn(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID))));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactEntityConversationsTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID), contactId))
                .and(Criteria.eq(callConversationTable.getColumn(OMECCALLCONVERSATIONS.CALL_HISTORY_ID), callId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Checks whether a specific deal is associated with a given call history entry within the context of an organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @param callId Represents the call  id
     * @return true if the contact is linked to the call history; false otherwise
     */
    private boolean isDealLinkedWithCall(final long orgId, final long dealId, final long callId) {
        final Table dealEntityConversationsTable = Table.get(OMECDEALCONVERSATIONS.TABLE);
        final Column entityConversationPKColumn = Table.get(OMECENTITYCONVERSATIONS.TABLE).getPKColumn();
        final Table callConversationTable = getCallConversationTable();
        final CountQuery countQuery = CountQuery.get(dealEntityConversationsTable);

        countQuery.addJoinClause(List.of(new JoinClause(dealEntityConversationsTable.getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPKColumn),
                new JoinClause(entityConversationPKColumn, callConversationTable.getColumn(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID))));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealEntityConversationsTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID), dealId))
                .and(Criteria.eq(callConversationTable.getColumn(OMECCALLCONVERSATIONS.CALL_HISTORY_ID), callId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Checks whether a specific company is associated with a given call history entry within the context of an organization
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param companyId Represents the company id
     * @param callId    Represents the call id
     * @return true if the contact is linked to the call history; false otherwise
     */
    private boolean isCompanyLinkedWithCall(final long orgId, final long companyId, final long callId) {
        final Table companyEntityConversationsTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
        final Column entityConversationPKColumn = Table.get(OMECENTITYCONVERSATIONS.TABLE).getPKColumn();
        final Table callConversationTable = getCallConversationTable();
        final CountQuery countQuery = CountQuery.get(companyEntityConversationsTable);

        countQuery.addJoinClause(List.of(new JoinClause(companyEntityConversationsTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPKColumn),
                new JoinClause(entityConversationPKColumn, callConversationTable.getColumn(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID))));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(companyEntityConversationsTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID), companyId))
                .and(Criteria.eq(callConversationTable.getColumn(OMECCALLCONVERSATIONS.CALL_HISTORY_ID), callId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Loads a timeline entry for the given {@link CallDetails} into the provided {@link DataContainer}
     * </p>
     *
     * @param callDetails     Represents the {@link CallDetails}
     * @param targetContainer The data container into which the timeline row(s) should be loaded
     */
    private void loadCallDetailsToTimeline(final CallDetails callDetails, final DataContainer targetContainer) {
        final Contact contact = callDetails.getContact();

        if (Objects.nonNull(contact)) {
            final Long id = contact.id();

            if (Objects.isNull(id)) {
                return;
            }
            final CallStatus status = callDetails.getStatus();

            if (CallStatus.COMPLETED == status || CallStatus.NO_ANSWER == status) {
                final CallType callType = callDetails.getType();
                final Timeline timeline = buildTimeline(callDetails.orgId());

                timeline.setOwnerId(callDetails.getParticipantUser().id());
                timeline.setTimelineType(switch (status) {
                    case NO_ANSWER ->
                            callType == CallType.INCOMING ? TimelineType.MISSED_INCOMING_CALL : TimelineType.UN_ANSWERED_OUTGOING_CALL;
                    case COMPLETED ->
                            callType == CallType.INCOMING ? TimelineType.INCOMING_CALL : TimelineType.OUTGOING_CALL;
                    default -> null;
                });
                commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
                commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), id, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                        new Row(commonTimelineManager.getContactTimelineTable()), targetContainer);
                final TimelineType timelineType = timeline.getTimelineType();

                if (TimelineType.INCOMING_CALL == timelineType || TimelineType.OUTGOING_CALL == timelineType) {
                    final Table callTimelineTable = getCallTimelineTable();
                    final Row callTimelineRow = new Row(callTimelineTable);

                    callTimelineRow.setOrgId(callDetails.orgId());
                    callTimelineRow.set(OMTLCALLTIMELINE.TIMELINE_ID, timeline.id());
                    callTimelineRow.set(OMTLCALLTIMELINE.CALL_HISTORY_ID, callDetails.id());
                    targetContainer.addNewRow(callTimelineRow);
                }
            }
        }
    }

    /**
     * <p>
     * Loads the details of a forwarded call into the timeline for a specific contact within the given organization. This method constructs a {@link Timeline}
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param contactId          Represents the contact id
     * @param callForwardDetails Represents the {@link CallForwardDetails}
     * @param targetContainer    The {@link DataContainer} into which the data should be loaded.
     */
    private void loadForwardDetailsToTimeline(final long orgId, final Long contactId, final CallForwardDetails callForwardDetails,
                                              final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(orgId);
        final Long forwardedById = callForwardDetails.getForwardedBy().id();

        timeline.setOwnerId(forwardedById);
        final JsonObject jsonObject = Json.object()
                .put(TimelineFields.FROM.getName(), forwardedById)
                .put(TimelineFields.TO.getName(), callForwardDetails.getForwardedTo().id());

        timeline.setFieldTimeline(new FieldTimeline(jsonObject.toString()));
        timeline.setTimelineType(TimelineType.TRANSFERRED_CALL);

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
        commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), contactId, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                new Row(commonTimelineManager.getContactTimelineTable()), targetContainer);
    }

    /**
     * <p>
     * Builds the {@link Timeline} for the given org id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link Timeline}
     */
    private Timeline buildTimeline(final long orgId) {
        final Timeline timeline = new Timeline();

        timeline.setOrgId(orgId);
        timeline.setCategory(Timeline.Category.CALLS);
        timeline.setAction(TimelineActionFields.ASSOCIATED);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));

        return timeline;
    }

    /**
     * <p>
     * Builds a {@link DerivedTable} from the specified table with a filtering condition
     * </p>
     *
     * @param table         The source table used to build the derived table
     * @param fieldIdColumn The column representing the field identifier on which the condition is applied
     * @param fieldId       The value used to filter rows of the table by the field ID
     * @param alias         Optional alias for the derived table; if null the table's default alias is used
     * @return A {@link DerivedTable} representing the filtered query of the input table
     */
    private DerivedTable buildDerivedTable(final Table table, final Column fieldIdColumn, final Long fieldId, final String alias) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(fieldIdColumn, fieldId)));

        return DerivedTable.get(table.getName(), Objects.nonNull(alias) ? alias : table.getAlias(), selectQuery);
    }

}