package com.tlc.crm.auto.assignment.api.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.page.Page;

import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;
import com.tlc.scheduler.algorithm.roundrobin.manager.RoundRobinTrackerManager;
import com.tlc.scheduler.algorithm.roundrobin.sql.resource.OMSCHROUNDROBINRULE;
import com.tlc.scheduler.algorithm.roundrobin.sql.resource.OMSCHROUNDROBINTRACKER;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.crm.entity.handler.OrderManager;
import com.tlc.crm.entity.resolver.OrderResolver;

import com.tlc.crm.auto.assignment.api.model.AssignmentModule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteria;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaValue;
import com.tlc.crm.auto.assignment.api.model.CriteriaConnector;
import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.cache.service.AutoAssignmentComparatorService;
import com.tlc.crm.auto.assignment.internal.status.AutoAssignmentErrorCodes;
import com.tlc.crm.auto.assignment.sql.resource.OMASRASSIGNMENTRULE;
import com.tlc.crm.auto.assignment.sql.resource.OMASRCRITERIA;
import com.tlc.crm.auto.assignment.sql.resource.OMASRCRITERIASET;
import com.tlc.crm.auto.assignment.sql.resource.OMASRCRITERIAVALUE;
import com.tlc.crm.auto.assignment.sql.resource.OMASRRULEUSERS;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link AutoAssignmentComparator} related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentManager extends AbstractConfigManager<AutoAssignmentRule> {

    private final AutoAssignmentComparatorService comparatorService;
    private final OrderResolver orderResolver;
    private final OrderManager orderManager;
    private final UserService userService;
    private final TerritoryManager territoryManager;
    private final RoundRobinTrackerManager roundRobinTrackerManager;

    private AutoAssignmentManager() {
        this.comparatorService = AutoAssignmentComparatorService.getInstance();
        this.orderResolver = OrderResolver.getInstance();
        this.orderManager = OrderManager.getInstance();
        this.userService = UserService.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.roundRobinTrackerManager = RoundRobinTrackerManager.getInstance();
    }

    private static class Instance {
        private static final AutoAssignmentManager ASSIGNMENT_MANAGER = new AutoAssignmentManager();
    }

    /**
     * Gets the {@link AutoAssignmentManager} Instance.
     *
     * @return {@link AutoAssignmentManager}
     */
    public static AutoAssignmentManager getInstance() {
        return Instance.ASSIGNMENT_MANAGER;
    }

    /**
     * Gets the {@link Table} of auto-assignment.
     *
     * @return The auto-assignment table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMASRASSIGNMENTRULE.TABLE);
    }

    /**
     * Gets the {@link Table} of criteria.
     *
     * @return The auto-assignment criteria table.
     */
    private Table getCriteriaTable() {
        return Table.get(OMASRCRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of criteria set.
     *
     * @return The auto-assignment criteria set table.
     */
    private Table getCriteriaSetTable() {
        return Table.get(OMASRCRITERIASET.TABLE);
    }

    /**
     * Gets the {@link Table} of criteria value.
     *
     * @return The auto-assignment criteria value table.
     */
    private Table getCriteriaValueTable() {
        return Table.get(OMASRCRITERIAVALUE.TABLE);
    }

    /**
     * Gets the {@link Table} of rule users.
     *
     * @return The rule users table.
     */
    private Table getRuleUsersTable() {
        return Table.get(OMASRRULEUSERS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Refers the organization id.
     * @param ids   Refers the auto-assignment ids.
     * @return The collection of {@link AutoAssignmentRule}.
     */
    @Override
    public Collection<AutoAssignmentRule> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(getAutoAssignmentJoinClauses());
        selectQuery.addSelectClause(table, getCriteriaTable(), getCriteriaSetTable(), getCriteriaValueTable(), getRuleUsersTable());
        selectQuery.addSelectClause(List.of(territoryTable.getPKColumn(), territoryTable.getColumn(OMCRMTERRITORY.NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptySet();
        } else {
            final Collection<Long> userIds = dataContainer.getRows(getRuleUsersTable()).map(row -> (Long) row.get(OMASRRULEUSERS.USER_ID))
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            final Collection<Row> rows = dataContainer.getRows(table).toList();

            rows.forEach(row -> {
                userIds.add(row.get(OMASRASSIGNMENTRULE.CREATED_BY));
                userIds.add(row.get(OMASRASSIGNMENTRULE.UPDATED_BY));
            });
            final Map<Long, CrmUser> crmUsersMap = userService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(TlcModel::id, Function.identity()));
            final Collection<AutoAssignmentRule> autoAssignmentRules = new HashSet<>();

            for (final Row row : rows) {
                autoAssignmentRules.add(convertRowToModel(row, dataContainer, crmUsersMap));
            }
            return autoAssignmentRules;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of ids.
     * @return The collection of {@link AutoAssignmentRule}.
     */
    @Override
    public Collection<AutoAssignmentRule> partialGet(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Refers the organization id.
     * @param autoAssignmentRules Refers the collection of {@link AutoAssignmentRule}.
     * @return
     */
    @Override
    public ProcessOutcome create(final Long orgId, final Collection<AutoAssignmentRule> autoAssignmentRules) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();

        for (final AutoAssignmentRule autoAssignmentRule : autoAssignmentRules) {

            try {
                create(autoAssignmentRule);
                processOutcome.addProcessedRecord(autoAssignmentRule.id());
            } catch (ErrorCode errorCode) {
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed")));
            }
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed")), failedEntries));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param autoAssignmentRule Refers the {@link AutoAssignmentRule}.
     * @return The auto-assignment.
     */
    @Override
    public AutoAssignmentRule create(final AutoAssignmentRule autoAssignmentRule) {
        final DataContainer dataContainer = DataContainer.create();

        loadAutoAssignmentIntoContainer(true, autoAssignmentRule, dataContainer);
        orgDataStore(autoAssignmentRule.orgId()).commitChanges(dataContainer);

        return autoAssignmentRule;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represents the organisation id.
     * @param autoAssignmentRules Holds the details of the {@link AutoAssignmentRule}.
     * @return The response of update action.
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<AutoAssignmentRule> autoAssignmentRules) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> ids = autoAssignmentRules.stream().map(AutoAssignmentRule::id).filter(Objects::nonNull).toList();
        final DataContainer entityContainer = getDataContainer(orgId, ids);
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();

        autoAssignmentRules.forEach(autoAssignment -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadAutoAssignmentIntoContainer(false, autoAssignment, entityContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(autoAssignment.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Auto assignment update failed Org ID : {} ID : {} Reason : {}", orgId, autoAssignment.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, autoAssignment.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * Get the count of auto assignments for the given organization id.
     *
     * @param orgId The organization id.
     * @return The total number of auto assignments.
     */
    public long getAutoAssignmentCount(long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * Get a page of auto assignments for the specified organization.
     *
     * @param orgId The organization id.
     * @param start The starting index for pagination.
     * @param limit The maximum number of records to retrieve.
     * @return A Page of AutoAssignment objects.
     */
    public Page<AutoAssignmentRule> getAutoAssignments(final long orgId, final int start, final int limit) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column orderColumn = table.getColumn(OMASRASSIGNMENTRULE.ORDER);

        selectQuery.addSelectClause(List.of(table.getPKColumn(), table.getColumn(OMASRASSIGNMENTRULE.NAME),
                table.getColumn(OMASRASSIGNMENTRULE.DESCRIPTION), table.getColumn(OMASRASSIGNMENTRULE.ORDER),
                table.getColumn(OMASRASSIGNMENTRULE.MODULE), table.getColumn(OMASRASSIGNMENTRULE.UPDATED_BY),
                table.getColumn(OMASRASSIGNMENTRULE.MODIFIED_TIME), table.getColumn(OMASRASSIGNMENTRULE.IS_ACTIVE)));

        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(orderColumn));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Page<AutoAssignmentRule> assignmentPage = new Page<>();
        final Collection<Long> userIds = new HashSet<>();

        dataContainer.getRows(table).forEach(row -> {
            userIds.add(row.get(OMASRASSIGNMENTRULE.UPDATED_BY));
        });
        final Map<Long, CrmUser> crmUsers = userService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(TlcModel::id, Function.identity()));

        assignmentPage.setData(dataContainer.getRows(table).map(row -> {
            final AutoAssignmentRule autoAssignmentRule = new AutoAssignmentRule();

            autoAssignmentRule.setId(row.getPKValue());
            autoAssignmentRule.setActive(row.get(OMASRASSIGNMENTRULE.IS_ACTIVE));
            autoAssignmentRule.setName(row.get(OMASRASSIGNMENTRULE.NAME));
            autoAssignmentRule.setDescription(row.get(OMASRASSIGNMENTRULE.DESCRIPTION));
            autoAssignmentRule.setOrder(row.get(OMASRASSIGNMENTRULE.ORDER));
            autoAssignmentRule.setModifiedTime(row.get(OMASRASSIGNMENTRULE.MODIFIED_TIME));
            autoAssignmentRule.setUpdatedBy(crmUsers.get((Long) row.get(OMASRASSIGNMENTRULE.UPDATED_BY)));
            autoAssignmentRule.setAssignmentModule(AssignmentModule.get(row.get(OMASRASSIGNMENTRULE.MODULE)));

            return autoAssignmentRule;
        }).toList());

        return assignmentPage;
    }

    /**
     * Checks whether all the given assignments are exists or not.
     *
     * @param orgId         Represents the organisation ID.
     * @param assignmentIds Which contains the collection of assignment IDs.
     * @return true if all the deals are exists
     */
    public boolean exists(final long orgId, final Set<Long> assignmentIds) {
        if (!assignmentIds.isEmpty()) {
            final Table table = getTable();
            final CountQuery countQuery = CountQuery.get(table);

            countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), assignmentIds)));
            return assignmentIds.size() == orgDataStore(orgId).get(countQuery);
        }
        return false;
    }

    /**
     * Checks whether the auto-assignment exists or not.
     *
     * @param orgId            Represents the organization id.
     * @param assignmentModule Represents the {@link AssignmentModule}.
     * @return true if the exists, false otherwise.
     */
    public boolean exists(final long orgId, final AssignmentModule assignmentModule) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMASRASSIGNMENTRULE.MODULE), assignmentModule.getId()))
                .and(Criteria.eq(table.getColumn(OMASRASSIGNMENTRULE.IS_ACTIVE), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Gets the auto-assignment rules by given module.
     *
     * @param orgId            Represents the organization id.
     * @param assignmentModule Represents the {@link AssignmentModule}.
     * @return The collection of {@code AssignmentModule}.
     */
    public Collection<AutoAssignmentRule> getByAssignmentModule(final long orgId, final AssignmentModule assignmentModule) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Table territoryTable = territoryManager.getTable();

        selectQuery.addJoinClause(getAutoAssignmentJoinClauses());
        selectQuery.addSelectClause(table, getCriteriaTable(), getCriteriaSetTable(), getCriteriaValueTable(), getRuleUsersTable());
        selectQuery.addSelectClause(List.of(territoryTable.getPKColumn(), territoryTable.getColumn(OMCRMTERRITORY.NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMASRASSIGNMENTRULE.MODULE), assignmentModule.getId()))
                .and(Criteria.eq(table.getColumn(OMASRASSIGNMENTRULE.IS_ACTIVE), true)));
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMASRASSIGNMENTRULE.ORDER), OrderByClause.OrderType.ASCENDING));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptySet();
        } else {
            final Collection<Long> userIds = dataContainer.getRows(getRuleUsersTable()).map(row -> (Long) row.get(OMASRRULEUSERS.USER_ID))
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            final Collection<Row> rows = dataContainer.getRows(table).toList();

            rows.forEach(row -> {
                userIds.add(row.get(OMASRASSIGNMENTRULE.CREATED_BY));
                userIds.add(row.get(OMASRASSIGNMENTRULE.UPDATED_BY));
            });
            final Map<Long, CrmUser> crmUsersMap = userService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(TlcModel::id, Function.identity()));
            final Collection<AutoAssignmentRule> autoAssignmentRules = new HashSet<>();

            for (final Row row : rows) {
                autoAssignmentRules.add(convertRowToModel(row, dataContainer, crmUsersMap));
            }
            return autoAssignmentRules;
        }
    }

    /**
     * Update the order of auto assignments for the specified organization.
     *
     * @param orgId         The organization id.
     * @param assignmentIds The set of assignment ids to update.
     */
    public void updateOrder(final long orgId, final Set<Long> assignmentIds) {
        orderManager.update(orgId, new Counter(), assignmentIds, getTable());
    }

    /**
     * Updates the status of the {@link AutoAssignmentRule}.
     *
     * @param orgId    Refers the organization id.
     * @param id       Refers the auto-assignment id.
     * @param isActive Refers the auto-assignment status to be changed.
     */
    public void updateStatus(final long orgId, final long id, final boolean isActive) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMASRASSIGNMENTRULE.IS_ACTIVE), isActive);
        updateQuery.addUpdateColumn(table.getColumn(OMASRASSIGNMENTRULE.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * Fetches the {@link DataContainer} of auto-assignment for the given ids.
     *
     * @param orgId         Refers the organization id.
     * @param assignmentIds Refers the auto-assignment ids.
     * @return The container filled with the auto-assignment details.
     */
    private DataContainer getDataContainer(final long orgId, final Collection<Long> assignmentIds) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Table ruleTable = roundRobinTrackerManager.getRuleTable();
        final Table ruleTrackerTable = roundRobinTrackerManager.getTable();
        final JoinClause.JoinType joinType = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(table, getCriteriaTable(), getCriteriaSetTable(), getCriteriaValueTable(), getRuleUsersTable(), ruleTrackerTable);
        selectQuery.addJoinClause(getAutoAssignmentJoinClauses());
        selectQuery.addJoinClause(List.of(new JoinClause(getTable().getPKColumn(), ruleTable.getColumn(OMSCHROUNDROBINRULE.RULE_ENTITY_ID), joinType),
                new JoinClause(ruleTable.getPKColumn(), ruleTrackerTable.getColumn(OMSCHROUNDROBINTRACKER.RULE_ID), joinType)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), assignmentIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Gets the required collection of {@link JoinClause} of auto-assignment.
     *
     * @return The {@link List} of auto-assignment {@link JoinClause}.
     */
    private List<JoinClause> getAutoAssignmentJoinClauses() {
        final Table criteriaTable = getCriteriaTable();
        final Table ruleTable = getRuleUsersTable();
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
        final Column assignmentPkColumn = getTable().getPKColumn();
        final Column criteriaPkColumn = criteriaTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        return List.of(new JoinClause(assignmentPkColumn, ruleTable.getColumn(OMASRRULEUSERS.ASSIGNMENT_RULE_ID)),
                new JoinClause(assignmentPkColumn, getCriteriaSetTable().getColumn(OMASRCRITERIASET.ASSIGNMENT_RULE_ID), leftJoin),
                new JoinClause(assignmentPkColumn, criteriaTable.getColumn(OMASRCRITERIA.ASSIGNMENT_RULE_ID), leftJoin),
                new JoinClause(criteriaPkColumn, getCriteriaValueTable().getColumn(OMASRCRITERIAVALUE.CRITERIA_ID), leftJoin),
                new JoinClause(ruleTable.getColumn(OMASRRULEUSERS.TERRITORY_ID), territoryTable.getPKColumn(), leftJoin));
    }

    /**
     * Loads the auto-assignment details into the data container.
     *
     * @param isCreateEvent      A flag which indicates whether the action is related to create or update event.
     * @param autoAssignmentRule Refers the {@link AutoAssignmentRule} details.
     * @param dataContainer      Holds the updated information of auto-assignment values.
     */
    private void loadAutoAssignmentIntoContainer(final boolean isCreateEvent, final AutoAssignmentRule autoAssignmentRule,
                                                 final DataContainer dataContainer) {
        final DataContainer existingAssignmentContainer = isCreateEvent ? null : getDataContainer(autoAssignmentRule.orgId(), List.of(autoAssignmentRule.id()));

        loadAutoAssignmentIntoContainer(isCreateEvent, autoAssignmentRule, existingAssignmentContainer, dataContainer);
    }

    /**
     * Loads the auto-assignment details into the data container.
     *
     * @param isCreateEvent               A flag which indicates whether the action is related to create or update event.
     * @param autoAssignmentRule          Refers the {@code AutoAssignment}.
     * @param existingAssignmentContainer Holds the information of existing auto-assignment attributes.
     * @param dataContainer               Holds the updated information of auto-assignment values.
     */
    private void loadAutoAssignmentIntoContainer(final boolean isCreateEvent, final AutoAssignmentRule autoAssignmentRule,
                                                 final DataContainer existingAssignmentContainer, final DataContainer dataContainer) {
        final Table table = getTable();
        final Long id = autoAssignmentRule.id();
        final Long orgId = autoAssignmentRule.orgId();
        final Row assignmentRow = isCreateEvent ? new Row(table) : existingAssignmentContainer.getRow(table, id);
        final long currentTime = Instant.now().toEpochMilli();

        if (!isCreateEvent && Objects.isNull(assignmentRow)) {
            throw ErrorCode.get(AutoAssignmentErrorCodes.AUTO_ASSIGNMENT_NOT_FOUND, "i18n.auto.assignment.not.found");
        }

        if (assignmentRow.isNewRow()) {
            assignmentRow.setOrgId(orgId);
            autoAssignmentRule.setId(assignmentRow.getPKValue());
            assignmentRow.set(OMASRASSIGNMENTRULE.CREATED_BY, autoAssignmentRule.getCreatedBy().id());
            assignmentRow.set(OMASRASSIGNMENTRULE.CREATED_TIME, currentTime);
            assignmentRow.set(OMASRASSIGNMENTRULE.ORDER, orderResolver.getNextOrder(orgId, null, table));
        }
        loadCriteriaSetsIntoContainer(isCreateEvent, autoAssignmentRule, existingAssignmentContainer, dataContainer);
        loadRuleIntoContainer(orgId, isCreateEvent, autoAssignmentRule, existingAssignmentContainer, dataContainer);

        assignmentRow.set(OMASRASSIGNMENTRULE.UPDATED_BY, autoAssignmentRule.getUpdatedBy().id());
        assignmentRow.set(OMASRASSIGNMENTRULE.NAME, autoAssignmentRule.getName());
        assignmentRow.set(OMASRASSIGNMENTRULE.DESCRIPTION, autoAssignmentRule.getDescription());
        assignmentRow.set(OMASRASSIGNMENTRULE.IS_ACTIVE, autoAssignmentRule.isActive());
        assignmentRow.set(OMASRASSIGNMENTRULE.TEMPLATE_ID, autoAssignmentRule.getTemplateId());
        assignmentRow.set(OMASRASSIGNMENTRULE.MODULE, autoAssignmentRule.getAssignmentModule().getId());
        assignmentRow.set(OMASRASSIGNMENTRULE.MODIFIED_TIME, currentTime);

        dataContainer.updateRow(assignmentRow);

        if (isCreateEvent) {
            roundRobinTrackerManager.loadTrackerRowIntoContainer(orgId, AutoAssignmentAttributes.AUTO_ASSIGNMENT,
                    assignmentRow.getPKValue(), autoAssignmentRule.getAssignmentRuleUsers().size(), dataContainer);
        } else {
            deleteUnmappedCriteriaSet(autoAssignmentRule, existingAssignmentContainer, dataContainer);
        }
    }

    /**
     * Deletes the unmapped {@link AutoAssignmentCriteriaSet} of auto-assignment.
     *
     * @param autoAssignmentRule          Holds the attributes of {@link AutoAssignmentRule}.
     * @param existingAssignmentContainer Holds the information of existing auto-assignment attributes.
     * @param dataContainer               Holds the updated information of auto-assignment values.
     */
    private void deleteUnmappedCriteriaSet(final AutoAssignmentRule autoAssignmentRule, final DataContainer existingAssignmentContainer,
                                           final DataContainer dataContainer) {
        final Table criteriaSetTable = getCriteriaSetTable();
        final Collection<AutoAssignmentCriteriaSet> criteriaSets = autoAssignmentRule.getCriteriaSets();
        final WhereClause whereClause = new WhereClause(Criteria.eq(criteriaSetTable.getColumn(OMASRCRITERIASET.ASSIGNMENT_RULE_ID), autoAssignmentRule.id()));
        final Stream<Row> criteriaSetRows = existingAssignmentContainer.getRows(criteriaSetTable,
                Objects.nonNull(criteriaSets) && !criteriaSets.isEmpty() ? whereClause
                        .and(Criteria.notIn(criteriaSetTable.getPKColumn(), criteriaSets.stream().map(AutoAssignmentCriteriaSet::getId)
                                .collect(Collectors.toUnmodifiableSet()))) : whereClause);

        criteriaSetRows.forEach(dataContainer::deleteRow);
    }

    /**
     * Loads the {@link AutoAssignmentCriteriaSet} into the data container.
     *
     * @param isCreateEvent               A flag which indicates whether the action is related to create or update event.
     * @param autoAssignmentRule          Refers the {@link AutoAssignmentRule} attributes.
     * @param existingAssignmentContainer Holds the information of existing auto-assignment attributes.
     * @param dataContainer               Holds the updated information of auto-assignment values.
     */
    private void loadCriteriaSetsIntoContainer(final boolean isCreateEvent, final AutoAssignmentRule autoAssignmentRule,
                                               final DataContainer existingAssignmentContainer, final DataContainer dataContainer) {
        final Collection<AutoAssignmentCriteriaSet> criteriaSets = autoAssignmentRule.getCriteriaSets();

        if (Objects.nonNull(criteriaSets)) {
            final Table criteriaSetTable = getCriteriaSetTable();
            final Counter order = new Counter(1);
            final Long assignmentId = autoAssignmentRule.id();

            for (final AutoAssignmentCriteriaSet assignmentCriteriaSet : criteriaSets) {
                final CriteriaConnector connector = assignmentCriteriaSet.getConnector();
                final Long criteriaSetId = assignmentCriteriaSet.getId();
                final Row criteriaSetRow = Objects.isNull(criteriaSetId) ? new Row(criteriaSetTable)
                        : existingAssignmentContainer.getRow(criteriaSetTable, criteriaSetId);

                if (criteriaSetRow.isNewRow()) {
                    assignmentCriteriaSet.setId(criteriaSetRow.getPKValue());
                    criteriaSetRow.setOrgId(autoAssignmentRule.orgId());
                }

                if (Objects.nonNull(connector)) {
                    criteriaSetRow.set(OMASRCRITERIASET.CONNECTOR_ID, connector.getId());
                }
                criteriaSetRow.set(OMASRCRITERIASET.ASSIGNMENT_RULE_ID, assignmentId);
                criteriaSetRow.set(OMASRCRITERIASET.ORDER, order.getAndIncrement());
                dataContainer.updateRow(criteriaSetRow);

                loadCriteriaIntoContainer(isCreateEvent, autoAssignmentRule, assignmentCriteriaSet, existingAssignmentContainer, dataContainer);
            }
        }
    }

    /**
     * Loads the {@link AutoAssignmentCriteria} into the data container.
     *
     * @param isCreateEvent               A flag which indicates whether the action is related to create or update event.
     * @param autoAssignmentRule          Refers the {@link AutoAssignmentRule} attributes.
     * @param assignmentCriteriaSet       Refers the {@link AutoAssignmentCriteriaSet} details.
     * @param existingAssignmentContainer Holds the information of existing auto-assignment attributes.
     * @param dataContainer               Holds the updated information of auto-assignment values.
     */
    private void loadCriteriaIntoContainer(final boolean isCreateEvent, final AutoAssignmentRule autoAssignmentRule, final AutoAssignmentCriteriaSet assignmentCriteriaSet,
                                           final DataContainer existingAssignmentContainer, final DataContainer dataContainer) {
        final Collection<AutoAssignmentCriteria> criteriaList = assignmentCriteriaSet.getCriteriaList();
        final Table criteriaTable = getCriteriaTable();
        final Long assignmentId = autoAssignmentRule.id();
        final Long orgId = autoAssignmentRule.orgId();
        final Counter order = new Counter(1);

        for (final AutoAssignmentCriteria assignmentCriteria : criteriaList) {
            final CriteriaConnector connector = assignmentCriteria.getConnector();
            final Long criteriaId = assignmentCriteria.getId();
            final Row criteriaRow = Objects.isNull(criteriaId) ? new Row(criteriaTable)
                    : existingAssignmentContainer.getRow(criteriaTable, criteriaId);

            if (criteriaRow.isNewRow()) {
                assignmentCriteria.setId(criteriaRow.getPKValue());
                criteriaRow.setOrgId(orgId);
            }

            if (Objects.nonNull(connector)) {
                criteriaRow.set(OMASRCRITERIA.CONNECTOR_ID, connector.getId());
            }

            criteriaRow.set(OMASRCRITERIA.ASSIGNMENT_RULE_ID, assignmentId);
            criteriaRow.set(OMASRCRITERIA.SET_ID, assignmentCriteriaSet.getId());
            criteriaRow.set(OMASRCRITERIA.FIELD_ID, assignmentCriteria.getFieldId());
            criteriaRow.set(OMASRCRITERIA.COMPARATOR_ID, assignmentCriteria.getComparator().getId());
            criteriaRow.set(OMASRCRITERIA.ORDER, order.getAndIncrement());

            dataContainer.updateRow(criteriaRow);
            loadValueIntoContainer(orgId, isCreateEvent, assignmentCriteria, existingAssignmentContainer, dataContainer);
        }

        if (!isCreateEvent) {
            final Stream<Row> criteriaRows = existingAssignmentContainer.getRows(criteriaTable,
                    new WhereClause(Criteria.eq(criteriaTable.getColumn(OMASRCRITERIA.ASSIGNMENT_RULE_ID), assignmentId))
                            .and(Criteria.eq(criteriaTable.getColumn(OMASRCRITERIA.SET_ID), assignmentCriteriaSet.getId()))
                            .and(Criteria.notIn(criteriaTable.getPKColumn(), criteriaList.stream()
                                    .map(AutoAssignmentCriteria::getId).collect(Collectors.toUnmodifiableSet()))));

            criteriaRows.filter(Objects::nonNull).forEach(dataContainer::deleteRow);
        }
    }

    /**
     * Loads the {@link AutoAssignmentCriteriaValue} into the data container.
     *
     * @param isCreateEvent               A flag which indicates whether the action is related to create or update event.
     * @param autoAssignmentCriteria      Refers the {@link AutoAssignmentCriteria} details.
     * @param existingAssignmentContainer Holds the information of existing auto-assignment attributes.
     * @param dataContainer               Holds the updated information of auto-assignment values.
     */
    private void loadValueIntoContainer(final long orgId, final boolean isCreateEvent, final AutoAssignmentCriteria autoAssignmentCriteria,
                                        final DataContainer existingAssignmentContainer, final DataContainer dataContainer) {
        final Collection<AutoAssignmentCriteriaValue> criteriaValues = autoAssignmentCriteria.getCriteriaValues();

        if (Objects.nonNull(criteriaValues)) {
            final Table criteriaValueTable = getCriteriaValueTable();
            final Long criteriaId = autoAssignmentCriteria.getId();

            for (final AutoAssignmentCriteriaValue assignmentCriteriaValue : criteriaValues) {
                final Row criteriaValueRow;

                if (isCreateEvent) {
                    criteriaValueRow = new Row(criteriaValueTable);
                } else {
                    final Row oldCriteriaValueRow = existingAssignmentContainer.getRow(criteriaValueTable,
                            new WhereClause(Criteria.eq(criteriaValueTable.getColumn(OMASRCRITERIAVALUE.CRITERIA_ID), criteriaId))
                                    .and(Criteria.eq(criteriaValueTable.getPKColumn(), assignmentCriteriaValue.getId())));

                    criteriaValueRow = Objects.nonNull(oldCriteriaValueRow) ? oldCriteriaValueRow : new Row(criteriaValueTable);
                }

                if (criteriaValueRow.isNewRow()) {
                    criteriaValueRow.setOrgId(orgId);
                }
                criteriaValueRow.set(OMASRCRITERIAVALUE.CRITERIA_ID, criteriaId);
                criteriaValueRow.set(OMASRCRITERIAVALUE.VALUE, assignmentCriteriaValue.getValue());
                criteriaValueRow.set(OMASRCRITERIAVALUE.VALUE_ENTITY_ID, assignmentCriteriaValue.getValueEntityId());
                assignmentCriteriaValue.setId(criteriaValueRow.getPKValue());

                dataContainer.updateRow(criteriaValueRow);
            }

            if (!isCreateEvent) {
                final Stream<Row> criteriaValueRows = existingAssignmentContainer.getRows(criteriaValueTable,
                        new WhereClause(Criteria.eq(criteriaValueTable.getColumn(OMASRCRITERIAVALUE.CRITERIA_ID), criteriaId))
                                .and(Criteria.notIn(criteriaValueTable.getPKColumn(), criteriaValues.stream()
                                        .map(AutoAssignmentCriteriaValue::getId).collect(Collectors.toUnmodifiableSet()))));

                criteriaValueRows.filter(Objects::nonNull).forEach(dataContainer::deleteRow);
            }
        }
    }

    /**
     * Load the auto assignment rule into the specified data container.
     *
     * @param orgId                       The organization id.
     * @param isCreateEvent               True if the rule is being loaded for a create event.
     * @param autoAssignmentRule          The AutoAssignment object to load.
     * @param existingAssignmentContainer The container holding existing assignments.
     * @param dataContainer               The target container where the rule will be loaded.
     */
    private void loadRuleIntoContainer(final long orgId, final boolean isCreateEvent, final AutoAssignmentRule autoAssignmentRule,
                                       final DataContainer existingAssignmentContainer, final DataContainer dataContainer) {
        final Table ruleTable = getRuleUsersTable();
        final Long assignmentId = autoAssignmentRule.id();
        final Collection<AssignmentRuleUser> assignmentRuleUsers = autoAssignmentRule.getAssignmentRuleUsers();

        for (final AssignmentRuleUser assignmentRuleUser : assignmentRuleUsers) {
            final Territory territory = assignmentRuleUser.getTerritory();
            final boolean hasTerritory = Objects.nonNull(territory);
            final Row ruleAssignmentUserRow;

            if (isCreateEvent) {
                ruleAssignmentUserRow = new Row(ruleTable);
            } else {
                final Row oldRuleUserRow = existingAssignmentContainer.getRow(ruleTable,
                        new WhereClause(Criteria.eq(ruleTable.getColumn(OMASRRULEUSERS.ASSIGNMENT_RULE_ID), assignmentId))
                                .and(Criteria.eq(ruleTable.getColumn(OMASRRULEUSERS.USER_ID), assignmentRuleUser.getUserId())));

                ruleAssignmentUserRow = Objects.nonNull(oldRuleUserRow) ? oldRuleUserRow : new Row(ruleTable);
            }

            if (ruleAssignmentUserRow.isNewRow()) {
                ruleAssignmentUserRow.setOrgId(orgId);
            }
            ruleAssignmentUserRow.set(OMASRRULEUSERS.ASSIGNMENT_RULE_ID, assignmentId);
            ruleAssignmentUserRow.set(OMASRRULEUSERS.TERRITORY_ID, hasTerritory ? territory.id() : null);
            ruleAssignmentUserRow.set(OMASRRULEUSERS.USER_ID, assignmentRuleUser.getUserId());

            dataContainer.updateRow(ruleAssignmentUserRow);
        }

        if (!isCreateEvent) {
            final Stream<Row> ruleAssignmentUserRows = existingAssignmentContainer.getRows(ruleTable,
                    new WhereClause(Criteria.eq(ruleTable.getColumn(OMASRRULEUSERS.ASSIGNMENT_RULE_ID), assignmentId))
                            .and(Criteria.notIn(ruleTable.getColumn(OMASRRULEUSERS.USER_ID), assignmentRuleUsers.stream()
                                    .map(AssignmentRuleUser::getUserId).collect(Collectors.toUnmodifiableSet()))));

            ruleAssignmentUserRows.forEach(dataContainer::deleteRow);

            final Table ruleTrackerTable = roundRobinTrackerManager.getTable();

            if (existingAssignmentContainer.size(ruleTrackerTable) > 0) {
                final Row row = existingAssignmentContainer.getRow(ruleTrackerTable);

                row.set(ruleTrackerTable.getColumn(OMSCHROUNDROBINTRACKER.TOTAL_COUNT), assignmentRuleUsers.size());
                dataContainer.updateRow(row);
            }
        }
    }

    /**
     * Converts the row into the {@link AutoAssignmentRule} attributes.
     *
     * @param row           Which has the auto-assignment details.
     * @param dataContainer Holds the updated information of auto-assignment.
     * @param crmUsers      Refers the grouped users in a map.
     * @return The auto-assignment details.
     */
    private AutoAssignmentRule convertRowToModel(final Row row, final DataContainer dataContainer, final Map<Long, CrmUser> crmUsers) {
        final AutoAssignmentRule autoAssignmentRule = new AutoAssignmentRule();
        final Long assignmentId = row.getPKValue();
        final Long orgId = row.getOrgId();
        final Long createdUserId = row.get(OMASRASSIGNMENTRULE.CREATED_BY);
        final Long updatedUserId = row.get(OMASRASSIGNMENTRULE.UPDATED_BY);

        if (Objects.equals(createdUserId, updatedUserId)) {
            final CrmUser crmUser = crmUsers.get(createdUserId);

            autoAssignmentRule.setCreatedBy(crmUser);
            autoAssignmentRule.setUpdatedBy(crmUser);
        } else {
            autoAssignmentRule.setCreatedBy(crmUsers.get(createdUserId));
            autoAssignmentRule.setUpdatedBy(crmUsers.get(updatedUserId));
        }

        autoAssignmentRule.setId(assignmentId);
        autoAssignmentRule.setOrgId(orgId);
        autoAssignmentRule.setActive(row.get(OMASRASSIGNMENTRULE.IS_ACTIVE));
        autoAssignmentRule.setTemplateId(row.get(OMASRASSIGNMENTRULE.TEMPLATE_ID));
        autoAssignmentRule.setName(row.get(OMASRASSIGNMENTRULE.NAME));
        autoAssignmentRule.setDescription(row.get(OMASRASSIGNMENTRULE.DESCRIPTION));
        autoAssignmentRule.setAssignmentModule(AssignmentModule.get(row.get(OMASRASSIGNMENTRULE.MODULE)));
        autoAssignmentRule.setOrder(row.get(OMASRASSIGNMENTRULE.ORDER));
        autoAssignmentRule.setModifiedTime(row.get(OMASRASSIGNMENTRULE.MODIFIED_TIME));
        autoAssignmentRule.setCreatedTime(row.get(OMASRASSIGNMENTRULE.CREATED_TIME));
        autoAssignmentRule.setCriteriaSets(getCriteriaSets(assignmentId, dataContainer));
        autoAssignmentRule.setAssignmentRuleUsers(getRuleAssignmentUsers(orgId, assignmentId, dataContainer, crmUsers));

        return autoAssignmentRule;
    }

    /**
     * Gets the collection of {@link AutoAssignmentCriteriaSet}.
     *
     * @param assignmentId  Refers the assignment id.
     * @param dataContainer A container that holds the auto-assignment information.
     * @return The collection of {@link AutoAssignmentCriteriaSet}.
     */
    private Collection<AutoAssignmentCriteriaSet> getCriteriaSets(final Long assignmentId, final DataContainer dataContainer) {
        final Table criteriaSetTable = getCriteriaSetTable();
        final Stream<Row> criteriaSetRows = dataContainer.getRows(criteriaSetTable,
                new WhereClause(Criteria.eq(criteriaSetTable.getColumn(OMASRCRITERIASET.ASSIGNMENT_RULE_ID), assignmentId)));

        return criteriaSetRows.map(criteriaSetRow -> convertRowToCriteriaSet(criteriaSetRow, dataContainer)).toList();
    }

    /**
     * Converts the row into {@link AutoAssignmentCriteriaSet}.
     *
     * @param criteriaSetRow Refers the row of the criteria set.
     * @param dataContainer  The container that holds the auto-assignment related information.
     * @return The {@link AutoAssignmentCriteriaSet} details.
     */
    private AutoAssignmentCriteriaSet convertRowToCriteriaSet(final Row criteriaSetRow, final DataContainer dataContainer) {
        final Table criteriaTable = getCriteriaTable();
        final Table criteriaValueTable = getCriteriaValueTable();
        final Long criteriaSetPkValue = criteriaSetRow.getPKValue();
        final Stream<Row> criteriaRows = dataContainer.getRows(criteriaTable,
                new WhereClause(Criteria.eq(criteriaTable.getColumn(OMASRCRITERIA.SET_ID), criteriaSetPkValue)));
        final Short connectorId = criteriaSetRow.get(OMASRCRITERIASET.CONNECTOR_ID);
        final AutoAssignmentCriteriaSet autoAssignmentCriteriaSet = new AutoAssignmentCriteriaSet();

        if (Objects.nonNull(connectorId)) {
            autoAssignmentCriteriaSet.setConnector(CriteriaConnector.get(connectorId));
        }

        autoAssignmentCriteriaSet.setOrder(criteriaSetRow.get(OMASRCRITERIASET.ORDER));
        autoAssignmentCriteriaSet.setId(criteriaSetPkValue);
        autoAssignmentCriteriaSet.setCriteriaList(criteriaRows.map(criteriaRow -> {
            final AutoAssignmentCriteria autoAssignmentCriteria = new AutoAssignmentCriteria();
            final Long criteriaId = criteriaRow.getPKValue();
            final Short criteriaConnectorId = criteriaRow.get(OMASRCRITERIA.CONNECTOR_ID);
            final Stream<Row> criteriaValues = dataContainer.getRows(criteriaValueTable,
                    new WhereClause(Criteria.eq(criteriaValueTable.getColumn(OMASRCRITERIAVALUE.CRITERIA_ID), criteriaId)));

            if (Objects.nonNull(criteriaConnectorId)) {
                autoAssignmentCriteria.setConnector(CriteriaConnector.get(criteriaConnectorId));
            }
            autoAssignmentCriteria.setOrder(criteriaRow.get(OMASRCRITERIA.ORDER));
            autoAssignmentCriteria.setId(criteriaId);
            autoAssignmentCriteria.setFieldId(criteriaRow.get(OMASRCRITERIA.FIELD_ID));
            autoAssignmentCriteria.setComparator(comparatorService.get(criteriaRow.get(OMASRCRITERIA.COMPARATOR_ID)));
            autoAssignmentCriteria.setCriteriaValues(criteriaValues.map(valueRow -> {
                final AutoAssignmentCriteriaValue assignmentCriteriaValue = new AutoAssignmentCriteriaValue();

                assignmentCriteriaValue.setId(valueRow.getPKValue());
                assignmentCriteriaValue.setValue(valueRow.get(OMASRCRITERIAVALUE.VALUE));
                assignmentCriteriaValue.setValueEntityId(valueRow.get(OMASRCRITERIAVALUE.VALUE_ENTITY_ID));

                return assignmentCriteriaValue;
            }).toList());

            return autoAssignmentCriteria;
        }).toList());

        return autoAssignmentCriteriaSet;
    }

    /**
     * Get the collection of rule assignment users for the specified assignment and data container.
     *
     * @param orgId         Refers the organisation ID.
     * @param assignmentId  The assignment id.
     * @param dataContainer The DataContainer containing user data.
     * @param crmUsers      Refers the grouped users in a map.
     * @return A collection of RuleAssignmentUser objects.
     */
    private Collection<AssignmentRuleUser> getRuleAssignmentUsers(final long orgId, final Long assignmentId, final DataContainer dataContainer,
                                                                  final Map<Long, CrmUser> crmUsers) {
        final Table table = getRuleUsersTable();
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);

        return dataContainer.getRows(table, new WhereClause(Criteria.eq(table.getColumn(OMASRRULEUSERS.ASSIGNMENT_RULE_ID), assignmentId)))
                .map(row -> {
                    final AssignmentRuleUser assignmentRuleUser = new AssignmentRuleUser();
                    final Long territoryId = row.get(OMASRRULEUSERS.TERRITORY_ID);
                    final Long userId = row.get(OMASRRULEUSERS.USER_ID);

                    if (Objects.nonNull(territoryId)) {
                        final Territory territory = new Territory();

                        territory.setId(territoryId);
                        territory.setName(dataContainer.getRow(territoryTable, territoryId).get(OMCRMTERRITORY.NAME));
                        assignmentRuleUser.setTerritory(territory);
                    }
                    assignmentRuleUser.setId(row.getPKValue());
                    assignmentRuleUser.setUserId(userId);
                    assignmentRuleUser.setCrmUser(crmUsers.get(userId));

                    return assignmentRuleUser;
                }).toList();
    }

    /**
     * <p>
     * Gets the auto assignment rule name
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param autoAssignmentRuleId  Represents the rule id
     * @return The name of the auto assignment rule
     */
    public String getName(final Long orgId, final Long autoAssignmentRuleId) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, List.of(autoAssignmentRuleId), List.of(table.getColumn(OMASRASSIGNMENTRULE.NAME))).getRow(table);

        return Objects.isNull(row) ? null : row.get(OMASRASSIGNMENTRULE.NAME);
    }
}
