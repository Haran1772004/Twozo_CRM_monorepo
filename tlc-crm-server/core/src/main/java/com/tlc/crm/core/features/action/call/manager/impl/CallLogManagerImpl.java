package com.tlc.crm.core.features.action.call.manager.impl;

import com.google.common.base.Supplier;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.features.action.call.model.Outcome;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.field.call.CallLogFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.OMECCALLLOGCONVERSATIONS;
import com.tlc.crm.core.sql.resource.MNCCALLOUTCOME;
import com.tlc.crm.core.sql.resource.MNCOUTCOME;
import com.tlc.crm.core.sql.resource.MNCDEALCALLLOG;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMNCALLLOG;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.call.CallType;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the call log related DB actions.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallLogManagerImpl extends AbstractConfigManager<CallLog> implements CallLogManager {

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final MonetaryManager monetaryManager;
    private final PipelineStageManager pipelineStageManager;
    private final PhoneManager phoneManager;
    private final UserService userService;
    private final FieldService fieldService;
    private final AdminDataStore adminDataStore;
    private final CommonTimelineManager commonTimelineManager;

    private CallLogManagerImpl() {
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        dealManager = DealManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        userService = UserService.getInstance();
        fieldService = FieldService.getSecureInstance();
        adminDataStore = DataStoreProvider.getAdminDataStore();
        commonTimelineManager = CommonTimelineManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final CallLogManagerImpl CALL_LOG_MANAGER = new CallLogManagerImpl();
    }

    public static CallLogManagerImpl getInstance() {
        return InstanceHolder.CALL_LOG_MANAGER;
    }

    @Override
    public Table getTable() {
        return Table.get(OMNCALLLOG.TABLE);
    }

    private Table getOutcomeTable() {
        return Table.get(MNCOUTCOME.TABLE);
    }

    private Table getCallOutcomeTable() {
        return Table.get(MNCCALLOUTCOME.TABLE);
    }

    private Table getDealCallTable() {
        return Table.get(MNCDEALCALLLOG.TABLE);
    }

    /**
     * <p>
     * Gets the call log conversation table
     * </p>
     *
     * @return The call log conversation table
     */
    private Table getCallLogConversationTable() {
        return Table.get(OMECCALLLOGCONVERSATIONS.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CallLog> get(final Long orgId, final Collection<Long> ids) {
        final Table callLogTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(callLogTable.getPKColumn(), ids));
        final DataContainer dataContainer = fetchCallLogContainer(orgId, whereClause, null);

        return dataContainer.getRows(callLogTable).map(callDetailsRow -> convertRowToCallLog(callDetailsRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CallLog> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table callLogTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(callLogTable,
                new WhereClause(Criteria.in(callLogTable.getPKColumn(), ids)));

        return dataContainer.getRows(callLogTable).map(callDetailsRow -> convertRowToCallLog(callDetailsRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallLog create(final CallLog callLog) {
        final DataContainer targetContainer = DataContainer.create();

        loadCallLogIntoContainer(true, callLog, null, targetContainer);
        orgDataStore(callLog.orgId()).commitChanges(targetContainer);
        return callLog;
    }

    /**
     * <p>
     * Updates the rows that are matches the given records
     * Failure of one record doesn't impact other records from update operation
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param callLogs Holds the details of the call log.
     * @return Collection of updated call logs.
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<CallLog> callLogs) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = callLogs.stream().map(CallLog::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = fetchCallLogContainer(orgId, new WhereClause(Criteria.in(getTable().getPKColumn(), ids)), null);

        callLogs.forEach(callLog -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadCallLogIntoContainer(false, callLog, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(callLog.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Contact update failed", errorCode);
                failedEntities.add(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_FAILED, callLog.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 The organization ID for which the call log is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder The {@link CriteriaDetails} defining filtering conditions.
     * @param limitConfig           The {@link LimitConfig} defining pagination limits.
     * @return The {@link CallLog} containing the list of call records.
     */
    @Override
    public Collection<CallLog> get(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder,
                                   final LimitConfig limitConfig) {
        final Table callLogTable = getTable();
        final Optional<WhereClause> optionalSearchCriteria = buildSearchCriteria(searchText);
        final Optional<WhereClause> filterCriteria = criteriaDetailsHolder.getCriteria(this::getColumn);
        final WhereClause whereClause = filterCriteria.map(criteria ->
                optionalSearchCriteria.map(criteria::and).orElse(criteria)).orElseGet(() -> null);
        final DataContainer dataContainer = fetchCallLogContainer(orgId, whereClause, new LimitClause(limitConfig.start(), limitConfig.limit()));
        final Stream<Row> callDetailsRows = dataContainer.getRows(callLogTable);

        return callDetailsRows.map(callDetailsRow -> convertRowToCallLog(callDetailsRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 The organization ID for which the call log is retrieved.
     * @param searchText            The search text to filter call records.
     * @param criteriaDetailsHolder The {@link CriteriaDetails} defining filtering conditions.
     * @return The count of the {@link CallLog}.
     */
    @Override
    public long getTotalCount(final long orgId, final String searchText, final CriteriaDetailsHolder criteriaDetailsHolder) {
        final Table callLogTable = getTable();
        final Table contactTable = contactManager.getTable();
        final Optional<WhereClause> optionalSearchCriteria = buildSearchCriteria(searchText);
        final Optional<WhereClause> filterCriteria = criteriaDetailsHolder.getCriteria(this::getColumn);
        final WhereClause whereClause = filterCriteria.map(criteria ->
                optionalSearchCriteria.map(criteria::and).orElse(criteria)).or(() -> optionalSearchCriteria).orElseGet(() -> null);
        final CountQuery countQuery = CountQuery.get(callLogTable);

        if (Objects.nonNull(searchText)) {
            countQuery.addJoinClause(new JoinClause(callLogTable.getColumn(OMNCALLLOG.PARTICIPANT_ID), contactTable.getPKColumn()));
        }
        countQuery.setWhereClause(whereClause);
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteria the filtering criteria to apply (e.g., search term, conditions)
     * @param limit    the maximum number of results to return and paging info
     * @return a collection of Outcome entities matching the criteria
     */
    @Override
    public Collection<Outcome> getOutcomes(final DropdownCriteria criteria, final DropdownLimit limit) {
        final Table outcomeTable = getOutcomeTable();
        final SelectQuery selectQuery = SelectQuery.get(outcomeTable);

        selectQuery.setWhereClause(new WhereClause(criteria.build(outcomeTable.getColumn(MNCOUTCOME.NAME))));
        selectQuery.addOrderByClause(new OrderByClause(outcomeTable.getPKColumn()));
        selectQuery.setLimitClause(limit.getLimitClause());
        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return dataContainer.getRows(outcomeTable).map(this::convertRowToOutcome).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param criteria filtering conditions to apply (e.g., search terms, constraints)
     * @return the total count of matching outcomes
     */
    @Override
    public long getTotalOutcomesCount(final DropdownCriteria criteria) {
        final Table outcomeTable = getOutcomeTable();
        final CountQuery countQuery = CountQuery.get(outcomeTable);

        countQuery.setWhereClause(new WhereClause(criteria.build(outcomeTable.getColumn(MNCOUTCOME.NAME))));
        return adminDataStore.get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization ID.
     * @param id        Represents the call log ID.
     * @param startTime Represents the start time of the call.
     */
    @Override
    public void updateStartTime(final long orgId, final Long id, final DateTime startTime) {
        final Table callLogTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(callLogTable);

        updateQuery.addUpdateColumn(callLogTable.getColumn(OMNCALLLOG.START_TIME), startTime);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(callLogTable.getPKColumn(), id)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * Loads the given {@link CallLog} data into the target {@link DataContainer}.
     * Creates or updates a row in the call log table depending on whether it's a create or update event.
     *
     * @param isCreateEvent   whether this is a create operation (true) or update (false)
     * @param callLog         the CallLog object containing data to be stored
     * @param sourceContainer the container used for reading existing data
     * @param targetContainer the container used for writing updated or new data
     */
    private void loadCallLogIntoContainer(final boolean isCreateEvent, final CallLog callLog, final DataContainer sourceContainer,
                                          final DataContainer targetContainer) {
        final Table callLogTable = getTable();
        final Row callLogRow = isCreateEvent ? new Row(callLogTable) : sourceContainer.getRow(callLogTable, callLog.id());
        final Long orgId = callLog.orgId();

        if (Objects.isNull(callLogRow)) {
            throw ErrorCode.get(CoreErrorCodes.CALL_LOG_NOT_FOUND, String.format("No call log details found. Org ID: %s, ID: %s", orgId, callLog.id()));
        }
        final DateTime dateTime = new DateTime(Instant.now().toEpochMilli());

        if (callLogRow.isNewRow()) {
            callLog.setId(callLogRow.getPKValue());
            callLogRow.setOrgId(orgId);
            callLogRow.set(OMNCALLLOG.CREATED_BY, callLog.getCreatedBy().id());
            callLogRow.set(OMNCALLLOG.LOGGED_TIME, dateTime);
        }
        callLogRow.set(OMNCALLLOG.AGENDA, callLog.getAgenda());
        callLogRow.set(OMNCALLLOG.CALL_TYPE, callLog.getCallType().getId());
        callLogRow.set(OMNCALLLOG.START_TIME, callLog.getStartTime());
        callLogRow.set(OMNCALLLOG.UPDATED_TIME, dateTime);
        final Long callLogId = callLog.id();
        final Outcome outcome = callLog.getOutcome();
        final Deal relatedDeal = callLog.getRelatedDeal();

        if (Objects.nonNull(outcome)) {
            loadCallOutcomeIntoContainer(isCreateEvent, callLogId, outcome, sourceContainer, targetContainer);
        }
        final Table dealCallTable = getDealCallTable();

        if (Objects.isNull(relatedDeal)) {
            final Row dealCallLogRow = orgDataStore(orgId).get(dealCallTable, new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLogId))).getRow(dealCallTable);

            if (Objects.nonNull(dealCallLogRow)) {
                deleteCallLogConversations(orgId, dealCallLogRow.get(MNCDEALCALLLOG.DEAL_ID), callLogId, targetContainer);
                targetContainer.deleteRow(dealCallLogRow);
            }
        } else {

            if (hasDifferentDealForCallLog(orgId, callLogId, relatedDeal.id())) {
                final Row existingDealCallLogRow = orgDataStore(orgId).get(dealCallTable,
                        new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLogId))).getRow(dealCallTable);

                if (Objects.nonNull(existingDealCallLogRow)) {
                    deleteCallLogConversations(orgId, existingDealCallLogRow.get(MNCDEALCALLLOG.DEAL_ID), callLogId, targetContainer);
                }
            }
            loadRelatedDealIntoContainer(isCreateEvent, callLogId, relatedDeal, sourceContainer, targetContainer);
        }
        loadTimeline(isCreateEvent, callLogRow, callLog, targetContainer);
        loadCallLogIntoEntityConversations(isCreateEvent, callLogRow, callLog, targetContainer);
        loadDealCallLogIntoEntityConversations(isCreateEvent, callLog, targetContainer);
        callLogRow.set(OMNCALLLOG.PARTICIPANT_ID, callLog.getParticipant().id());
        targetContainer.updateRow(callLogRow);
    }

    /**
     * Loads the related deal for a call log into the target {@link DataContainer}.
     * If an existing row is found for the call log ID, it updates it; otherwise, a new row is created.
     *
     * @param isCreateEvent   whether this is a create operation (true) or update (false)
     * @param callLogId       ID of the associated call log
     * @param relatedDeal     the Deal associated with the call log
     * @param sourceContainer container for reading existing rows
     * @param targetContainer container to which the deal data is written
     */
    private void loadRelatedDealIntoContainer(final boolean isCreateEvent, final Long callLogId, final Deal relatedDeal,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table dealCallTable = getDealCallTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLogId));
        final Supplier<Row> rowCreator = () -> new Row(dealCallTable);
        final Row dealCallRow = isCreateEvent ? rowCreator.get() : Optional.ofNullable(sourceContainer.getRow(dealCallTable, whereClause))
                .orElseGet(rowCreator);

        dealCallRow.set(MNCDEALCALLLOG.CALL_LOG_ID, callLogId);
        dealCallRow.set(MNCDEALCALLLOG.DEAL_ID, relatedDeal.id());
        targetContainer.updateRow(dealCallRow);
    }

    /**
     * Loads the outcome of a call log into the target {@link DataContainer}.
     * Updates existing outcome if found, or inserts a new row linked by call log ID.
     *
     * @param isCreateEvent   whether this is a create operation (true) or update (false)
     * @param callLogId       ID of the associated call log
     * @param outcome         the Outcome object to store
     * @param sourceContainer container for existing data
     * @param targetContainer container where the outcome data will be updated or inserted
     */
    private void loadCallOutcomeIntoContainer(final boolean isCreateEvent, final Long callLogId, final Outcome outcome,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table callOutcomeTable = getCallOutcomeTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(callOutcomeTable.getColumn(MNCCALLOUTCOME.CALL_LOG_ID), callLogId));
        final Supplier<Row> rowCreator = () -> new Row(callOutcomeTable);
        final Row callOutcomeRow = isCreateEvent ? rowCreator.get() : Optional.ofNullable(sourceContainer.getRow(callOutcomeTable, whereClause))
                .orElseGet(rowCreator);

        callOutcomeRow.set(MNCCALLOUTCOME.CALL_LOG_ID, callLogId);
        callOutcomeRow.set(MNCCALLOUTCOME.OUTCOME_ID, outcome.getId());
        targetContainer.updateRow(callOutcomeRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param callLogRow    the row from the call log table
     * @param dataContainer the full container holding all joined tables and related data
     * @return a fully populated {@link CallLog}
     */
    @Override
    public CallLog convertRowToCallLog(final Row callLogRow, final DataContainer dataContainer) {
        final CallLog callLog = new CallLog();
        final Table contactTable = contactManager.getTable();
        final Table callOutcomeTable = getCallOutcomeTable();
        final Table dealCallTable = getDealCallTable();
        final Long callLogId = callLogRow.getPKValue();
        final Long orgId = callLogRow.getOrgId();
        final Long participantId = callLogRow.get(OMNCALLLOG.PARTICIPANT_ID);
        final Row participantRow = Objects.isNull(participantId) ? null : dataContainer.getRow(contactTable, participantId);
        final Row callOutcomeRow = dataContainer.getRow(callOutcomeTable,
                new WhereClause(Criteria.eq(callOutcomeTable.getColumn(MNCCALLOUTCOME.CALL_LOG_ID), callLogId)));
        final Row dealCallRow = dataContainer.getRow(dealCallTable,
                new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLogId)));
        final Long createdBy = callLogRow.get(OMNCALLLOG.CREATED_BY);

        if (Objects.nonNull(createdBy)) {
            callLog.setCreatedBy(userService.getUser(orgId, createdBy));
        }

        callLog.setId(callLogId);
        callLog.setOrgId(orgId);
        callLog.setAgenda(callLogRow.get(OMNCALLLOG.AGENDA));
        callLog.setCallType(CallType.get((short) callLogRow.get(OMNCALLLOG.CALL_TYPE)));
        callLog.setLoggedTime(callLogRow.get(OMNCALLLOG.LOGGED_TIME));
        callLog.setStartTime(callLogRow.get(OMNCALLLOG.START_TIME));
        callLog.setUpdatedTime(callLogRow.get(OMNCALLLOG.UPDATED_TIME));

        if (Objects.nonNull(participantRow)) {
            final Table contactCompanyTable = contactManager.getContactCompanyTable();
            final Table phoneTable = phoneManager.getTable();
            final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable,
                    new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), participantRow.getPKValue())));
            final Row phoneRow = dataContainer.getRow(phoneTable,
                    new WhereClause(Criteria.eq(phoneTable.getPKColumn(), participantRow.get(MCOCONTACT.PRIMARY_PHONE))));
            final Contact contact = contactManager.convertRowToPartialModel(participantRow);

            if (Objects.nonNull(contactCompanyRow)) {
                final Row companyRow = dataContainer.getRow(companyManager.getTable(), (long) contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID));

                contact.setCompany(companyManager.convertRowToPartialModel(companyRow));
            }

            if (Objects.nonNull(phoneRow)) {
                contact.setPrimaryPhone(phoneManager.convertRowToModel(phoneRow));
            }
            callLog.setParticipant(contact);
        }

        if (Objects.nonNull(callOutcomeRow)) {
            final Table outcomeTable = getOutcomeTable();
            final Row outcomeRow = dataContainer.getRow(outcomeTable, (long) callOutcomeRow.get(MNCCALLOUTCOME.OUTCOME_ID));

            callLog.setOutcome(convertRowToOutcome(outcomeRow));
        }

        if (Objects.nonNull(dealCallRow)) {
            final Row dealRow = dataContainer.getRow(dealManager.getTable(), (long) dealCallRow.get(MNCDEALCALLLOG.DEAL_ID));

            if (Objects.nonNull(dealRow)) {
                final Deal deal = dealManager.convertRowToPartialModel(dealRow);
                final Long monetaryId = dealRow.get(MDODEAL.MONETARY_ID);
                final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
                final Short statusId = dealRow.get(MDODEAL.STATUS);

                if (Objects.nonNull(monetaryId)) {
                    final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), monetaryId);

                    deal.setDealValue(new DealValue(monetaryManager.convertRowToModel(monetaryRow)));
                }

                if (Objects.nonNull(pipelineStageId)) {
                    final Row pipelineStageRow = dataContainer.getRow(pipelineStageManager.getTable(), pipelineStageId);

                    deal.setPipelineStage(pipelineStageManager.convertPartialRowToModel(pipelineStageRow));
                }

                if (Objects.nonNull(statusId)) {
                    deal.setStatus(statusId);
                }
                deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
                callLog.setRelatedDeal(deal);
            }
        }
        return callLog;
    }

    /**
     * Converts a row from the outcome table into an {@link Outcome}.
     *
     * @param outcomeRow the row from the outcome table
     * @return a basic {@link Outcome} model populated from the row
     */
    private Outcome convertRowToOutcome(final Row outcomeRow) {
        final Outcome outcome = new Outcome();

        outcome.setId(outcomeRow.getPKValue());
        outcome.setName(outcomeRow.get(MNCOUTCOME.NAME));
        return outcome;
    }

    /**
     * Fetches call log data and associated records (participant, outcome, deal) using the provided filters.
     * Constructs a {@link DataContainer} using joins across related tables.
     *
     * @param orgId       the organization ID used to fetch scoped data
     * @param whereClause filter criteria to limit the results
     * @param limitClause limits the number of records fetched
     * @return a DataContainer with call log data and related entities for the given filters
     */
    private DataContainer fetchCallLogContainer(final Long orgId, final WhereClause whereClause, final LimitClause limitClause) {
        final Table callLogTable = getTable();
        final Table outcomeTable = getOutcomeTable();
        final Table callOutcomeTable = getCallOutcomeTable();
        final Table dealCallTable = getDealCallTable();
        final Column callLogPkColumn = callLogTable.getPKColumn();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(callLogTable);

        selectQuery.addSelectClause(callLogTable, outcomeTable, callOutcomeTable, dealCallTable);
        selectQuery.addJoinClause(new JoinClause(callLogPkColumn, callOutcomeTable.getColumn(MNCCALLOUTCOME.CALL_LOG_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(callOutcomeTable.getColumn(MNCCALLOUTCOME.OUTCOME_ID), outcomeTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(callLogPkColumn, dealCallTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(new OrderByClause(callLogTable.getColumn(OMNCALLLOG.LOGGED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(limitClause);
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final Set<Long> contactIds = dataContainer.getRows(callLogTable)
                .map(callLogRow -> (Long) callLogRow.get(OMNCALLLOG.PARTICIPANT_ID)).collect(Collectors.toSet());

        if (!contactIds.isEmpty()) {
            final Table contactTable = contactManager.getTable();
            final Table phoneTable = phoneManager.getTable();
            final Column contactPkColumn = contactTable.getPKColumn();
            final SelectQuery contactCompanyQuery = SelectQuery.get(contactTable);
            final Optional<FieldInfo> optionalCompanyField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, CallLogFields.COMPANY.getName());
            final WhereClause contactCompanyCriteria = new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds));

            contactCompanyQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.FIRST_NAME));
            contactCompanyQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.LAST_NAME));
            contactCompanyQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE));
            contactCompanyQuery.addSelectClause(phoneTable);

            if (optionalCompanyField.isPresent()) {
                final Table companyTable = companyManager.getTable();
                final Table contactCompanyTable = contactManager.getContactCompanyTable();
                final SelectQuery contactCompanyDerivedQuery = SelectQuery.get(contactCompanyTable);

                contactCompanyDerivedQuery.addSelectClause(contactCompanyTable);
                contactCompanyDerivedQuery.setWhereClause(new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), optionalCompanyField.get().id())));
                final DerivedTable derivedContactCompanyTable = DerivedTable.get(contactCompanyTable.getName(), contactCompanyTable.getAlias(), contactCompanyDerivedQuery);

                contactCompanyQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
                contactCompanyQuery.addSelectClause(derivedContactCompanyTable);
                contactCompanyQuery.addJoinClause(new JoinClause(contactPkColumn, derivedContactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT));
                contactCompanyQuery.addJoinClause(new JoinClause(derivedContactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), JoinClause.JoinType.LEFT));
            }
            contactCompanyQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), JoinClause.JoinType.LEFT));
            contactCompanyQuery.setWhereClause(contactCompanyCriteria);
            dataContainer.append(orgDataStore.get(contactCompanyQuery));
        }
        final Set<Long> dealIds = dataContainer.getRows(dealCallTable).map(row -> (Long) row.get(MNCDEALCALLLOG.DEAL_ID)).collect(Collectors.toSet());

        if (!dealIds.isEmpty()) {
            final Table dealTable = dealManager.getTable();
            final Table monetaryTable = monetaryManager.getTable();
            final Table pipelineStageTable = pipelineStageManager.getTable();
            final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
            final Column pipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
            final SelectQuery dealDetailsQuery = SelectQuery.get(dealTable);

            dealDetailsQuery.addSelectClause(monetaryTable);
            dealDetailsQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), monetaryIdColumn,
                    dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), pipelineStageIdColumn, pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME),
                    dealTable.getColumn(MDODEAL.STATUS)));
            dealDetailsQuery.addJoinClause(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()));
            dealDetailsQuery.addJoinClause(new JoinClause(pipelineStageIdColumn, pipelineStageTable.getPKColumn()));
            dealDetailsQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), dealIds)));
            dataContainer.append(orgDataStore.get(dealDetailsQuery));
        }

        return dataContainer;
    }

    /**
     * Resolves a field name string into a corresponding {@link Column} object.
     *
     * @param field the string field name (e.g., "status", "type")
     * @return the corresponding Column for filtering
     */
    private Column getColumn(final String field) {
        final Table callLogTable = getTable();

        return switch (field.toLowerCase()) {
            case "salesowner" -> callLogTable.getColumn(OMNCALLLOG.CREATED_BY);
            case "type" -> callLogTable.getColumn(OMNCALLLOG.CALL_TYPE);
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_FILTER_FIELD);
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

        return Optional.of(new WhereClause(Criteria.contains(contactTable.getColumn(MCOCONTACT.FIRST_NAME), searchText))
                .or(Criteria.contains(contactTable.getColumn(MCOCONTACT.LAST_NAME), searchText)));
    }

    /**
     * <p>
     * Loads the call log into entity conversations
     * </p>
     *
     * @param isCreateEvent   whether this is a create operation (true) or update (false)
     * @param callLogRow      Represents the call log row
     * @param callLog         the CallLog object containing data to be stored
     * @param targetContainer the container used for writing updated or new data
     */
    private void loadCallLogIntoEntityConversations(final boolean isCreateEvent, final Row callLogRow, final CallLog callLog, final DataContainer targetContainer) {
        final Contact contact = callLog.getParticipant();
        final boolean canLoadConversation = isCreateEvent || (Objects.nonNull(contact)
                && !Objects.equals(callLogRow.get(OMNCALLLOG.PARTICIPANT_ID), contact.id()));

        if (canLoadConversation) {
            final Table callLogConversationTable = getCallLogConversationTable();
            final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
            final Table contactEntityConversationsTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);
            final Long orgId = callLog.orgId();
            final Row entityConversationRow = new Row(entityConversationTable);
            final Row callLogConversationRow = new Row(callLogConversationTable);
            final Row contactEntityConversationRow = new Row(contactEntityConversationsTable);
            final Long entityConversationPKValue = entityConversationRow.getPKValue();

            entityConversationRow.setOrgId(orgId);
            entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, EntityConversation.Type.MANUALLY_LOGGED_CALL.getId());
            entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, Instant.now().toEpochMilli());
            entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, LoggedInUserInfo.getUser().id());

            callLogConversationRow.setOrgId(orgId);
            callLogConversationRow.set(OMECCALLLOGCONVERSATIONS.CALL_LOG_ID, callLog.id());
            callLogConversationRow.set(OMECCALLLOGCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);

            contactEntityConversationRow.set(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);
            contactEntityConversationRow.set(OMECCONTACTCONVERSATIONS.CONTACT_ID, contact.id());

            targetContainer.addNewRow(contactEntityConversationRow);
            targetContainer.addNewRow(callLogConversationRow);
            targetContainer.addNewRow(entityConversationRow);
        }
    }

    /**
     * <p>
     * Loads the deal call log into entity conversations
     * </p>
     *
     * @param isCreateEvent   Whether this is a create operation (true) or update (false)
     * @param callLog         the CallLog object containing data to be stored
     * @param targetContainer the container used for writing updated or new data
     */
    private void loadDealCallLogIntoEntityConversations(final boolean isCreateEvent, final CallLog callLog, final DataContainer targetContainer) {
        final Deal deal = callLog.getRelatedDeal();

        if (Objects.nonNull(deal)) {
            final Long callLogId = callLog.id();
            final Long dealId = deal.id();
            final Long orgId = callLog.orgId();

            if (isCreateEvent || hasDifferentDealForCallLog(orgId, callLogId, dealId)) {
                final Table callLogConversationTable = getCallLogConversationTable();
                final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
                final Table dealEntityConversationsTable = Table.get(OMECDEALCONVERSATIONS.TABLE);
                final Row entityConversationRow = new Row(entityConversationTable);
                final Row callLogConversationRow = new Row(callLogConversationTable);
                final Row dealEntityConversationRow = new Row(dealEntityConversationsTable);
                final Long entityConversationPKValue = entityConversationRow.getPKValue();

                entityConversationRow.setOrgId(orgId);
                entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, EntityConversation.Type.MANUALLY_LOGGED_CALL.getId());
                entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, Instant.now().toEpochMilli());
                entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, LoggedInUserInfo.getUser().id());

                callLogConversationRow.setOrgId(orgId);
                callLogConversationRow.set(OMECCALLLOGCONVERSATIONS.CALL_LOG_ID, callLogId);
                callLogConversationRow.set(OMECCALLLOGCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);

                dealEntityConversationRow.set(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);
                dealEntityConversationRow.set(OMECDEALCONVERSATIONS.DEAL_ID, dealId);

                targetContainer.addNewRow(dealEntityConversationRow);
                targetContainer.addNewRow(callLogConversationRow);
                targetContainer.addNewRow(entityConversationRow);
            }
        }
    }

    /**
     * <p>
     * Loads a timeline entry for the given {@link CallLog} into the provided {@link DataContainer}
     * </p>
     *
     * @param isCreateEvent   Whether this is a create operation (true) or update (false)
     * @param callLogRow      Represents the call {@link Row}
     * @param callLog         Represents the {@link CallLog}
     * @param targetContainer The container used for writing updated or new data
     */
    private void loadTimeline(final boolean isCreateEvent, final Row callLogRow, final CallLog callLog, final DataContainer targetContainer) {
        final Contact contact = callLog.getParticipant();
        final boolean canLoadTimeline = isCreateEvent || (Objects.nonNull(contact)
                && !Objects.equals(callLogRow.get(OMNCALLLOG.PARTICIPANT_ID), contact.id()));

        if (canLoadTimeline) {
            final Timeline timeline = new Timeline();
            final Table contactTimelineTable = commonTimelineManager.getContactTimelineTable();
            final Row contactTimelineRow = new Row(contactTimelineTable);

            timeline.setOrgId(callLog.orgId());
            timeline.setCategory(Timeline.Category.CALLS);
            timeline.setAction(TimelineActionFields.ASSOCIATED);
            timeline.setOwnerId(LoggedInUserInfo.getUser().id());
            timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
            timeline.setTimelineType(TimelineType.CALL_LOG);

            commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
            contactTimelineRow.set(MTLCONTACTTIMELINE.CONTACT_ID, contact.id());
            contactTimelineRow.set(MTLCONTACTTIMELINE.TIMELINE_ID, timeline.id());
            targetContainer.addNewRow(contactTimelineRow);
        }
    }

    /**
     * <p>
     * Checks whether the given call log is associated with a deal other than the specified deal.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param callLogId Represents an call log id
     * @param dealId    Represents the deal id
     * @return true if the call log is linked to a different deal, false otherwise
     */
    private boolean hasDifferentDealForCallLog(final Long orgId, final Long callLogId, final Long dealId) {
        final Table dealCallLogTable = getDealCallTable();
        final CountQuery countQuery = CountQuery.get(dealCallLogTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealCallLogTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLogId))
                .and(Criteria.notEq(dealCallLogTable.getColumn(MNCDEALCALLLOG.DEAL_ID), dealId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Deletes Conversation records associated with the given call log
     * </p>
     *
     * @param orgId           The organization ID
     * @param dealId          Represents the deal id
     * @param callLogId       The call log ID whose conversations need to be removed
     * @param targetContainer The data container used to delete the records
     */
    private void deleteCallLogConversations(final long orgId, final Long dealId, final long callLogId, final DataContainer targetContainer) {
        final Table callLogConversationTable = getCallLogConversationTable();
        final Table dealCallTable = getDealCallTable();
        final SelectQuery selectQuery = SelectQuery.get(dealCallTable);
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table dealEntityConversationsTable = Table.get(OMECDEALCONVERSATIONS.TABLE);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();

        selectQuery.addSelectClause(entityConversationTable, callLogConversationTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(dealCallTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLogConversationTable.getColumn(OMECCALLLOGCONVERSATIONS.CALL_LOG_ID)),
                new JoinClause(callLogConversationTable.getColumn(OMECCALLLOGCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPkColumn),
                new JoinClause(entityConversationPkColumn, dealEntityConversationsTable.getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLogId))
                .and(Criteria.eq(dealEntityConversationsTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID), dealId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row callLogConversationRow = dataContainer.getRow(callLogConversationTable);

        if (Objects.isNull(callLogConversationRow)) {
            return;
        }
        final Long entityConversationId = callLogConversationRow.get(OMECCALLLOGCONVERSATIONS.ENTITY_CONVERSATION_ID);

        if (Objects.nonNull(entityConversationId)) {
            final Row entityConversationRow = dataContainer.getRow(entityConversationTable, entityConversationId);

            if (Objects.nonNull(entityConversationRow)) {
                targetContainer.deleteRow(entityConversationRow);
            }
        }
        targetContainer.deleteRow(callLogConversationRow);
    }
}
