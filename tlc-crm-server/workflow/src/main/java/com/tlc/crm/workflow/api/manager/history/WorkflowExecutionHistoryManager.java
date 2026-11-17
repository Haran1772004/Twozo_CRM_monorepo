package com.tlc.crm.workflow.api.manager.history;

import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.commons.page.Page;
import com.tlc.commons.json.Json;

import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.model.filter.Type;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionHistory;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionHistory;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilter;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilterType;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.sql.resource.MWFLACTIONEVENT;
import com.tlc.crm.workflow.sql.resource.OMWFLACTIONEXECUTIONHISTORY;
import com.tlc.crm.workflow.sql.resource.OMWFLEXECUTIONHISTORY;
import com.tlc.crm.workflow.sql.resource.OMWFLACTIONS;
import com.tlc.crm.workflow.sql.resource.OMWFLFILTER;
import com.tlc.crm.workflow.sql.resource.OMWFLFILTERVALUES;
import com.tlc.crm.workflow.sql.resource.OMWFLWORKFLOW;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowActionExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;

import com.tlc.l10n.temporal.resolver.DateTimeUtil;
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
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;
import com.tlc.sql.common.api.ds.WritableDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * <p>
 * Manages the {@link WorkflowExecutionHistory} related DB actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowExecutionHistoryManager {

    private final WorkflowFilterManager workflowFilterManager;
    private final WorkflowManager workflowManager;
    private final UserService userService;
    private final DateTimeUtil dateTimeUtil;

    private WorkflowExecutionHistoryManager() {
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
        this.workflowManager = WorkflowManager.getInstance();
        this.userService = UserService.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final WorkflowExecutionHistoryManager INSTANCE = new WorkflowExecutionHistoryManager();
    }

    /**
     * <p>
     * Gets the {@link WorkflowExecutionHistoryManager} Instance
     * </p>
     *
     * @return {@link WorkflowExecutionHistoryManager}
     */
    public static WorkflowExecutionHistoryManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Returns {@link WritableDataStore}.
     *
     * @return {@link WritableDataStore}.
     */
    private WritableDataStore writableDataStore() {
        return DataStoreProvider.getWritableDataStore();
    }

    /**
     * Gets the {@link Table} of history.
     *
     * @return The history table.
     */
    private Table getTable() {
        return Table.get(OMWFLEXECUTIONHISTORY.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of action history
     * </p>
     *
     * @return The action history table
     */
    private Table getActionExecutionHistoryTable() {
        return Table.get(OMWFLACTIONEXECUTIONHISTORY.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of action event
     * </p>
     *
     * @return The action event table
     */
    private Table getActionEventTable() {
        return Table.get(MWFLACTIONEVENT.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Retrieves the total count of {@link WorkflowExecutionHistory} matches for the given organization id and filter criteria
     * </p>
     *
     * @param orgId                          Represents an organization id
     * @param workflowExecutionHistoryFilter It holds the {@link WorkflowExecutionHistoryFilter} details
     * @return The total count of {@link WorkflowExecutionHistory}
     */
    public long getTotalCount(final long orgId, final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        final Table executionHistoryTable = getTable();
        final CountQuery countQuery = CountQuery.get(executionHistoryTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.ORG_ID), orgId))
                .and(Criteria.notEq(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.STATUS), WorkflowExecutionStatus.IN_PROGRESS.getId()));
        final WhereClause executionHistoryFilterWhereClause = Objects.isNull(workflowExecutionHistoryFilter) ? null : workflowFilterManager.getExecutionHistoryFilterWhereClause(workflowExecutionHistoryFilter);

        countQuery.addJoinClause(new JoinClause(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_ID), workflowManager.getTable().getPKColumn()));
        countQuery.setWhereClause(Objects.isNull(executionHistoryFilterWhereClause) ? whereClause : whereClause.and(executionHistoryFilterWhereClause));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the Collection of {@link WorkflowExecutionHistory} for given organization id
     * </p>
     *
     * @param orgId                          Refers an organization id
     * @param start                          Refers the start value of the history to be fetched
     * @param limit                          Refers the limit value of the history to be fetched
     * @param sortField                      Refers the field used to sorted the data
     * @param sortType                       Refers the sort type
     * @param workflowExecutionHistoryFilter An entity which holds the attributes of {@link WorkflowExecutionHistoryFilter}
     * @return The collection of {@link WorkflowExecutionHistory}
     */
    public Collection<WorkflowExecutionHistory> getWorkflowExecutionHistoryList(final long orgId, final int start, final int limit, final String sortField,
                                                                                final String sortType, final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        final Collection<Long> executionHistoryIds = getExecutionHistoryIds(orgId, start, limit, sortField, sortType, workflowExecutionHistoryFilter);

        if (Objects.nonNull(executionHistoryIds)) {
            final Table workflowTable = workflowManager.getTable();
            final Table executionHistoryTable = getTable();
            final SelectQuery selectQuery = SelectQuery.get(workflowTable);
            final Column workflowPkColumn = workflowTable.getPKColumn();
            final Column actionWorkflowIdColumn = workflowManager.getActionTable().getColumn(OMWFLACTIONS.WORKFLOW_ID);
            final Column executionHistoryWorkflowIdColumn = executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_ID);

            selectQuery.addSelectClause(List.of(workflowTable.getColumn(OMWFLWORKFLOW.NAME), workflowTable.getColumn(OMWFLWORKFLOW.TRIGGER_SOURCE),
                    executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME), executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.STATUS), actionWorkflowIdColumn,
                    executionHistoryWorkflowIdColumn, workflowTable.getColumn(OMWFLWORKFLOW.ORG_ID), workflowTable.getColumn(OMWFLWORKFLOW.OWNER_ID)));
            selectQuery.addJoinClause(List.of(new JoinClause(workflowPkColumn, actionWorkflowIdColumn),
                    new JoinClause(workflowPkColumn, executionHistoryWorkflowIdColumn)));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(executionHistoryTable.getPKColumn(), executionHistoryIds)));
            applySortCriteria(sortField, sortType, selectQuery);
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
            final Collection<WorkflowExecutionHistory> histories = dataContainer.getRows(executionHistoryTable).map(row -> convertListRowToModel(row, dataContainer)).toList();

            if (Objects.equals(WorkflowAttributes.ACTIONS, sortField)) {
                final Comparator<WorkflowExecutionHistory> comparator = Comparator.comparing(WorkflowExecutionHistory::getWorkflow, Comparator.nullsLast(Comparator.comparing(Workflow::getTotalActions)));
                final Comparator<WorkflowExecutionHistory> historyComparator = WorkflowAttributes.DESC.equalsIgnoreCase(sortType)
                        ? comparator.reversed() : comparator;

                return histories.stream().sorted(historyComparator).toList();
            }

            return histories;
        }
        return Collections.emptyList();
    }

    /**
     * <p>
     * Applies order by clause and select clause based on given sort field and their sort type
     * </p>
     *
     * @param sortField   Represents the name of the field which is needs to be sort
     * @param sortType    Represents the type of the sorting
     * @param selectQuery Represents the {@link SelectQuery}
     */
    private void applySortCriteria(final String sortField, final String sortType, final SelectQuery selectQuery) {
        final Table workflowTable = workflowManager.getTable();
        final Table executionHistoryTable = getTable();
        final OrderType orderType = WorkflowAttributes.DESC.equalsIgnoreCase(sortType) ?
                OrderType.DESCENDING : OrderType.ASCENDING;
        final Consumer<Column> consumer = (column) -> selectQuery.addOrderByClause(new OrderByClause(column, orderType));

        switch (sortField) {
            case "workflowName" -> consumer.accept(workflowTable.getColumn(OMWFLWORKFLOW.NAME));
            case "triggerSource" -> consumer.accept(workflowTable.getColumn(OMWFLWORKFLOW.TRIGGER_SOURCE));
            case "executedTime" -> consumer.accept(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME));
            case "status" -> consumer.accept(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.STATUS));
        }
    }

    /**
     * <p>
     * Converts the row to {@link WorkflowExecutionHistory}
     * </p>
     *
     * @param row           The row which holds the history records
     * @param dataContainer A container that holds the updated information of {@link WorkflowExecutionHistory}
     * @return {@link WorkflowExecutionHistory}
     */
    private WorkflowExecutionHistory convertListRowToModel(final Row row, final DataContainer dataContainer) {
        final WorkflowExecutionHistory workflowExecutionHistory = new WorkflowExecutionHistory();
        final Workflow workflow = new Workflow();
        final Table workflowTable = workflowManager.getTable();
        final Table actionTable = workflowManager.getActionTable();
        final Long workflowId = row.get(OMWFLEXECUTIONHISTORY.WORKFLOW_ID);
        final Row workflowRow = dataContainer.getRow(workflowTable, new WhereClause(Criteria.eq(workflowTable.getPKColumn(), workflowId)));
        final Number actionCount = dataContainer.getRows(actionTable,
                new WhereClause(Criteria.eq(actionTable.getColumn(OMWFLACTIONS.WORKFLOW_ID), workflowId))).count();

        if (Objects.nonNull(workflowRow)) {
            workflow.setName(workflowRow.get(OMWFLWORKFLOW.NAME));
            workflow.setTriggerSource(TriggerSource.get(workflowRow.get(OMWFLWORKFLOW.TRIGGER_SOURCE)));
            workflowExecutionHistory.setCrmUser(userService.getUser(workflowRow.getOrgId(), workflowRow.get(OMWFLWORKFLOW.OWNER_ID)));
        }

        workflow.setId(workflowId);
        workflow.setTotalActions(actionCount.intValue());
        workflowExecutionHistory.setId(row.getPKValue());
        workflowExecutionHistory.setExecutedTime(row.get(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME));
        workflowExecutionHistory.setExecutionStatus(WorkflowExecutionStatus.get(row.get(OMWFLEXECUTIONHISTORY.STATUS)));
        workflowExecutionHistory.setWorkflow(workflow);

        return workflowExecutionHistory;
    }

    /**
     * <p>
     * Gets the {@link Collection} of history ids matches for given {@link WorkflowExecutionHistoryFilter}
     * </p>
     *
     * @param orgId                          Refers an organization id
     * @param start                          Refers the start value of the history to be fetched
     * @param limit                          Refers the limit value of the history to be fetched
     * @param sortField                      Refers the field used to sorted the data
     * @param sortType                       Refers the sort type ASC/DESC
     * @param workflowExecutionHistoryFilter An entity which holds the {@link WorkflowExecutionHistoryFilter}
     * @return {@link Collection} of history ids
     */
    private Collection<Long> getExecutionHistoryIds(final long orgId, final int start, final int limit, final String sortField,
                                                    final String sortType, final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        final Table executionHistoryTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(executionHistoryTable);
        final WhereClause executionHistoryFilterWhereClause = Objects.isNull(workflowExecutionHistoryFilter) ? null : workflowFilterManager.getExecutionHistoryFilterWhereClause(workflowExecutionHistoryFilter);
        final Criteria criteria = Criteria.notEq(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.STATUS), WorkflowExecutionStatus.IN_PROGRESS.getId());

        selectQuery.addJoinClause(new JoinClause(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_ID), workflowManager.getTable().getPKColumn()));
        selectQuery.setWhereClause(Objects.isNull(executionHistoryFilterWhereClause) ? new WhereClause(criteria) : executionHistoryFilterWhereClause.and(criteria));
        applySortCriteria(sortField, sortType, selectQuery);
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRows(executionHistoryTable).filter(Objects::nonNull).map(Row::getPKValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * <p>
     * Gets the {@link WorkflowExecutionHistory} for the given organization id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link WorkflowExecutionHistory}
     */
    public WorkflowExecutionHistory get(final long orgId, final Long historyId) {
        final Table executionHistoryTable = getTable();
        final Table actionExecutionHistoryTable = getActionExecutionHistoryTable();
        final SelectQuery selectQuery = SelectQuery.get(executionHistoryTable);
        final Column historyPkColumn = executionHistoryTable.getPKColumn();
        final Column actionExecutionHistoryIdColumn = actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.EXECUTION_HISTORY_ID);
        final Column actionIdColumn = actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.ACTION_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(List.of(actionExecutionHistoryIdColumn, actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.STATUS),
                actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.FAILURE_REASON), actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.ACTION_EXECUTED_TIME),
                actionIdColumn, executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME), executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.ORG_ID),
                executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.CRITERIA_EXECUTED_TIME), executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.CRITERIA_STATUS),
                executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_ID), executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_STATE)));
        selectQuery.addJoinClause(new JoinClause(historyPkColumn, actionExecutionHistoryIdColumn, leftJoin));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(historyPkColumn, historyId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : convertRowToModel(dataContainer.getRow(executionHistoryTable), dataContainer);
    }

    /**
     * <p>
     * Converts the row to {@link WorkflowExecutionHistory}
     * </p>
     *
     * @param row           Holds the row of the history table
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link WorkflowExecutionHistory}
     */
    private WorkflowExecutionHistory convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Table actionExecutionHistoryTable = getActionExecutionHistoryTable();
        final Long workflowId = row.get(OMWFLEXECUTIONHISTORY.WORKFLOW_ID);
        final Long orgId = row.getOrgId();
        final WorkflowExecutionHistory workflowExecutionHistory = new WorkflowExecutionHistory(orgId, workflowId);
        final Long historyId = row.getPKValue();final Collection<WorkflowActionExecutionHistory> workflowActionExecutionHistories = new ArrayList<>();
        final Stream<Row> actionExecutionHistoryRows = dataContainer.getRows(actionExecutionHistoryTable,
                new WhereClause(Criteria.eq(actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.EXECUTION_HISTORY_ID), historyId)));
        final Short criteriaStatus = row.get(OMWFLEXECUTIONHISTORY.CRITERIA_STATUS);

        actionExecutionHistoryRows.forEach(actionExecutionHistoryRow -> {
            final WorkflowActionExecutionHistory workflowActionExecutionHistory = new WorkflowActionExecutionHistory();
            final Long actionId = actionExecutionHistoryRow.get(OMWFLACTIONEXECUTIONHISTORY.ACTION_ID);
            final Short failureReason = actionExecutionHistoryRow.get(OMWFLACTIONEXECUTIONHISTORY.FAILURE_REASON);

            workflowActionExecutionHistory.setActionId(actionId);
            workflowActionExecutionHistory.setActionExecutionStatus(WorkflowActionExecutionStatus.get(actionExecutionHistoryRow.get(OMWFLACTIONEXECUTIONHISTORY.STATUS)));
            workflowActionExecutionHistory.setExecutedTime(dateTimeUtil.getTimeStamp(actionExecutionHistoryRow.get(OMWFLACTIONEXECUTIONHISTORY.ACTION_EXECUTED_TIME)));

            if (Objects.nonNull(failureReason)) {
                workflowActionExecutionHistory.setFailureCause(WorkflowActionExecutionFailureCause.get(failureReason));
            }

            workflowActionExecutionHistories.add(workflowActionExecutionHistory);
        });

        workflowExecutionHistory.setId(historyId);
        workflowExecutionHistory.setOrgId(orgId);
        workflowExecutionHistory.setExecutedTime(row.get(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME));
        workflowExecutionHistory.setConditionExecutedTime(row.get(OMWFLEXECUTIONHISTORY.CRITERIA_EXECUTED_TIME));
        workflowExecutionHistory.setActionExecutionHistories(workflowActionExecutionHistories);
        workflowExecutionHistory.setWorkflowState(Json.object((String) row.get(OMWFLEXECUTIONHISTORY.WORKFLOW_STATE)));

        if (Objects.nonNull(criteriaStatus)) {
            workflowExecutionHistory.setCriteriaExecutionStatus(WorkflowCriteriaExecutionStatus.get(criteriaStatus));
        }

        return workflowExecutionHistory;
    }

    /**
     * <p>
     * Checks whether the given history id exists or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param historyId Represents the history id
     * @return true if its exits
     */
    public boolean exists(final long orgId, final Long historyId) {
        final Table executionHistoryTable = getTable();
        final CountQuery countQuery = CountQuery.get(executionHistoryTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(executionHistoryTable.getPKColumn(), historyId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the {@link WorkflowExecutionHistoryFilter} for the given organization id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link Collection} of {@link WorkflowExecutionHistoryFilter}
     */
    public WorkflowExecutionHistoryFilter getWorkflowExecutionHistoryFilter(final long orgId, final long userId, final Type type) {
        final DataContainer dataContainer = workflowFilterManager.getWorkflowFilterContainer(orgId, userId, type);

        if (!dataContainer.isEmpty()) {
            final Table filterValueTable = workflowFilterManager.getFilterValueTable();
            final Map<WorkflowExecutionHistoryFilterType, Long> conditions = new HashMap<>();
            final Row filterRow = dataContainer.getRow(workflowFilterManager.getTable());

            if (Objects.nonNull(filterRow)) {
                final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter = new WorkflowExecutionHistoryFilter();
                final WorkflowExecutionHistoryFilterType historyFilterType = WorkflowExecutionHistoryFilterType.get(filterRow.get(OMWFLFILTER.FILTER_GROUP));
                final Long value = dataContainer.getRow(filterValueTable, new WhereClause(Criteria.eq(filterValueTable.getColumn(OMWFLFILTERVALUES.FILTER_ID),
                        filterRow.getPKValue()))).get(OMWFLFILTERVALUES.VALUE);

                conditions.put(historyFilterType, value);
                workflowExecutionHistoryFilter.setCriteria(conditions);

                return workflowExecutionHistoryFilter;
            }
        }
        return null;
    }

    /**
     * Retrieves the history id by loading the history details into the database.
     *
     * @param workflowExecutionHistory Refers the {@link WorkflowExecutionHistory} attributes.
     * @return The history id.
     */
    public Long create(final WorkflowExecutionHistory workflowExecutionHistory) {
        final Long orgId = workflowExecutionHistory.orgId();
        final Row executionHistoryRow = new Row(getTable());
        final Long executionHistoryPkValue = executionHistoryRow.getPKValue();

        executionHistoryRow.setOrgId(orgId);
        executionHistoryRow.set(OMWFLEXECUTIONHISTORY.WORKFLOW_ID, workflowExecutionHistory.getWorkflowId());
        executionHistoryRow.set(OMWFLEXECUTIONHISTORY.STATUS, WorkflowExecutionStatus.IN_PROGRESS.getId());
        executionHistoryRow.set(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME, Instant.now().toEpochMilli());
        executionHistoryRow.set(OMWFLEXECUTIONHISTORY.WORKFLOW_STATE, workflowExecutionHistory.getWorkflowState().toString());

        orgDataStore(orgId).addRow(executionHistoryRow);

        return executionHistoryPkValue;
    }

    /**
     * Updates the {@link WorkflowCriteriaExecutionStatus} of workflow execution process.
     *
     * @param orgId                           Refers the organization id.
     * @param executionHistoryId              Refers the execution history id.
     * @param workflowCriteriaExecutionStatus Refers the {@link WorkflowCriteriaExecutionStatus}.
     */
    public void updateCriteriaExecutionStatus(final long orgId, final long executionHistoryId,
                                              final WorkflowCriteriaExecutionStatus workflowCriteriaExecutionStatus) {
        final Table historyTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(historyTable);

        updateQuery.addUpdateColumn(historyTable.getColumn(OMWFLEXECUTIONHISTORY.CRITERIA_EXECUTED_TIME), Instant.now().toEpochMilli());
        updateQuery.addUpdateColumn(historyTable.getColumn(OMWFLEXECUTIONHISTORY.CRITERIA_STATUS), workflowCriteriaExecutionStatus.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(historyTable.getPKColumn(), executionHistoryId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * Updates the history status of a workflow.
     *
     * @param orgId                   Refers the organization id.
     * @param executionHistoryId      Refers the execution history id.
     * @param workflowExecutionStatus Refers the {@link WorkflowExecutionStatus}.
     */
    public void updateExecutionHistoryStatus(final long orgId, final long executionHistoryId, final WorkflowExecutionStatus workflowExecutionStatus) {
        final Table historyTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(historyTable);

        updateQuery.addUpdateColumn(historyTable.getColumn(OMWFLEXECUTIONHISTORY.STATUS), workflowExecutionStatus.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(historyTable.getPKColumn(), executionHistoryId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * Stores the {@link WorkflowActionExecutionHistory} record into the database.
     *
     * @param workflowActionExecutionHistory Refers the {@link WorkflowActionExecutionHistory} attributes.
     */
    public void createActionExecutionHistory(final WorkflowActionExecutionHistory workflowActionExecutionHistory) {
        final Row actionExecutionHistoryRow = new Row(getActionExecutionHistoryTable());
        final long orgId = workflowActionExecutionHistory.getOrgId();
        final WorkflowActionExecutionFailureCause failureCause = workflowActionExecutionHistory.getFailureCause();

        actionExecutionHistoryRow.setOrgId(orgId);
        actionExecutionHistoryRow.set(OMWFLACTIONEXECUTIONHISTORY.EXECUTION_HISTORY_ID, workflowActionExecutionHistory.getExecutionHistoryId());
        actionExecutionHistoryRow.set(OMWFLACTIONEXECUTIONHISTORY.ACTION_ID, workflowActionExecutionHistory.getActionId());
        actionExecutionHistoryRow.set(OMWFLACTIONEXECUTIONHISTORY.STATUS, workflowActionExecutionHistory.getActionExecutionStatus().getId());
        actionExecutionHistoryRow.set(OMWFLACTIONEXECUTIONHISTORY.FAILURE_REASON, Objects.isNull(failureCause) ? null : failureCause.getId());
        actionExecutionHistoryRow.set(OMWFLACTIONEXECUTIONHISTORY.ACTION_EXECUTED_TIME, workflowActionExecutionHistory.getExecutedTime());

        orgDataStore(orgId).addRow(actionExecutionHistoryRow);
    }

    /**
     * Updates the {@link WorkflowActionExecutionHistory} record into the database.
     *
     * @param workflowActionExecutionHistory Refers the {@link WorkflowActionExecutionHistory} attributes.
     */
    public void updateActionExecutionHistory(final WorkflowActionExecutionHistory workflowActionExecutionHistory) {
        final Table actionExecutionHistoryTable = getActionExecutionHistoryTable();
        final UpdateQuery updateQuery = UpdateQuery.get(actionExecutionHistoryTable);
        final WorkflowActionExecutionFailureCause failureCause = workflowActionExecutionHistory.getFailureCause();

        updateQuery.addUpdateColumn(actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.STATUS), workflowActionExecutionHistory.getActionExecutionStatus().getId());
        updateQuery.addUpdateColumn(actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.FAILURE_REASON), Objects.isNull(failureCause) ? null : failureCause.getId());
        updateQuery.addUpdateColumn(actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.ACTION_EXECUTED_TIME), workflowActionExecutionHistory.getExecutedTime());

        updateQuery.setWhereClause(new WhereClause(Criteria.eq(actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.EXECUTION_HISTORY_ID), workflowActionExecutionHistory.getExecutionHistoryId()))
                .and(Criteria.eq(actionExecutionHistoryTable.getColumn(OMWFLACTIONEXECUTIONHISTORY.ACTION_ID), workflowActionExecutionHistory.getActionId())));

        orgDataStore(workflowActionExecutionHistory.getOrgId()).update(updateQuery);
    }

    /**
     * Fetches the expired execution history log count.
     *
     * @return The expired history count.
     */
    public long getObsoleteExecutionHistoryCount() {
        final CountQuery countQuery = CountQuery.get(getTable());

        countQuery.setWhereClause(getObsoleteExecutionHistoryWhereClause());
        return writableDataStore().get(countQuery);
    }

    /**
     * Retrieves the page for the expired execution history.
     *
     * @param start Represents the lower bound of expired history to be fetched.
     * @param limit Represents the upper bound of expired history to be fetched.
     * @return {@link Page} of {@link WorkflowExecutionHistory}
     */
    public Page<WorkflowExecutionHistory> getObsoleteExecutionHistoryPage(final int start, final int limit) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getPKColumn());
        selectQuery.setWhereClause(getObsoleteExecutionHistoryWhereClause());
        selectQuery.setLimitClause(new LimitClause(start, limit));

        final DataContainer dataContainer = writableDataStore().get(selectQuery);
        final Page<WorkflowExecutionHistory> executionHistoryPage = new Page<>(start);

        if (!dataContainer.isEmpty()) {
            executionHistoryPage.setData(dataContainer.getRows(table).map(row -> {
                final WorkflowExecutionHistory workflowExecutionHistory = new WorkflowExecutionHistory();

                workflowExecutionHistory.setId(row.getPKValue());
                return workflowExecutionHistory;
            }).toList());
        }

        return executionHistoryPage;
    }

    /**
     * Gets the expired execution history {@link WhereClause}.
     *
     * @return The {@link WhereClause}.
     */
    private WhereClause getObsoleteExecutionHistoryWhereClause() {
        return new WhereClause(Criteria.lt(getTable().getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME),
                Instant.now().minus(15, ChronoUnit.DAYS).toEpochMilli()));
    }

    /**
     * Deletes the execution history logs for the specified IDs.
     *
     * @param executionHistoryIds Refers the ids of the execution history.
     */
    public void delete(final Collection<Long> executionHistoryIds) {
        writableDataStore().delete(getTable(), executionHistoryIds);
    }
}
