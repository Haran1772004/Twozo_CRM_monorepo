package com.tlc.crm.workflow.v2.api.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.sql.resource.OMWFLEMAILTRACKER;
import com.tlc.crm.core.workflow.internal.WorkflowEmailTrackerManager;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilter;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.v2.api.model.DueSchedule;
import com.tlc.crm.workflow.v2.api.model.EventContextType;
import com.tlc.crm.workflow.v2.api.model.ExecutionEntity;
import com.tlc.crm.workflow.v2.api.model.ExecutionStatus;
import com.tlc.crm.workflow.v2.api.model.NodeExecutionStatus;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.ScheduleType;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.api.model.WaitRegistration;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2Execution;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2ExecutionNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowVersion;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLEXECUTIONNODE;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLEXECUTIONSCHEDULER;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLEXECUTIONSOURCEENTITY;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLEXECUTIONV2;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLEXECUTIONWAITREGISTRATION;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLNODE;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLVERSION;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLWORKFLOWV2;

import com.tlc.commons.page.Page;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.WritableDataStore;

import java.time.temporal.ChronoUnit;
import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Persists workflow V2 executions, node outcomes, schedules, and wait registrations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2ExecutionManager {

    private final WorkflowFilterManager workflowFilterManager;
    private final UserService userService;
    private final WorkflowEmailTrackerManager workflowEmailTrackerManager;

    private WorkflowV2ExecutionManager() {
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
        this.userService = UserService.getInstance();
        this.workflowEmailTrackerManager = WorkflowEmailTrackerManager.getInstance();
    }

    private static class Instance {
        private static final WorkflowV2ExecutionManager INSTANCE = new WorkflowV2ExecutionManager();
    }

    public static WorkflowV2ExecutionManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the organization data store.
     *
     * @param orgId The organization ID.
     * @return The organization data store.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Returns the readable data store.
     *
     * @return The readable data store.
     */
    private ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * Returns the writable data store.
     *
     * @return the writable data store
     */
    private WritableDataStore writableDataStore() {
        return DataStoreProvider.getWritableDataStore();
    }

    /**
     * Returns the execution table.
     *
     * @return The execution table.
     */
    private Table getExecutionTable() {
        return Table.get(OMWFLEXECUTIONV2.TABLE);
    }

    /**
     * Returns the execution node table.
     *
     * @return The execution node table.
     */
    private Table getExecutionNodeTable() {
        return Table.get(OMWFLEXECUTIONNODE.TABLE);
    }

    /**
     * Returns the schedule table.
     *
     * @return The schedule table.
     */
    private Table getScheduleTable() {
        return Table.get(OMWFLEXECUTIONSCHEDULER.TABLE);
    }

    /**
     * Returns the wait table.
     *
     * @return The wait table.
     */
    private Table getWaitTable() {
        return Table.get(OMWFLEXECUTIONWAITREGISTRATION.TABLE);
    }

    /**
     * Returns the source entity table.
     *
     * @return The source entity table.
     */
    private Table getSourceEntityTable() {
        return Table.get(OMWFLEXECUTIONSOURCEENTITY.TABLE);
    }

    /**
     * Returns the workflow table.
     *
     * @return The workflow table.
     */
    private Table getWorkflowTable() {
        return Table.get(OMWFLWORKFLOWV2.TABLE);
    }

    /**
     * Returns the version table.
     *
     * @return The version table.
     */
    private Table getVersionTable() {
        return Table.get(OMWFLVERSION.TABLE);
    }

    /**
     * Returns the node table.
     *
     * @return The node table.
     */
    private Table getNodeTable() {
        return Table.get(OMWFLNODE.TABLE);
    }

    /**
     * Creates a running execution.
     *
     * @param workflow the published workflow graph
     * @return the new execution id
     */
    public Long create(final Long userId, final WorkflowV2 workflow) {
        final Long orgId = workflow.orgId();
        final WorkflowVersion version = workflow.getWorkflowVersion();
        final Row executionRow = new Row(getExecutionTable());
        final Long executionId = executionRow.getPKValue();

        executionRow.setOrgId(orgId);
        executionRow.set(OMWFLEXECUTIONV2.USER_ID, userId);
        executionRow.set(OMWFLEXECUTIONV2.VERSION_ID, version.id());
        executionRow.set(OMWFLEXECUTIONV2.STATUS, ExecutionStatus.IN_PROGRESS.getId());
        executionRow.set(OMWFLEXECUTIONV2.WORKFLOW_ID, workflow.id());
        executionRow.set(OMWFLEXECUTIONV2.STARTED_TIME, Instant.now().toEpochMilli());
        orgDataStore(orgId).addRow(executionRow);

        return executionId;
    }

    /**
     * Stores the node context.
     *
     * @param orgId       The organization ID.
     * @param executionId The execution ID.
     * @param json        The JSON object.
     */
    public void storeNodeContext(final long orgId, long executionId, final JsonObject json) {
        final Table executionTable = getExecutionTable();
        final UpdateQuery updateQuery = UpdateQuery.get(executionTable);

        updateQuery.addUpdateColumn(executionTable.getColumn(OMWFLEXECUTIONV2.NODE_CONTEXT_JSON), json);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(executionTable.getPKColumn(), executionId)));

        executeUpdates(orgId, List.of(updateQuery));
    }

    /**
     * Returns the node context.
     *
     * @param orgId       The organization ID.
     * @param executionId The execution ID.
     * @return The node context.
     */
    public String getNodeContext(final long orgId, long executionId) {
        final Table executionTable = getExecutionTable();
        final Row row = orgDataStore(orgId).get(executionTable, executionId, List.of(executionTable.getColumn(OMWFLEXECUTIONV2.NODE_CONTEXT_JSON)));

        return row.get(OMWFLEXECUTIONV2.NODE_CONTEXT_JSON);
    }

    /**
     * Starts a node execution row.
     *
     * @param orgId       the organization id
     * @param executionId the execution id
     * @param nodeId      the definition node id
     */
    public void startNode(final long orgId, final long executionId, final long nodeId) {
        final DataContainer container = DataContainer.create();
        final Row nodeRow = new Row(getExecutionNodeTable());

        nodeRow.setOrgId(orgId);
        nodeRow.set(OMWFLEXECUTIONNODE.EXECUTION_ID, executionId);
        nodeRow.set(OMWFLEXECUTIONNODE.NODE_ID, nodeId);
        nodeRow.set(OMWFLEXECUTIONNODE.STATUS, NodeExecutionStatus.IN_PROGRESS.getId());
        nodeRow.set(OMWFLEXECUTIONNODE.STARTED_TIME, Instant.now().toEpochMilli());
        container.addNewRow(nodeRow);

        commitChanges(orgId, container);
    }

    /**
     * Completes a node successfully.
     *
     * @param orgId       the organization id
     * @param executionId the execution id
     * @param nodeId      the definition node id
     */
    public void completeNode(final long orgId, final long executionId, final long nodeId) {
        executeUpdates(orgId, List.of(buildUpdateNodeStatusQuery(executionId, nodeId, NodeExecutionStatus.COMPLETED, null)));
    }

    /**
     * Completes a parked node and marks the execution running in one transaction.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @param nodeId      node id
     */
    public void completeNodeAndMarkRunning(final long orgId, final long executionId, final long nodeId) {
        executeUpdates(orgId, buildCompleteNodeAndMarkRunningQueries(executionId, nodeId));
    }

    /**
     * Cancels wait state, completes the wait node, and marks the execution running in one transaction.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @param nodeId      wait node id
     */
    public void completeWaitAndResume(final long orgId, final long executionId, final long nodeId) {
        executeUpdates(orgId, buildCompleteWaitAndResumeQueries(executionId, nodeId));
    }

    /**
     * Fail-fast: writes node FAILURE then execution FAILED, then cancels open schedules/waits.
     *
     * @param orgId       the organization id
     * @param executionId the execution id
     * @param nodeId      the failed node id
     * @param reason      optional failure reason id
     */
    public void failFast(final long orgId, final long executionId, final Long nodeId, final Short reason) {
        executeUpdates(orgId, buildFailFastQueries(executionId, nodeId, reason));
        workflowEmailTrackerManager.delete(orgId, executionId);
    }

    /**
     * Expires a wait-for-condition node and stops the execution without following subsequent nodes.
     *
     * @param orgId       the organization id
     * @param executionId the execution id
     * @param nodeId      the wait node id
     */
    public void expireWait(final long orgId, final long executionId, final long nodeId) {
        executeUpdates(orgId, buildExpireWaitQueries(executionId, nodeId));
    }

    /**
     * Marks the execution completed.
     *
     * @param orgId       the organization id
     * @param executionId the execution id
     */
    public void completeExecution(final long orgId, final long executionId) {
        executeUpdates(orgId, List.of(buildUpdateCurrentNodeQuery(executionId, null, ExecutionStatus.SUCCESS)));
        workflowEmailTrackerManager.delete(orgId, executionId);
    }

    /**
     * Cancels pending/running executions for a workflow version set.
     *
     * @param orgId      the organization id
     * @param versionIds published/archived version ids of the workflow
     */
    public void cancelPendingByVersionIds(final long orgId, final Collection<Long> versionIds) {
        final Table executionTable = getExecutionTable();
        final SelectQuery selectQuery = SelectQuery.get(executionTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(executionTable.getColumn(OMWFLEXECUTIONV2.VERSION_ID), versionIds))
                .and(Criteria.in(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS),
                        List.of(ExecutionStatus.IN_PROGRESS.getId(), ExecutionStatus.PENDING.getId()))));

        orgDataStore(orgId).get(selectQuery).getRows(executionTable).forEach(row -> {
            final Long executionId = row.getPKValue();
            final List<UpdateQuery> queries = new ArrayList<>();

            queries.add(buildUpdateCurrentNodeQuery(executionId, null, ExecutionStatus.STOPPED));
            queries.addAll(buildCancelOpenSchedulesAndWaitsQueries(executionId));
            executeUpdates(orgId, queries);
        });
    }

    /**
     * Parks a delay node: inserts the schedule and marks node/execution pending in one transaction.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @param nodeId      delay node id
     * @param fireAt      schedule fire time
     */
    public void parkDelay(final long orgId, final long executionId, final long nodeId, final long fireAt) {
        final DataContainer container = DataContainer.create();

        container.addNewRow(buildScheduleRow(orgId, executionId, nodeId, ScheduleType.DELAY, fireAt));
        applyPendingNodeChanges(container, orgId, executionId, nodeId);
        commitChanges(orgId, container);
    }

    /**
     * Parks a wait-for-condition node: registers the wait, inserts timeout schedule, and marks pending in one transaction.
     *
     * @param orgId        organization id
     * @param executionId  execution id
     * @param waitNodeId   wait node id
     * @param sourceNodeId watched source node id
     * @param contextType  event context type
     * @param fireAt       timeout fire time
     */
    public void parkWait(final long orgId, final long executionId, final long waitNodeId, final long sourceNodeId,
                         final EventContextType contextType, final long fireAt) {
        final DataContainer container = DataContainer.create();

        container.addNewRow(buildWaitRegistrationRow(orgId, executionId, waitNodeId, sourceNodeId, contextType));
        container.addNewRow(buildScheduleRow(orgId, executionId, waitNodeId, ScheduleType.WAIT, fireAt));
        applyPendingNodeChanges(container, orgId, executionId, waitNodeId);
        commitChanges(orgId, container);
    }

    /**
     * Parks an async action node while waiting for an external callback.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @param nodeId      action node id
     */
    public void parkAction(final long orgId, final long executionId, final long nodeId) {
        final DataContainer container = DataContainer.create();

        applyPendingNodeChanges(container, orgId, executionId, nodeId);
        commitChanges(orgId, container);
    }

    /**
     * Upserts the source entity.
     *
     * @param executionEntity The execution entity.
     */
    public void upsertSourceEntity(final ExecutionEntity executionEntity) {
        final Long orgId = executionEntity.orgId();
        final Table sourceEntityTable = getSourceEntityTable();
        final SelectQuery selectQuery = SelectQuery.get(sourceEntityTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(sourceEntityTable.getColumn(OMWFLEXECUTIONSOURCEENTITY.EXECUTION_ID), executionEntity.executionId()))
                .and(Criteria.eq(sourceEntityTable.getColumn(OMWFLEXECUTIONSOURCEENTITY.NODE_ID), executionEntity.nodeId()));
        selectQuery.setWhereClause(whereClause);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();
        final Row row = dataContainer.getOrInitRow(sourceEntityTable, whereClause);

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMWFLEXECUTIONSOURCEENTITY.EXECUTION_ID, executionEntity.executionId());
            row.set(OMWFLEXECUTIONSOURCEENTITY.NODE_ID, executionEntity.nodeId());
            row.set(OMWFLEXECUTIONSOURCEENTITY.ENTITY_ID, executionEntity.entityId());
            row.set(OMWFLEXECUTIONSOURCEENTITY.SOURCE, executionEntity.executionSource().getId());
        }
        row.set(OMWFLEXECUTIONSOURCEENTITY.ENTITY_JSON, executionEntity.entityJson());

        targetContainer.updateRow(row);
        commitChanges(orgId, targetContainer);
    }

    /**
     * Returns the entity id recorded for a source node.
     *
     * @param orgId        organization id
     * @param executionId  execution id
     * @param sourceNodeId graph node id
     * @return entity id or null
     */
    public Long getSourceEntityId(final long orgId, final long executionId, final long sourceNodeId) {
        final Table sourceEntityTable = getSourceEntityTable();
        final SelectQuery selectQuery = SelectQuery.get(sourceEntityTable);

        selectQuery.addSelectClause(List.of(sourceEntityTable.getPKColumn(), sourceEntityTable.getColumn(OMWFLEXECUTIONSOURCEENTITY.ENTITY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(sourceEntityTable.getColumn(OMWFLEXECUTIONSOURCEENTITY.EXECUTION_ID), executionId))
                .and(Criteria.eq(sourceEntityTable.getColumn(OMWFLEXECUTIONSOURCEENTITY.NODE_ID), sourceNodeId)));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(sourceEntityTable);

        return row.get(OMWFLEXECUTIONSOURCEENTITY.ENTITY_ID);
    }

    /**
     * Returns all source-node entity mappings for an execution.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @return source node id to (trigger source, entity id)
     */
    public ExecutionEntity getExecutionEntity(final long orgId, final long executionId, final long nodeId) {
        final Table sourceEntityTable = getSourceEntityTable();
        final SelectQuery selectQuery = SelectQuery.get(sourceEntityTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(sourceEntityTable.getColumn(OMWFLEXECUTIONSOURCEENTITY.EXECUTION_ID), executionId))
                .and(Criteria.eq(sourceEntityTable.getColumn(OMWFLEXECUTIONSOURCEENTITY.NODE_ID), nodeId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(sourceEntityTable);

        return new ExecutionEntity(row.getOrgId(), row.get(OMWFLEXECUTIONSOURCEENTITY.EXECUTION_ID),
                row.get(OMWFLEXECUTIONSOURCEENTITY.NODE_ID), row.get(OMWFLEXECUTIONSOURCEENTITY.ENTITY_ID),
                Json.object((String) row.get(OMWFLEXECUTIONSOURCEENTITY.ENTITY_JSON)), ExecutionEntity.ExecutionSource.get((Short) row.get(OMWFLEXECUTIONSOURCEENTITY.SOURCE)));
    }

    /**
     * Counts open wait registrations joined to pending executions.
     *
     * @param orgId organization id
     * @return open wait count
     */
    public long countOpenWaits(final long orgId) {
        final Table waitTable = getWaitTable();
        final Table executionTable = getExecutionTable();
        final CountQuery countQuery = CountQuery.get(waitTable);

        countQuery.addJoinClause(new JoinClause(waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.EXECUTION_ID),
                executionTable.getPKColumn()));
        countQuery.setWhereClause(openWaitsWhereClause(waitTable, executionTable));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Open wait registrations joined to pending executions.
     *
     * @param orgId  organization id
     * @param offset zero-based row offset
     * @param limit  max rows
     * @return open waits
     */
    public Collection<WaitRegistration> listOpenWaits(final long orgId, final int offset, final int limit) {
        final Table waitTable = getWaitTable();
        final Table executionTable = getExecutionTable();
        final SelectQuery selectQuery = SelectQuery.get(waitTable);

        selectQuery.addSelectClause(waitTable, executionTable);
        selectQuery.addJoinClause(new JoinClause(waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.EXECUTION_ID),
                executionTable.getPKColumn()));
        selectQuery.setWhereClause(openWaitsWhereClause(waitTable, executionTable));
        selectQuery.addOrderByClause(new OrderByClause(waitTable.getPKColumn(), OrderByClause.OrderType.ASCENDING));
        selectQuery.setLimitClause(new LimitClause(offset + 1, limit));
        return mapOpenWaits(orgDataStore(orgId).get(selectQuery), waitTable, executionTable);
    }

    /**
     * Gets a wait registration by execution id and node id.
     *
     * @param orgId       The organization id.
     * @param executionId The execution id.
     * @param nodeId      The node id.
     * @return The wait registration.
     */
    public Optional<WaitRegistration> getWaitRegistration(final long orgId, final long executionId, final long nodeId) {
        final Table waitTable = getWaitTable();
        final Table executionTable = getExecutionTable();
        final SelectQuery selectQuery = SelectQuery.get(waitTable);
        final Column column = waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.EXECUTION_ID);

        selectQuery.addSelectClause(waitTable, executionTable);
        selectQuery.addJoinClause(new JoinClause(column, executionTable.getPKColumn()));
        selectQuery.setWhereClause(openWaitsWhereClause(waitTable, executionTable)
                .and(Criteria.eq(column, executionId)).and(Criteria.eq(waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.NODE_ID), nodeId)));

        return mapOpenWaits(orgDataStore(orgId).get(selectQuery), waitTable, executionTable).stream().findFirst();
    }

    /**
     * Claims a due schedule if the execution is still pending.
     *
     * @param orgId      the organization id
     * @param scheduleId the schedule id
     * @return true if this caller won the claim
     */
    public boolean claimSchedule(final long orgId, final long scheduleId) {
        final Table scheduleTable = getScheduleTable();
        final UpdateQuery updateQuery = UpdateQuery.get(scheduleTable);

        updateQuery.addUpdateColumn(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.CANCELLED), true);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(scheduleTable.getPKColumn(), scheduleId))
                .and(Criteria.eq(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.CANCELLED), false)));

        orgDataStore(orgId).update(updateQuery);
        return true;
    }

    /**
     * Due, unclaimed schedules for the given type whose execution is still pending.
     *
     * @param scheduleType delay or wait; null returns both types
     * @param limit        max rows
     * @return due schedules
     */
    public Collection<DueSchedule> getDueSchedules(final ScheduleType scheduleType, final int limit) {
        final Table scheduleTable = getScheduleTable();
        final Table executionTable = getExecutionTable();
        final SelectQuery selectQuery = SelectQuery.get(scheduleTable);

        selectQuery.addSelectClause(scheduleTable, executionTable);
        selectQuery.addJoinClause(new JoinClause(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.EXECUTION_ID), executionTable.getPKColumn()));

        WhereClause whereClause = new WhereClause(Criteria.eq(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.CANCELLED), false))
                .and(Criteria.le(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.FIRE_AT), Instant.now().toEpochMilli()))
                .and(Criteria.eq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.PENDING.getId()));

        if (Objects.nonNull(scheduleType)) {
            whereClause = whereClause.and(Criteria.eq(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.SCHEDULE_TYPE), scheduleType.getId()));
        }
        selectQuery.setWhereClause(whereClause);
        selectQuery.setLimitClause(new LimitClause(1, limit));

        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Collection<DueSchedule> dueSchedules = new ArrayList<>();

        dataContainer.getRows(scheduleTable).forEach(row -> dueSchedules.add(new DueSchedule(
                row.getPKValue(),
                row.getOrgId(),
                row.get(OMWFLEXECUTIONSCHEDULER.EXECUTION_ID),
                row.get(OMWFLEXECUTIONSCHEDULER.NODE_ID),
                ScheduleType.get(row.get(OMWFLEXECUTIONSCHEDULER.SCHEDULE_TYPE))
        )));
        return dueSchedules;
    }

    /**
     * Counts due, unclaimed schedules for the given type whose execution is still pending.
     *
     * @param scheduleType delay or wait
     * @return due schedule count
     */
    public long countDueSchedules(final ScheduleType scheduleType) {
        final Table scheduleTable = getScheduleTable();
        final Table executionTable = getExecutionTable();
        final CountQuery countQuery = CountQuery.get(scheduleTable);

        countQuery.addJoinClause(new JoinClause(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.EXECUTION_ID), executionTable.getPKColumn()));

        WhereClause whereClause = new WhereClause(Criteria.eq(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.CANCELLED), false))
                .and(Criteria.le(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.FIRE_AT), Instant.now().toEpochMilli()))
                .and(Criteria.eq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.PENDING.getId()));

        if (Objects.nonNull(scheduleType)) {
            whereClause = whereClause.and(Criteria.eq(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.SCHEDULE_TYPE), scheduleType.getId()));
        }
        countQuery.setWhereClause(whereClause);
        return readableDataStore().get(countQuery);
    }

    /**
     * Loads a single execution.
     *
     * @param orgId       the organization id
     * @param executionId the execution id
     * @return execution or null
     */
    public WorkflowV2Execution get(final long orgId, final long executionId) {
        final Table executionTable = getExecutionTable();
        final Table nodeTable = getExecutionNodeTable();
        final Table versionTable = getVersionTable();
        final Table definitionNodeTable = Table.get(OMWFLNODE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(executionTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(executionTable, nodeTable, definitionNodeTable, versionTable, getWorkflowTable());
        selectQuery.addJoinClause(new JoinClause(executionTable.getPKColumn(), nodeTable.getColumn(OMWFLEXECUTIONNODE.EXECUTION_ID), leftJoin));
        selectQuery.addJoinClause(new JoinClause(nodeTable.getColumn(OMWFLEXECUTIONNODE.NODE_ID), definitionNodeTable.getPKColumn(), leftJoin));
        selectQuery.addJoinClause(new JoinClause(executionTable.getColumn(OMWFLEXECUTIONV2.VERSION_ID), versionTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), getWorkflowTable().getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(executionTable.getPKColumn(), executionId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return toExecution(dataContainer.getRow(executionTable), dataContainer, true);
    }

    /**
     * Lists completed/failed/cancelled executions with V1-compatible fields.
     *
     * @param orgId     organization id
     * @param start     1-based start
     * @param limit     page size
     * @param sortField workflowName, triggerSource, executedTime, status, or actions
     * @param sortType  asc/desc
     * @param filter    history filter
     * @return page of executions
     */
    public Collection<WorkflowV2Execution> getExecutionList(final long orgId, final int start, final int limit,
                                                            final String sortField, final String sortType,
                                                            final WorkflowExecutionHistoryFilter filter) {
        final Collection<Long> ids = getExecutionIds(orgId, start, limit, sortField, sortType, filter);

        if (Objects.isNull(ids) || ids.isEmpty()) {
            return List.of();
        }

        final Table executionTable = getExecutionTable();
        final Table workflowTable = getWorkflowTable();
        final Table versionTable = getVersionTable();
        final Table nodeTable = Table.get(OMWFLNODE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(executionTable);

        selectQuery.addSelectClause(executionTable, workflowTable, versionTable, nodeTable);
        selectQuery.addJoinClause(new JoinClause(executionTable.getColumn(OMWFLEXECUTIONV2.VERSION_ID), versionTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), workflowTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(versionTable.getPKColumn(), nodeTable.getColumn(OMWFLNODE.VERSION_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(executionTable.getPKColumn(), ids)));
        applySortCriteria(sortField, sortType, selectQuery);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<WorkflowV2Execution> executions = dataContainer.getRows(executionTable)
                .map(row -> toExecution(row, dataContainer, false))
                .toList();

        if (Objects.equals(WorkflowAttributesV2.ACTIONS, sortField)) {
            final Comparator<WorkflowV2Execution> comparator = Comparator.comparingInt(WorkflowV2Execution::getTotalActions);
            final Comparator<WorkflowV2Execution> executionComparator = WorkflowAttributesV2.DESC.equalsIgnoreCase(sortType)
                    ? comparator.reversed() : comparator;

            return executions.stream().sorted(executionComparator).toList();
        }
        return executions;
    }

    /**
     * Returns the total count.
     *
     * @param orgId  The organization ID.
     * @param filter The filter.
     * @return The total count.
     */
    public long getTotalCount(final long orgId, final WorkflowExecutionHistoryFilter filter) {
        final Table executionTable = getExecutionTable();
        final Table versionTable = getVersionTable();
        final CountQuery countQuery = CountQuery.get(executionTable);
        final WhereClause whereClause = terminalStatusClause(executionTable);
        final WhereClause filterClause = v2HistoryFilterClause(filter);

        countQuery.addJoinClause(new JoinClause(executionTable.getColumn(OMWFLEXECUTIONV2.VERSION_ID), versionTable.getPKColumn()));
        countQuery.addJoinClause(new JoinClause(versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), getWorkflowTable().getPKColumn()));
        countQuery.setWhereClause(Objects.isNull(filterClause) ? whereClause : whereClause.and(filterClause));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Checks if the execution exists.
     *
     * @param orgId       The organization ID.
     * @param executionId The execution ID.
     * @return true if the execution exists, false otherwise.
     */
    public boolean exists(final long orgId, final long executionId) {
        final Table executionTable = getExecutionTable();
        final CountQuery countQuery = CountQuery.get(executionTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(executionTable.getPKColumn(), executionId)));
        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Returns the execution IDs.
     *
     * @param orgId     The organization ID.
     * @param start     The start.
     * @param limit     The limit.
     * @param sortField The sort field.
     * @param sortType  The sort type.
     * @param filter    The filter.
     * @return The execution IDs.
     */
    private Collection<Long> getExecutionIds(final long orgId, final int start, final int limit, final String sortField,
                                             final String sortType, final WorkflowExecutionHistoryFilter filter) {
        final Table executionTable = getExecutionTable();
        final SelectQuery selectQuery = SelectQuery.get(executionTable);
        final WhereClause whereClause = terminalStatusClause(executionTable);
        final WhereClause filterClause = v2HistoryFilterClause(filter);

        selectQuery.addJoinClause(new JoinClause(executionTable.getColumn(OMWFLEXECUTIONV2.VERSION_ID), getVersionTable().getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(getVersionTable().getColumn(OMWFLVERSION.WORKFLOW_ID), getWorkflowTable().getPKColumn()));
        selectQuery.setWhereClause(Objects.isNull(filterClause) ? whereClause : whereClause.and(filterClause));
        applySortCriteria(sortField, sortType, selectQuery);
        selectQuery.setLimitClause(new LimitClause(start, limit));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRows(executionTable).map(Row::getPKValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Applies the sort criteria.
     *
     * @param sortField   The sort field.
     * @param sortType    The sort type.
     * @param selectQuery The select query.
     */
    private void applySortCriteria(final String sortField, final String sortType, final SelectQuery selectQuery) {
        final Table executionTable = getExecutionTable();
        final Table workflowTable = getWorkflowTable();
        final OrderByClause.OrderType orderType = WorkflowAttributes.DESC.equalsIgnoreCase(sortType)
                ? OrderByClause.OrderType.DESCENDING : OrderByClause.OrderType.ASCENDING;
        final Consumer<Column> consumer = (column) -> selectQuery.addOrderByClause(new OrderByClause(column, orderType));

        switch (sortField) {
            case WorkflowAttributesV2.WORKFLOW_NAME -> consumer.accept(workflowTable.getColumn(OMWFLWORKFLOWV2.NAME));
            case WorkflowAttributesV2.TRIGGER_SOURCE ->
                    consumer.accept(workflowTable.getColumn(OMWFLWORKFLOWV2.TRIGGER_SOURCE));
            case WorkflowAttributesV2.STATUS -> consumer.accept(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS));
            default -> consumer.accept(executionTable.getColumn(OMWFLEXECUTIONV2.STARTED_TIME));
        }
    }

    /**
     * Returns the terminal status clause.
     *
     * @param executionTable The execution table.
     * @return The terminal status clause.
     */
    private WhereClause terminalStatusClause(final Table executionTable) {
        return new WhereClause(Criteria.notEq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.IN_PROGRESS.getId()));
    }

    /**
     * Returns the V2 history filter clause.
     *
     * @param filter The filter.
     * @return The V2 history filter clause.
     */
    private WhereClause v2HistoryFilterClause(final WorkflowExecutionHistoryFilter filter) {
        return workflowFilterManager.buildExecutionHistoryFilterWhereClause(filter);
    }

    /**
     * Converts the execution to a WorkflowV2Execution object.
     *
     * @param executionRow  The execution row.
     * @param dataContainer The data container.
     * @param includeNodes  Whether to include nodes.
     * @return The WorkflowV2Execution object.
     */
    private WorkflowV2Execution toExecution(final Row executionRow, final DataContainer dataContainer, final boolean includeNodes) {
        final Table workflowTable = getWorkflowTable();
        final Table versionTable = getVersionTable();
        final Table nodeTable = getNodeTable();
        final Long orgId = executionRow.getOrgId();
        final WorkflowV2Execution execution = new WorkflowV2Execution();
        final Row versionRow = dataContainer.getRow(versionTable,
                new WhereClause(Criteria.eq(versionTable.getPKColumn(), executionRow.get(OMWFLEXECUTIONV2.VERSION_ID))));
        final Row workflowRow = Objects.isNull(versionRow) ? null : dataContainer.getRow(workflowTable,
                new WhereClause(Criteria.eq(workflowTable.getPKColumn(), versionRow.get(OMWFLVERSION.WORKFLOW_ID))));

        execution.setId(executionRow.getPKValue());
        execution.setOrgId(orgId);
        execution.setUserId(executionRow.get(OMWFLEXECUTIONV2.USER_ID));
        execution.setVersionId(executionRow.get(OMWFLEXECUTIONV2.VERSION_ID));
        execution.setStatus(ExecutionStatus.get(executionRow.get(OMWFLEXECUTIONV2.STATUS)));
        execution.setStartedTime(executionRow.get(OMWFLEXECUTIONV2.STARTED_TIME));
        execution.setCompletedTime(executionRow.get(OMWFLEXECUTIONV2.COMPLETED_TIME));

        if (Objects.nonNull(workflowRow)) {
            execution.setWorkflowName(workflowRow.get(OMWFLWORKFLOWV2.NAME));
            execution.setTriggerSource(TriggerSource.get(workflowRow.get(OMWFLWORKFLOWV2.TRIGGER_SOURCE)));
            execution.setCrmUser(userService.getUser(orgId, workflowRow.get(OMWFLWORKFLOWV2.OWNER_ID)));
        }

        if (Objects.nonNull(versionRow)) {
            execution.setWorkflowId(versionRow.get(OMWFLVERSION.WORKFLOW_ID));
        }

        final long actionCount = dataContainer.getRows(nodeTable, new WhereClause(Criteria.eq(nodeTable.getColumn(OMWFLNODE.VERSION_ID), execution.getVersionId()))
                .and(Criteria.eq(nodeTable.getColumn(OMWFLNODE.NODE_TYPE), NodeType.ACTION.getId()))).count();

        execution.setTotalActions((int) actionCount);

        if (includeNodes) {
            final Table executionNodeTable = getExecutionNodeTable();
            final Collection<WorkflowV2ExecutionNode> nodes = new ArrayList<>();

            dataContainer.getRows(executionNodeTable, new WhereClause(Criteria.eq(executionNodeTable.getColumn(OMWFLEXECUTIONNODE.EXECUTION_ID), execution.id())))
                    .forEach(nodeRow -> {
                        final WorkflowV2ExecutionNode node = new WorkflowV2ExecutionNode();
                        final Row definitionRow = dataContainer.getRow(nodeTable, new WhereClause(Criteria.eq(nodeTable.getPKColumn(), nodeRow.get(OMWFLEXECUTIONNODE.NODE_ID))));

                        node.setId(nodeRow.getPKValue());
                        node.setOrgId(orgId);
                        node.setExecutionId(execution.id());
                        node.setNodeId(nodeRow.get(OMWFLEXECUTIONNODE.NODE_ID));
                        node.setStatus(NodeExecutionStatus.get(nodeRow.get(OMWFLEXECUTIONNODE.STATUS)));
                        node.setStartedTime(nodeRow.get(OMWFLEXECUTIONNODE.STARTED_TIME));
                        node.setCompletedTime(nodeRow.get(OMWFLEXECUTIONNODE.COMPLETED_TIME));
                        node.setFailureReason(nodeRow.get(OMWFLEXECUTIONNODE.FAILURE_REASON));

                        if (Objects.nonNull(definitionRow)) {
                            node.setNodeType(NodeType.get(definitionRow.get(OMWFLNODE.NODE_TYPE)));
                        }
                        nodes.add(node);
                    });
            execution.setExecutionNodes(nodes);
        }
        return execution;
    }

    /**
     * Executes the updates.
     *
     * @param orgId         The organization ID.
     * @param updateQueries The update queries.
     */
    private void executeUpdates(final long orgId, final List<UpdateQuery> updateQueries) {
        if (updateQueries.isEmpty()) {
            return;
        }
        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * Commits the changes.
     *
     * @param orgId         The organization ID.
     * @param dataContainer The data container.
     */
    private void commitChanges(final long orgId, final DataContainer dataContainer) {
        if (Objects.isNull(dataContainer) || dataContainer.isEmpty()) {
            return;
        }
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * Builds the update node status query.
     *
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     * @param status      The status.
     * @param reason      The reason.
     * @return The update node status query.
     */
    private UpdateQuery buildUpdateNodeStatusQuery(final long executionId, final long nodeId,
                                                   final NodeExecutionStatus status, final Short reason) {
        final Table nodeTable = getExecutionNodeTable();
        final UpdateQuery updateQuery = UpdateQuery.get(nodeTable);

        updateQuery.addUpdateColumn(nodeTable.getColumn(OMWFLEXECUTIONNODE.STATUS), status.getId());
        updateQuery.addUpdateColumn(nodeTable.getColumn(OMWFLEXECUTIONNODE.COMPLETED_TIME), Instant.now().toEpochMilli());

        if (Objects.nonNull(reason)) {
            updateQuery.addUpdateColumn(nodeTable.getColumn(OMWFLEXECUTIONNODE.FAILURE_REASON), reason);
        }
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(nodeTable.getColumn(OMWFLEXECUTIONNODE.EXECUTION_ID), executionId))
                .and(Criteria.eq(nodeTable.getColumn(OMWFLEXECUTIONNODE.NODE_ID), nodeId)));

        return updateQuery;
    }

    /**
     * Builds the update current node query.
     *
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     * @param status      The status.
     * @return The update current node query.
     */
    private UpdateQuery buildUpdateCurrentNodeQuery(final long executionId, final Long nodeId, final ExecutionStatus status) {
        final Table executionTable = getExecutionTable();
        final UpdateQuery updateQuery = UpdateQuery.get(executionTable);

        if (Objects.nonNull(status)) {
            updateQuery.addUpdateColumn(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), status.getId());

            if (status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED || status == ExecutionStatus.STOPPED) {
                updateQuery.addUpdateColumn(executionTable.getColumn(OMWFLEXECUTIONV2.COMPLETED_TIME), Instant.now().toEpochMilli());
            }
        }
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(executionTable.getPKColumn(), executionId)));
        return updateQuery;
    }

    /**
     * Builds the cancel schedules query.
     *
     * @param executionId The execution ID.
     * @return The cancel schedules query.
     */
    private UpdateQuery buildCancelSchedulesQuery(final long executionId) {
        final Table scheduleTable = getScheduleTable();
        final UpdateQuery updateQuery = UpdateQuery.get(scheduleTable);

        updateQuery.addUpdateColumn(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.CANCELLED), true);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.EXECUTION_ID), executionId))
                .and(Criteria.eq(scheduleTable.getColumn(OMWFLEXECUTIONSCHEDULER.CANCELLED), false)));
        return updateQuery;
    }

    /**
     * Builds the cancel waits query.
     *
     * @param executionId The execution ID.
     * @return The cancel waits query.
     */
    private UpdateQuery buildCancelWaitsQuery(final long executionId) {
        final Table waitTable = getWaitTable();
        final UpdateQuery updateQuery = UpdateQuery.get(waitTable);

        updateQuery.addUpdateColumn(waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.CANCELLED), true);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.EXECUTION_ID), executionId))
                .and(Criteria.eq(waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.CANCELLED), false)));
        return updateQuery;
    }

    /**
     * Returns the open waits where clause.
     *
     * @param waitTable      The wait table.
     * @param executionTable The execution table.
     * @return The open waits where clause.
     */
    private WhereClause openWaitsWhereClause(final Table waitTable, final Table executionTable) {
        return new WhereClause(Criteria.eq(waitTable.getColumn(OMWFLEXECUTIONWAITREGISTRATION.CANCELLED), false))
                .and(Criteria.eq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.PENDING.getId()));
    }

    /**
     * Maps the open waits.
     *
     * @param dataContainer  The data container.
     * @param waitTable      The wait table.
     * @param executionTable The execution table.
     * @return The open waits.
     */
    private Collection<WaitRegistration> mapOpenWaits(final DataContainer dataContainer, final Table waitTable,
                                                      final Table executionTable) {
        final Collection<WaitRegistration> waits = new ArrayList<>();

        dataContainer.getRows(waitTable).forEach(waitRow -> {
            final Row executionRow = dataContainer.getRow(executionTable, new WhereClause(
                    Criteria.eq(executionTable.getPKColumn(), waitRow.get(OMWFLEXECUTIONWAITREGISTRATION.EXECUTION_ID))));

            if (Objects.nonNull(executionRow)) {
                waits.add(new WaitRegistration(
                        waitRow.getOrgId(),
                        waitRow.get(OMWFLEXECUTIONWAITREGISTRATION.EXECUTION_ID),
                        waitRow.get(OMWFLEXECUTIONWAITREGISTRATION.NODE_ID),
                        waitRow.get(OMWFLEXECUTIONWAITREGISTRATION.SOURCE_NODE_ID),
                        EventContextType.get(waitRow.get(OMWFLEXECUTIONWAITREGISTRATION.CONTEXT_TYPE)),
                        executionRow.get(OMWFLEXECUTIONV2.VERSION_ID)));
            }
        });
        return waits;
    }

    /**
     * Builds the cancel open schedules and waits queries.
     *
     * @param executionId The execution ID.
     * @return The cancel open schedules and waits queries.
     */
    private List<UpdateQuery> buildCancelOpenSchedulesAndWaitsQueries(final long executionId) {
        final List<UpdateQuery> queries = new ArrayList<>(2);

        queries.add(buildCancelSchedulesQuery(executionId));
        queries.add(buildCancelWaitsQuery(executionId));
        return queries;
    }

    /**
     * Builds the fail fast queries.
     *
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     * @param reason      The reason.
     * @return The fail fast queries.
     */
    private List<UpdateQuery> buildFailFastQueries(final long executionId, final Long nodeId, final Short reason) {
        final List<UpdateQuery> queries = new ArrayList<>(4);

        if (Objects.nonNull(nodeId)) {
            queries.add(buildUpdateNodeStatusQuery(executionId, nodeId, NodeExecutionStatus.FAILED, reason));
        }
        queries.add(buildUpdateCurrentNodeQuery(executionId, nodeId, ExecutionStatus.FAILED));
        queries.addAll(buildCancelOpenSchedulesAndWaitsQueries(executionId));
        return queries;
    }

    /**
     * Builds the expire wait queries.
     *
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     * @return The expire wait queries.
     */
    private List<UpdateQuery> buildExpireWaitQueries(final long executionId, final long nodeId) {
        final List<UpdateQuery> queries = new ArrayList<>(4);

        queries.add(buildUpdateNodeStatusQuery(executionId, nodeId, NodeExecutionStatus.EXPIRED, null));
        queries.add(buildUpdateCurrentNodeQuery(executionId, nodeId, ExecutionStatus.FAILED));
        queries.addAll(buildCancelOpenSchedulesAndWaitsQueries(executionId));
        return queries;
    }

    /**
     * Builds the complete node and mark running queries.
     *
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     * @return The complete node and mark running queries.
     */
    private List<UpdateQuery> buildCompleteNodeAndMarkRunningQueries(final long executionId, final long nodeId) {
        final List<UpdateQuery> queries = new ArrayList<>(2);

        queries.add(buildUpdateNodeStatusQuery(executionId, nodeId, NodeExecutionStatus.COMPLETED, null));
        queries.add(buildUpdateCurrentNodeQuery(executionId, null, ExecutionStatus.IN_PROGRESS));
        return queries;
    }

    /**
     * Builds the complete wait and resume queries.
     *
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     * @return The complete wait and resume queries.
     */
    private List<UpdateQuery> buildCompleteWaitAndResumeQueries(final long executionId, final long nodeId) {
        final List<UpdateQuery> queries = new ArrayList<>(4);

        queries.addAll(buildCancelOpenSchedulesAndWaitsQueries(executionId));
        queries.add(buildUpdateNodeStatusQuery(executionId, nodeId, NodeExecutionStatus.COMPLETED, null));
        queries.add(buildUpdateCurrentNodeQuery(executionId, null, ExecutionStatus.IN_PROGRESS));
        return queries;
    }

    /**
     * Returns the execution row.
     *
     * @param orgId       The organization ID.
     * @param executionId The execution ID.
     * @return The execution row.
     */
    private Row getExecutionRow(final long orgId, final long executionId) {
        final Table executionTable = getExecutionTable();
        final SelectQuery selectQuery = SelectQuery.get(executionTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(executionTable.getPKColumn(), executionId)));
        return orgDataStore(orgId).get(selectQuery).getRow(executionTable);
    }

    /**
     * Returns the execution node row.
     *
     * @param orgId       The organization ID.
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     * @return The execution node row.
     */
    private Row getExecutionNodeRow(final long orgId, final long executionId, final long nodeId) {
        final Table nodeTable = getExecutionNodeTable();
        final SelectQuery selectQuery = SelectQuery.get(nodeTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(nodeTable.getColumn(OMWFLEXECUTIONNODE.EXECUTION_ID), executionId))
                .and(Criteria.eq(nodeTable.getColumn(OMWFLEXECUTIONNODE.NODE_ID), nodeId)));
        return orgDataStore(orgId).get(selectQuery).getRow(nodeTable);
    }

    /**
     * Applies the pending node changes.
     *
     * @param container   The data container.
     * @param orgId       The organization ID.
     * @param executionId The execution ID.
     * @param nodeId      The node ID.
     */
    private void applyPendingNodeChanges(final DataContainer container, final long orgId, final long executionId, final long nodeId) {
        final Row nodeRow = getExecutionNodeRow(orgId, executionId, nodeId);
        final long now = Instant.now().toEpochMilli();

        nodeRow.set(OMWFLEXECUTIONNODE.STATUS, NodeExecutionStatus.PENDING.getId());
        nodeRow.set(OMWFLEXECUTIONNODE.COMPLETED_TIME, now);
        container.updateRow(nodeRow);

        final Row executionRow = getExecutionRow(orgId, executionId);

        executionRow.set(OMWFLEXECUTIONV2.STATUS, ExecutionStatus.PENDING.getId());
        container.updateRow(executionRow);
    }

    /**
     * Builds the schedule row.
     *
     * @param orgId        The organization ID.
     * @param executionId  The execution ID.
     * @param nodeId       The node ID.
     * @param scheduleType The schedule type.
     * @param fireAt       The fire at.
     * @return The schedule row.
     */
    private Row buildScheduleRow(final long orgId, final long executionId, final long nodeId,
                                 final ScheduleType scheduleType, final long fireAt) {
        final Row scheduleRow = new Row(getScheduleTable());

        scheduleRow.setOrgId(orgId);
        scheduleRow.set(OMWFLEXECUTIONSCHEDULER.EXECUTION_ID, executionId);
        scheduleRow.set(OMWFLEXECUTIONSCHEDULER.NODE_ID, nodeId);
        scheduleRow.set(OMWFLEXECUTIONSCHEDULER.SCHEDULE_TYPE, scheduleType.getId());
        scheduleRow.set(OMWFLEXECUTIONSCHEDULER.FIRE_AT, fireAt);
        scheduleRow.set(OMWFLEXECUTIONSCHEDULER.CANCELLED, false);
        return scheduleRow;
    }

    /**
     * Builds the wait registration row.
     *
     * @param orgId        The organization ID.
     * @param executionId  The execution ID.
     * @param waitNodeId   The wait node ID.
     * @param sourceNodeId The source node ID.
     * @param contextType  The context type.
     * @return The wait registration row.
     */
    private Row buildWaitRegistrationRow(final long orgId, final long executionId, final long waitNodeId,
                                         final long sourceNodeId, final EventContextType contextType) {
        final Row waitRow = new Row(getWaitTable());

        waitRow.setOrgId(orgId);
        waitRow.set(OMWFLEXECUTIONWAITREGISTRATION.EXECUTION_ID, executionId);
        waitRow.set(OMWFLEXECUTIONWAITREGISTRATION.NODE_ID, waitNodeId);
        waitRow.set(OMWFLEXECUTIONWAITREGISTRATION.SOURCE_NODE_ID, sourceNodeId);
        waitRow.set(OMWFLEXECUTIONWAITREGISTRATION.CONTEXT_TYPE, contextType.getId());
        waitRow.set(OMWFLEXECUTIONWAITREGISTRATION.CANCELLED, false);

        return waitRow;
    }

    /**
     * Fetches the count of terminal executions older than the retention window.
     *
     * @return obsolete execution count
     */
    public long getObsoleteExecutionCount() {
        final CountQuery countQuery = CountQuery.get(getExecutionTable());

        countQuery.setWhereClause(getObsoleteExecutionWhereClause());
        return writableDataStore().get(countQuery);
    }

    /**
     * Retrieves a page of obsolete terminal executions for cleanup.
     *
     * @param start lower bound offset
     * @param limit page size
     * @return page of executions with id and orgId set
     */
    public Page<WorkflowV2Execution> getObsoleteExecutionHistoryPage(final int start, final int limit) {
        final Table executionTable = getExecutionTable();
        final SelectQuery selectQuery = SelectQuery.get(executionTable);

        selectQuery.addSelectClause(List.of(executionTable.getPKColumn(), executionTable.getColumn(OMWFLEXECUTIONV2.ORG_ID)));
        selectQuery.setWhereClause(getObsoleteExecutionWhereClause());
        selectQuery.setLimitClause(new LimitClause(start, limit));

        final DataContainer dataContainer = writableDataStore().get(selectQuery);
        final Page<WorkflowV2Execution> executionPage = new Page<>(start);

        if (!dataContainer.isEmpty()) {
            executionPage.setData(dataContainer.getRows(executionTable).map(row -> {
                final WorkflowV2Execution execution = new WorkflowV2Execution();

                execution.setId(row.getPKValue());
                execution.setOrgId(row.get(OMWFLEXECUTIONV2.ORG_ID));

                return execution;
            }).toList());
        }

        return executionPage;
    }

    /**
     * Deletes obsolete executions and related email tracker rows.
     *
     * @param executions executions to remove (id and orgId required)
     */
    public void deleteObsoleteExecutions(final Collection<WorkflowV2Execution> executions) {
        final Collection<Long> executionIds = executions.stream().map(WorkflowV2Execution::id).collect(Collectors.toUnmodifiableSet());
        final List<DeleteQuery> deleteQueries = new ArrayList<>();
        final Table table = getExecutionTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), executionIds)));
        deleteQueries.add(deleteQuery);

        final Table emailTrackerTable = workflowEmailTrackerManager.getTable();
        final DeleteQuery emailTrackerDeleteQuery = DeleteQuery.get(emailTrackerTable);

        emailTrackerDeleteQuery.setWhereClause(new WhereClause(Criteria.in(emailTrackerTable.getColumn(OMWFLEMAILTRACKER.EXECUTION_ID), executionIds)));
        deleteQueries.add(emailTrackerDeleteQuery);

        writableDataStore().delete(deleteQueries);
    }

    /**
     * Returns the where clause for the obsolete executions.
     *
     * @return The where clause for the obsolete executions.
     */
    private WhereClause getObsoleteExecutionWhereClause() {
        final Table executionTable = getExecutionTable();
        final long cutoff = Instant.now().minus(15, ChronoUnit.DAYS).toEpochMilli();

        return new WhereClause(Criteria.lt(executionTable.getColumn(OMWFLEXECUTIONV2.STARTED_TIME), cutoff))
                .and(Criteria.in(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), List.of(
                        ExecutionStatus.SUCCESS.getId(),
                        ExecutionStatus.FAILED.getId(),
                        ExecutionStatus.STOPPED.getId())));
    }
}
