package com.tlc.crm.workflow.v2.api.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;

import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.model.filter.WorkflowFilter;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.sql.resource.OMWFLWORKFLOW;
import com.tlc.crm.workflow.v2.api.model.ConditionConfiguration;
import com.tlc.crm.workflow.v2.api.model.CustomSpanUnit;
import com.tlc.crm.workflow.v2.api.model.DelayConfiguration;
import com.tlc.crm.workflow.v2.api.model.EdgeType;
import com.tlc.crm.workflow.v2.api.model.EventContextType;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.IfElseConfiguration;
import com.tlc.crm.workflow.v2.api.model.NodeEvent;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.PredefinedSpanUnit;
import com.tlc.crm.workflow.v2.api.model.SpanConfiguration;
import com.tlc.crm.workflow.v2.api.model.SpanType;
import com.tlc.crm.workflow.v2.api.model.WorkflowListDTO;
import com.tlc.crm.workflow.v2.api.model.WorkflowNodeSupport;
import com.tlc.crm.workflow.v2.api.model.WorkflowVersion;
import com.tlc.crm.workflow.v2.api.model.TriggerScope;
import com.tlc.crm.workflow.v2.api.model.TriggerConfiguration;
import com.tlc.crm.workflow.v2.api.model.VersionStatus;
import com.tlc.crm.workflow.v2.api.model.WaitForConditionConfiguration;
import com.tlc.crm.workflow.v2.api.model.WorkflowNodeEdge;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;
import com.tlc.crm.workflow.v2.sql.resource.MWFLCRITERIAVALUEV2;
import com.tlc.crm.workflow.v2.sql.resource.MWFLFIELDCRITERIAV2;
import com.tlc.crm.workflow.v2.sql.resource.MWFLSOURCEFIELDCRITERIAV2;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLACTIONSV2;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLCRITERIASETV2;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLCRITERIAV2;
import com.tlc.crm.workflow.v2.sql.resource.MWFLNODEEDGE;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLNODEEVENT;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLNODESPANCONFIG;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLNODE;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLTRIGGERUSERS;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLVERSION;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLWORKFLOWV2;

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

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages workflow V2 CRUD and status updates.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2Manager extends AbstractConfigManager<WorkflowV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2Manager.class);

    private final WorkflowComparatorService comparatorService;
    private final TriggerActionSourceService triggerActionSourceService;
    private final TriggerActionEventService triggerActionEventService;
    private final WorkflowFilterManager workflowFilterManager;
    private final WorkflowV2ExecutionManager workflowExecutionManager;

    private WorkflowV2Manager() {
        this.comparatorService = WorkflowComparatorService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
        this.workflowExecutionManager = WorkflowV2ExecutionManager.getInstance();
    }

    private static class Instance {
        private static final WorkflowV2Manager INSTANCE = new WorkflowV2Manager();
    }

    public static WorkflowV2Manager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the table for the workflow V2.
     *
     * @return the table for the workflow V2
     */
    @Override
    public Table getTable() {
        return Table.get(OMWFLWORKFLOWV2.TABLE);
    }

    /**
     * Returns the table for the workflow version.
     *
     * @return the table for the workflow version
     */
    private Table getVersionTable() {
        return Table.get(OMWFLVERSION.TABLE);
    }

    /**
     * Returns the table for the trigger users.
     *
     * @return the table for the trigger users
     */
    private Table getTriggerUsersTable() {
        return Table.get(OMWFLTRIGGERUSERS.TABLE);
    }

    /**
     * Returns the table for the nodes.
     *
     * @return the table for the nodes
     */
    private Table getNodeTable() {
        return Table.get(OMWFLNODE.TABLE);
    }

    /**
     * Returns the table for the edges.
     *
     * @return the table for the edges
     */
    private Table getEdgeTable() {
        return Table.get(MWFLNODEEDGE.TABLE);
    }

    /**
     * Returns the table for the span configurations.
     *
     * @return the table for the span configurations
     */
    private Table getSpanConfigTable() {
        return Table.get(OMWFLNODESPANCONFIG.TABLE);
    }

    /**
     * Returns the table for the node events.
     *
     * @return the table for the node events
     */
    private Table getNodeEventTable() {
        return Table.get(OMWFLNODEEVENT.TABLE);
    }

    /**
     * Returns the table for the actions.
     *
     * @return the table for the actions
     */
    private Table getActionTable() {
        return Table.get(OMWFLACTIONSV2.TABLE);
    }

    /**
     * Returns the table for the criteria sets.
     *
     * @return the table for the criteria sets
     */
    private Table getCriteriaSetTable() {
        return Table.get(OMWFLCRITERIASETV2.TABLE);
    }

    /**
     * Returns the table for the criteria.
     *
     * @return the table for the criteria
     */
    private Table getCriteriaTable() {
        return Table.get(OMWFLCRITERIAV2.TABLE);
    }

    /**
     * Returns the table for the criteria values.
     *
     * @return the table for the criteria values
     */
    private Table getCriteriaValueTable() {
        return Table.get(MWFLCRITERIAVALUEV2.TABLE);
    }

    /**
     * Returns the table for the source field criteria.
     *
     * @return the table for the source field criteria
     */
    private Table getSourceFieldCriteriaTable() {
        return Table.get(MWFLSOURCEFIELDCRITERIAV2.TABLE);
    }

    /**
     * Returns the table for the field criteria.
     *
     * @return the table for the field criteria
     */
    private Table getFieldCriteriaTable() {
        return Table.get(MWFLFIELDCRITERIAV2.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId the organization id
     * @param ids   the ids
     * @return the workflow V2s for the given organization and ids
     */
    @Override
    public Collection<WorkflowV2> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final DataContainer dataContainer = getDataContainer(orgId, ids);

        return dataContainer.getRows(table).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * Deletes the workflow V2 for the given organization and id.
     *
     * @param orgId                  the organization id
     * @param workflowId             the workflow id
     * @param cancelPendingExecution the flag to cancel pending execution
     */
    public void delete(final Long orgId, final Long workflowId, final boolean cancelPendingExecution) {
        if (cancelPendingExecution) {
            workflowExecutionManager.cancelPendingByVersionIds(orgId, getAllVersionIds(orgId, workflowId));
        }
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOWV2.IS_DELETED), true);
        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOWV2.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), workflowId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId the organization id
     * @param ids   the ids
     * @return the workflow V2s for the given organization and ids
     */
    @Override
    public Collection<WorkflowV2> partialGet(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    /**
     * Creates a new workflow V2.
     *
     * @param workflow the workflow V2 to create
     * @return the created workflow V2
     */
    @Override
    public WorkflowV2 create(final WorkflowV2 workflow) {
        final DataContainer dataContainer = DataContainer.create();

        loadWorkflowV2DetailsIntoContainer(true, workflow, dataContainer);
        orgDataStore(workflow.orgId()).commitChanges(dataContainer);

        return workflow;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     the organization id
     * @param workflows the workflows to update
     * @return the process outcome
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<WorkflowV2> workflows) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> ids = workflows.stream().map(WorkflowV2::id).filter(Objects::nonNull).toList();
        final DataContainer entityContainer = getExistingDataContainer(orgId, ids);
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();

        workflows.forEach(workflow -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadWorkflowV2DetailsIntoContainer(false, workflow, entityContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);

                if (workflow.isCancelPendingExecution()) {
                    workflowExecutionManager.cancelPendingByVersionIds(orgId, getAllVersionIds(orgId, workflow.id()));
                }
                processOutcome.addProcessedRecord(workflow.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Workflow V2 update failed Org ID: {} ID: {} reason: {}", orgId, workflow.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, workflow.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * Updates the status of the workflow V2.
     *
     * @param orgId    Refers the organization id.
     * @param id       Refers the workflow id.
     * @param isActive Refers the workflow status to be changed.
     */
    public void updateStatus(final long orgId, final long id, final boolean isActive, final boolean cancelPendingExecution) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOW.IS_ACTIVE), isActive);
        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOW.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        orgDataStore(orgId).update(updateQuery);

        if (cancelPendingExecution) {
            workflowExecutionManager.cancelPendingByVersionIds(orgId, getAllVersionIds(orgId, id));
        }
    }

    /**
     * Gets the workflow V2 list.
     *
     * @param orgId          the organization id
     * @param start          the start
     * @param limit          the limit
     * @param sortField      the sort field
     * @param sortType       the sort type
     * @param workflowFilter the workflow filter
     * @return the workflow V2 list
     */
    public Collection<WorkflowListDTO> getWorkflowV2List(final long orgId, final int start, final int limit,
                                                         final String sortField, final String sortType,
                                                         final WorkflowFilter workflowFilter) {
        final Collection<Long> workflowIds = getWorkflowIds(orgId, start, limit, sortField, sortType, workflowFilter);

        if (!workflowIds.isEmpty()) {
            final Table table = getTable();
            final Table nodeTable = getNodeTable();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), workflowIds))
                    .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.IS_DELETED), false))
                    .and(Criteria.eq(nodeTable.getColumn(OMWFLNODE.NODE_TYPE), NodeType.ACTION.getId()))
                    .and(getPublishedVersionCriteria()));
            selectQuery.addSelectClause(table, getVersionTable(), nodeTable);
            selectQuery.addJoinClause(getListFilterJoinClauses());
            applySortCriteria(sortField, sortType, selectQuery);

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
            final Collection<WorkflowListDTO> workflows = dataContainer.getRows(table).map(row -> convertListRowToModel(row, dataContainer)).toList();

            if (Objects.equals(WorkflowAttributesV2.ACTIONS, sortField)) {
                final Comparator<WorkflowListDTO> comparator = Comparator.comparingInt(WorkflowListDTO::getTotalActions);
                final Comparator<WorkflowListDTO> workflowComparator = WorkflowAttributesV2.DESC.equalsIgnoreCase(sortType)
                        ? comparator.reversed() : comparator;

                return workflows.stream().sorted(workflowComparator).toList();
            }
            return workflows;
        }

        return Collections.emptyList();
    }

    /**
     * Gets the workflow V2 count.
     *
     * @param orgId          the organization id
     * @param workflowFilter the workflow filter
     * @return the workflow V2 count
     */
    public long getWorkflowV2Count(final long orgId, final WorkflowFilter workflowFilter) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.ORG_ID), orgId)).and(getPublishedVersionCriteria());
        final WhereClause filterWhereClause = Objects.isNull(workflowFilter) ? null : workflowFilterManager.buildWorkflowFilterWhereClause(workflowFilter);
        final Column workflowPkColumn = table.getPKColumn();

        selectQuery.addSelectClause(workflowPkColumn.distinct());
        selectQuery.addJoinClause(getListFilterJoinClauses());
        selectQuery.setWhereClause(Objects.isNull(filterWhereClause) ? whereClause : whereClause.and(filterWhereClause));
        countQuery.setWhereClause(new WhereClause(Criteria.in(workflowPkColumn, selectQuery))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.IS_DELETED), false)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets the workflow V2 ids.
     *
     * @param orgId          the organization id
     * @param start          the start
     * @param limit          the limit
     * @param sortField      the sort field
     * @param sortType       the sort type
     * @param workflowFilter the workflow filter
     * @return the workflow V2 ids
     */
    private Collection<Long> getWorkflowIds(final long orgId, final int start, final int limit, final String sortField,
                                            final String sortType, final WorkflowFilter workflowFilter) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final WhereClause filterWhereClause = Objects.isNull(workflowFilter) ? null : workflowFilterManager.buildWorkflowFilterWhereClause(workflowFilter);

        selectQuery.addJoinClause(getListFilterJoinClauses());
        selectQuery.addSelectClause(table.getPKColumn().distinct());
        selectQuery.setLimitClause(new LimitClause(start, limit));
        applySortCriteria(sortField, sortType, selectQuery);

        WhereClause whereClause = new WhereClause(getPublishedVersionCriteria());

        if (Objects.nonNull(filterWhereClause)) {
            whereClause = whereClause.and(filterWhereClause);
        }
        selectQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(selectQuery).getRows(table).filter(Objects::nonNull).map(Row::getPKValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets the list filter join clauses.
     *
     * @return the list filter join clauses
     */
    private List<JoinClause> getListFilterJoinClauses() {
        final Table workflowTable = getTable();
        final Table versionTable = getVersionTable();
        final Table nodeTable = getNodeTable();
        final Table actionTable = getActionTable();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        return List.of(new JoinClause(workflowTable.getPKColumn(), versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), leftJoin),
                new JoinClause(versionTable.getPKColumn(), nodeTable.getColumn(OMWFLNODE.VERSION_ID), leftJoin),
                new JoinClause(nodeTable.getPKColumn(), actionTable.getColumn(OMWFLACTIONSV2.NODE_ID), leftJoin)
        );
    }

    /**
     * Gets the published version criteria.
     *
     * @return the published version criteria
     */
    private Criteria getPublishedVersionCriteria() {
        return Criteria.eq(getVersionTable().getColumn(OMWFLVERSION.STATUS), VersionStatus.PUBLISHED.getId());
    }

    /**
     * Loads the workflow V2 details into the container.
     *
     * @param isCreateEvent the flag to indicate if the workflow is being created
     * @param workflow      the workflow V2
     * @param dataContainer the data container
     */
    private void loadWorkflowV2DetailsIntoContainer(final boolean isCreateEvent, final WorkflowV2 workflow,
                                                    final DataContainer dataContainer) {
        final DataContainer existingContainer = isCreateEvent ? null : getExistingDataContainer(workflow.orgId(), List.of(workflow.id()));

        loadWorkflowV2DetailsIntoContainer(isCreateEvent, workflow, existingContainer, dataContainer);
    }

    /**
     * Loads the workflow V2 details into the container.
     *
     * @param isCreateEvent     the flag to indicate if the workflow is being created
     * @param workflow          the workflow V2
     * @param existingContainer the existing data container
     * @param dataContainer     the data container
     */
    private void loadWorkflowV2DetailsIntoContainer(final boolean isCreateEvent, final WorkflowV2 workflow,
                                                    final DataContainer existingContainer, final DataContainer dataContainer) {
        final long currentTime = Instant.now().toEpochMilli();
        final Table workflowTable = getTable();
        final Long workflowId = workflow.id();
        final Long orgId = workflow.orgId();
        final Row workflowRow = isCreateEvent ? new Row(workflowTable) : existingContainer.getRow(workflowTable, workflowId);

        if (!isCreateEvent && Objects.isNull(workflowRow)) {
            throw ErrorCode.get(WorkflowV2ErrorCodes.WORKFLOW_NOT_FOUND, "i18n.workflow.not.found");
        }

        if (workflowRow.isNewRow()) {
            workflowRow.setOrgId(orgId);
            workflow.setId(workflowRow.getPKValue());
            workflowRow.set(OMWFLWORKFLOWV2.CREATED_TIME, currentTime);
        }
        final TriggerConfiguration triggerConfiguration = WorkflowNodeSupport.configurationAs(workflow.getNodesAsCollection(), TriggerConfiguration.class);

        workflowRow.set(OMWFLWORKFLOWV2.NAME, workflow.getName());
        workflowRow.set(OMWFLWORKFLOWV2.DESCRIPTION, workflow.getDescription());
        workflowRow.set(OMWFLWORKFLOWV2.OWNER_ID, workflow.getOwnerId());
        workflowRow.set(OMWFLWORKFLOWV2.TRIGGER_SCOPE, triggerConfiguration.getTriggerScope().getId());
        workflowRow.set(OMWFLWORKFLOWV2.TRIGGER_EVENT, triggerConfiguration.getTriggerEvent().getId());
        workflowRow.set(OMWFLWORKFLOWV2.TRIGGER_SOURCE, triggerConfiguration.getTriggerSource().getId());
        workflowRow.set(OMWFLWORKFLOWV2.IS_ACTIVE, workflow.isActive());
        workflowRow.set(OMWFLWORKFLOWV2.MODIFIED_TIME, currentTime);
        dataContainer.updateRow(workflowRow);

        final Long versionId = loadVersionIntoContainer(isCreateEvent, workflow, existingContainer, dataContainer, currentTime);

        if (isCreateEvent || workflow.isGraphUpdate()) {
            loadTriggerUsersIntoContainer(workflow, triggerConfiguration, dataContainer);
        }

        final Map<String, Long> clientKeyToId = loadNodesIntoContainer(workflow, versionId, dataContainer);

        loadEdgesIntoContainer(workflow, clientKeyToId, dataContainer);

        for (final WorkflowNode node : workflow.getNodesAsCollection()) {
            final Long nodeId = clientKeyToId.get(node.getClientKey());

            node.setId(nodeId);

            switch (node.getNodeType()) {
                case INSTANT_CONDITION -> {
                    final ConditionConfiguration conditionConfiguration = node.configurationAs(ConditionConfiguration.class);

                    loadCriteriaSetsIntoContainer(conditionConfiguration, workflow, nodeId, dataContainer);
                    loadNodeEventIntoContainer(workflow, conditionConfiguration.getNodeEvent(), nodeId, clientKeyToId, dataContainer);
                }
                case IF_ELSE_CONDITION -> {
                    final IfElseConfiguration ifElseConfiguration = node.configurationAs(IfElseConfiguration.class);

                    loadCriteriaSetsIntoContainer(ifElseConfiguration.getCondition(), workflow, nodeId, dataContainer);
                    loadNodeEventIntoContainer(workflow, ifElseConfiguration.getNodeEvent(), nodeId, clientKeyToId, dataContainer);
                }
                case WAIT_FOR_CONDITION -> {
                    final WaitForConditionConfiguration waitForConditionConfiguration = node.configurationAs(WaitForConditionConfiguration.class);

                    loadCriteriaSetsIntoContainer(waitForConditionConfiguration.getCondition(), workflow, nodeId, dataContainer);
                    loadNodeEventIntoContainer(workflow, waitForConditionConfiguration.getNodeEvent(), nodeId, clientKeyToId, dataContainer);
                    loadSpanConfigIntoContainer(waitForConditionConfiguration.getSpan(), workflow, nodeId, dataContainer);
                }
                case ACTION -> {
                    final ActionConfiguration actionConfiguration = node.configurationAs(ActionConfiguration.class);

                    loadActionIntoContainer(actionConfiguration, workflow, nodeId, dataContainer);
                }
                case DELAY -> {
                    final DelayConfiguration delayConfiguration = node.configurationAs(DelayConfiguration.class);

                    loadSpanConfigIntoContainer(delayConfiguration.getSpan(), workflow, nodeId, dataContainer);
                }
            }
        }
    }

    /**
     * Loads the version into the container.
     *
     * @param isCreateEvent     the flag to indicate if the workflow is being created
     * @param workflow          the workflow V2
     * @param existingContainer the existing data container
     * @param dataContainer     the data container
     * @param currentTime       the current time
     * @return the version id
     */
    private Long loadVersionIntoContainer(final boolean isCreateEvent, final WorkflowV2 workflow,
                                          final DataContainer existingContainer, final DataContainer dataContainer,
                                          final long currentTime) {
        final Table versionTable = getVersionTable();
        final Long workflowId = workflow.id();
        final Long orgId = workflow.orgId();
        final WorkflowVersion workflowVersion = workflow.getWorkflowVersion();
        final Row versionRow = new Row(versionTable);

        workflowVersion.setId(versionRow.getPKValue());
        versionRow.setOrgId(orgId);
        versionRow.set(OMWFLVERSION.WORKFLOW_ID, workflowId);
        versionRow.set(OMWFLVERSION.STATUS, workflowVersion.getVersionStatus().getId());
        versionRow.set(OMWFLVERSION.INITIAL_NODE_ID, workflowVersion.getInitialNodeId());
        versionRow.set(OMWFLVERSION.PUBLISHED_TIME, currentTime);

        if (!isCreateEvent) {
            final Row row = existingContainer.getRow(versionTable, new WhereClause(Criteria.eq(versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), workflowId)));
            final int version = row.get(OMWFLVERSION.VERSION_NUMBER);

            row.set(OMWFLVERSION.STATUS, VersionStatus.ARCHIVED.getId());
            versionRow.set(OMWFLVERSION.VERSION_NUMBER, version + 1);
            dataContainer.updateRow(row);
        } else {
            versionRow.set(OMWFLVERSION.VERSION_NUMBER, 1);
        }
        dataContainer.updateRow(versionRow);

        return versionRow.getPKValue();
    }

    /**
     * Loads the trigger users into the container.
     *
     * @param workflow             the workflow V2
     * @param triggerConfiguration the trigger configuration
     * @param dataContainer        the data container
     */
    private void loadTriggerUsersIntoContainer(final WorkflowV2 workflow, final TriggerConfiguration triggerConfiguration,
                                               final DataContainer dataContainer) {
        if (TriggerScope.SELECTED_USERS == triggerConfiguration.getTriggerScope()) {
            final Table triggerUserTable = getTriggerUsersTable();
            final Long workflowId = workflow.id();
            final Collection<Long> userIds = triggerConfiguration.getUserIds();

            for (final Long userId : userIds) {
                final Row triggerUserRow = new Row(triggerUserTable);

                triggerUserRow.setOrgId(workflow.orgId());
                triggerUserRow.set(OMWFLTRIGGERUSERS.WORKFLOW_ID, workflowId);
                triggerUserRow.set(OMWFLTRIGGERUSERS.VERSION_ID, workflow.getWorkflowVersion().id());
                triggerUserRow.set(OMWFLTRIGGERUSERS.USER_ID, userId);

                dataContainer.updateRow(triggerUserRow);
            }
        }
    }

    /**
     * Loads the nodes into the container.
     *
     * @param workflow      the workflow V2
     * @param versionId     the version id
     * @param dataContainer the data container
     * @return the map of client key to id
     */
    private Map<String, Long> loadNodesIntoContainer(final WorkflowV2 workflow, final Long versionId, final DataContainer dataContainer) {
        final Table nodeTable = getNodeTable();
        final Map<String, Long> clientKeyToId = new LinkedHashMap<>();

        for (final WorkflowNode node : workflow.getNodesAsCollection()) {
            final Row nodeRow = new Row(nodeTable);

            nodeRow.setOrgId(workflow.orgId());
            node.setId(nodeRow.getPKValue());
            nodeRow.set(OMWFLNODE.VERSION_ID, versionId);
            nodeRow.set(OMWFLNODE.CLIENT_KEY, node.getClientKey());
            nodeRow.set(OMWFLNODE.NODE_TYPE, node.getNodeType().getId());

            dataContainer.updateRow(nodeRow);
            clientKeyToId.put(node.getClientKey(), nodeRow.getPKValue());
        }
        return clientKeyToId;
    }

    /**
     * Loads the edges into the container.
     *
     * @param workflow      the workflow V2
     * @param clientKeyToId the map of client key to id
     * @param dataContainer the data container
     */
    private void loadEdgesIntoContainer(final WorkflowV2 workflow, final Map<String, Long> clientKeyToId,
                                        final DataContainer dataContainer) {
        final Table edgeTable = getEdgeTable();

        for (final WorkflowNode node : workflow.getNodesAsCollection()) {
            final Long fromNodeId = clientKeyToId.get(node.getClientKey());

            final WorkflowNodeEdge nextEdge = node.getNextEdge();
            final WorkflowNodeEdge fallbackEdge = node.getFallbackEdge();

            if (Objects.nonNull(nextEdge)) {
                upsertEdge(edgeTable, fromNodeId, clientKeyToId.get(String.valueOf(nextEdge.getToNodeId())), EdgeType.NEXT, dataContainer);
            }

            if (Objects.nonNull(fallbackEdge)) {
                upsertEdge(edgeTable, fromNodeId, clientKeyToId.get(String.valueOf(fallbackEdge.getToNodeId())), EdgeType.FALLBACK, dataContainer);
            }
        }
    }

    /**
     * Upserts the edge into the container.
     *
     * @param edgeTable     the edge table
     * @param fromNodeId    the from node id
     * @param toNodeId      the to node id
     * @param edgeType      the edge type
     * @param dataContainer the data container
     */
    private void upsertEdge(final Table edgeTable, final Long fromNodeId, final Long toNodeId,
                            final EdgeType edgeType, final DataContainer dataContainer) {
        if (Objects.isNull(fromNodeId) || Objects.isNull(toNodeId)) {
            return;
        }
        final Row edgeRow = new Row(edgeTable);

        edgeRow.set(MWFLNODEEDGE.FROM_NODE_ID, fromNodeId);
        edgeRow.set(MWFLNODEEDGE.TO_NODE_ID, toNodeId);
        edgeRow.set(MWFLNODEEDGE.EDGE_TYPE, edgeType.getId());

        dataContainer.updateRow(edgeRow);
    }

    /**
     * Loads the span config into the container.
     *
     * @param spanConfiguration the span configuration
     * @param workflow          the workflow V2
     * @param nodeId            the node id
     * @param dataContainer     the data container
     */
    private void loadSpanConfigIntoContainer(final SpanConfiguration spanConfiguration, final WorkflowV2 workflow,
                                             final Long nodeId, final DataContainer dataContainer) {
        final Table spanTable = getSpanConfigTable();
        final Row spanRow = new Row(spanTable);

        spanRow.setOrgId(workflow.orgId());
        spanRow.set(OMWFLNODESPANCONFIG.AMOUNT, spanConfiguration.getAmount());
        spanRow.set(OMWFLNODESPANCONFIG.UNIT, spanConfiguration.getUnit());
        spanRow.set(OMWFLNODESPANCONFIG.TYPE, spanConfiguration.getType().getId());
        spanRow.set(OMWFLNODESPANCONFIG.NODE_ID, nodeId);
        spanRow.set(OMWFLNODESPANCONFIG.SKIP_WEEKENDS, spanConfiguration.getSkipWeekends());
        spanRow.set(OMWFLNODESPANCONFIG.DUE_TIME, spanConfiguration.getDueTime());

        dataContainer.updateRow(spanRow);
    }

    /**
     * Loads the node event into the container.
     *
     * @param workflow      the workflow V2
     * @param nodeEvent     the node event
     * @param nodeId        the node id
     * @param clientKeyToId the map of client key to id
     * @param dataContainer the data container
     */
    private void loadNodeEventIntoContainer(final WorkflowV2 workflow, final NodeEvent nodeEvent,
                                            final Long nodeId, final Map<String, Long> clientKeyToId,
                                            final DataContainer dataContainer) {
        final Table eventTable = getNodeEventTable();
        final Row eventRow = new Row(eventTable);

        eventRow.setOrgId(workflow.orgId());
        nodeEvent.setId(eventRow.getPKValue());
        eventRow.set(OMWFLNODEEVENT.NODE_ID, nodeId);
        eventRow.set(OMWFLNODEEVENT.CONTEXT_TYPE, nodeEvent.getContextType().getId());
        eventRow.set(OMWFLNODEEVENT.SOURCE_NODE_ID, clientKeyToId.get(nodeEvent.getSourceNodeClientKey()));

        dataContainer.updateRow(eventRow);
    }

    /**
     * Loads the action into the container.
     *
     * @param action        the action configuration
     * @param workflow      the workflow V2
     * @param nodeId        the node id
     * @param dataContainer the data container
     */
    private void loadActionIntoContainer(final ActionConfiguration action, final WorkflowV2 workflow,
                                         final Long nodeId, final DataContainer dataContainer) {
        final Table actionTable = getActionTable();
        final Row actionRow = new Row(actionTable);

        actionRow.setOrgId(workflow.orgId());
        action.setId(actionRow.getPKValue());
        actionRow.set(OMWFLACTIONSV2.NODE_ID, nodeId);
        actionRow.set(OMWFLACTIONSV2.ACTION_SOURCE, action.getTriggerActionSource().id());
        actionRow.set(OMWFLACTIONSV2.ACTION_EVENT, action.getTriggerActionEvent().id());
        actionRow.set(OMWFLACTIONSV2.FIELD_MAPPING, action.getFieldMappingAsString());

        dataContainer.updateRow(actionRow);
    }

    /**
     * Loads the criteria sets into the container.
     *
     * @param conditionConfiguration the condition configuration
     * @param workflow               the workflow V2
     * @param nodeId                 the node id
     * @param dataContainer          the data container
     */
    private void loadCriteriaSetsIntoContainer(final ConditionConfiguration conditionConfiguration, final WorkflowV2 workflow,
                                               final Long nodeId, final DataContainer dataContainer) {
        final Collection<WorkflowCriteriaSet> criteriaSets = conditionConfiguration.getCriteriaSets();
        final Counter order = new Counter(1);

        for (final WorkflowCriteriaSet criteriaSet : criteriaSets) {
            final CriteriaConnector connector = criteriaSet.getConnector();
            final Row criteriaSetRow = new Row(getCriteriaSetTable());

            criteriaSet.setId(criteriaSetRow.getPKValue());
            criteriaSetRow.setOrgId(workflow.orgId());

            if (Objects.nonNull(connector)) {
                criteriaSetRow.set(OMWFLCRITERIASETV2.CONNECTOR_ID, connector.getId());
            }

            criteriaSetRow.set(OMWFLCRITERIASETV2.NODE_ID, nodeId);
            criteriaSetRow.set(OMWFLCRITERIASETV2.ORDER, order.getAndIncrement());
            dataContainer.updateRow(criteriaSetRow);

            loadCriteriaIntoContainer(workflow, nodeId, criteriaSet, dataContainer);
        }

    }

    /**
     * Loads the criteria into the container.
     *
     * @param workflow      the workflow V2
     * @param nodeId        the node id
     * @param criteriaSet   the criteria set
     * @param dataContainer the data container
     */
    private void loadCriteriaIntoContainer(final WorkflowV2 workflow, final Long nodeId,
                                           final WorkflowCriteriaSet criteriaSet, final DataContainer dataContainer) {
        final Collection<WorkflowCriteria> criteriaList = criteriaSet.getCriteriaList();
        final Table criteriaTable = getCriteriaTable();
        final Counter order = new Counter(1);

        for (final WorkflowCriteria criteria : criteriaList) {
            final CriteriaConnector connector = criteria.getConnector();

            final Row criteriaRow = new Row(criteriaTable);

            criteria.setId(criteriaRow.getPKValue());
            criteriaRow.setOrgId(workflow.orgId());

            if (Objects.nonNull(connector)) {
                criteriaRow.set(OMWFLCRITERIAV2.CONNECTOR_ID, connector.getId());
            }

            criteriaRow.set(OMWFLCRITERIAV2.NODE_ID, nodeId);
            criteriaRow.set(OMWFLCRITERIAV2.SET_ID, criteriaSet.getId());
            criteriaRow.set(OMWFLCRITERIAV2.COMPARATOR_ID, criteria.getComparator().getId());
            criteriaRow.set(OMWFLCRITERIAV2.ORDER, order.getAndIncrement());
            dataContainer.updateRow(criteriaRow);

            loadFieldDetailsIntoContainer(criteria, dataContainer);
            loadValueIntoContainer(criteria, dataContainer);
        }
    }

    /**
     * Loads the field details into the container.
     *
     * @param criteria      the criteria
     * @param dataContainer the data container
     */
    private void loadFieldDetailsIntoContainer(final WorkflowCriteria criteria, final DataContainer dataContainer) {
        final WorkflowCriteriaField field = criteria.getCriteriaField();
        final Long criteriaId = criteria.getId();
        final Consumer<Table> fieldCriteria = fieldTable -> {
            final String criteriaIdColumn = FieldGroup.WORKFLOW_RELATED_FIELD == field.getFieldGroup()
                    ? MWFLFIELDCRITERIAV2.CRITERIA_ID : MWFLSOURCEFIELDCRITERIAV2.CRITERIA_ID;
            final String fieldIdColumn = FieldGroup.WORKFLOW_RELATED_FIELD == field.getFieldGroup()
                    ? MWFLFIELDCRITERIAV2.FIELD_ID : MWFLSOURCEFIELDCRITERIAV2.FIELD_ID;
            final Row fieldRow = new Row(fieldTable);

            fieldRow.set(fieldIdColumn, field.getFieldId());
            fieldRow.set(criteriaIdColumn, criteriaId);
            dataContainer.updateRow(fieldRow);
        };

        fieldCriteria.accept(FieldGroup.WORKFLOW_RELATED_FIELD == field.getFieldGroup() ? getFieldCriteriaTable() : getSourceFieldCriteriaTable());
    }

    /**
     * Loads the value into the container.
     *
     * @param criteria      the criteria
     * @param dataContainer the data container
     */
    private void loadValueIntoContainer(final WorkflowCriteria criteria, final DataContainer dataContainer) {
        final Collection<WorkflowCriteriaValue> values = criteria.getCriteriaValues();

        if (Objects.isNull(values)) {
            return;
        }
        final Table valueTable = getCriteriaValueTable();
        final Long criteriaId = criteria.getId();

        for (final WorkflowCriteriaValue value : values) {
            final Row valueRow = new Row(valueTable);

            valueRow.set(MWFLCRITERIAVALUEV2.CRITERIA_ID, criteriaId);
            valueRow.set(MWFLCRITERIAVALUEV2.VALUE, value.getValue());
            valueRow.set(MWFLCRITERIAVALUEV2.VALUE_ID, value.getValueId());
            value.setId(valueRow.getPKValue());
            dataContainer.updateRow(valueRow);
        }
    }

    /**
     * Gets the existing data container.
     *
     * @param orgId       the organization id
     * @param workflowIds the workflow ids
     * @return the existing data container
     */
    private DataContainer getExistingDataContainer(final long orgId, final Collection<Long> workflowIds) {
        final Table table = getTable();
        final Table versionTable = getVersionTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table, versionTable);
        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), workflowIds))
                .and(Criteria.eq(versionTable.getColumn(OMWFLVERSION.STATUS), VersionStatus.PUBLISHED.getId())));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Gets the data container.
     *
     * @param orgId       the organization id
     * @param workflowIds the workflow ids
     * @return the data container
     */
    private DataContainer getDataContainer(final long orgId, final Collection<Long> workflowIds) {
        final Table table = getTable();
        final Table versionTable = getVersionTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table, versionTable, getTriggerUsersTable(), getNodeTable(), getEdgeTable(),
                getSpanConfigTable(), getNodeEventTable(), getActionTable(), getCriteriaSetTable(), getCriteriaTable(),
                getCriteriaValueTable(), getSourceFieldCriteriaTable(), getFieldCriteriaTable());
        selectQuery.addJoinClause(getJoinClauses());
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), workflowIds))
                .and(Criteria.eq(versionTable.getColumn(OMWFLVERSION.STATUS), VersionStatus.PUBLISHED.getId())));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Gets the join clauses.
     *
     * @return the join clauses
     */
    private List<JoinClause> getJoinClauses() {
        final Table workflowTable = getTable();
        final Table versionTable = getVersionTable();
        final Table nodeTable = getNodeTable();
        final Table criteriaSetTable = getCriteriaSetTable();
        final Table criteriaTable = getCriteriaTable();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column workflowPk = workflowTable.getPKColumn();
        final Column versionPk = versionTable.getPKColumn();
        final Column nodePk = nodeTable.getPKColumn();
        final Column criteriaSetPk = criteriaSetTable.getPKColumn();
        final Column criteriaPk = criteriaTable.getPKColumn();

        return List.of(
                new JoinClause(workflowPk, versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID)),
                new JoinClause(versionPk, nodeTable.getColumn(OMWFLNODE.VERSION_ID)),
                new JoinClause(nodePk, getEdgeTable().getColumn(MWFLNODEEDGE.FROM_NODE_ID), leftJoin),
                new JoinClause(nodePk, getActionTable().getColumn(OMWFLACTIONSV2.NODE_ID), leftJoin),
                new JoinClause(nodePk, getSpanConfigTable().getColumn(OMWFLNODESPANCONFIG.NODE_ID), leftJoin),
                new JoinClause(nodePk, getNodeEventTable().getColumn(OMWFLNODEEVENT.NODE_ID), leftJoin),
                new JoinClause(nodePk, criteriaSetTable.getColumn(OMWFLCRITERIASETV2.NODE_ID), leftJoin),
                new JoinClause(criteriaSetPk, criteriaTable.getColumn(OMWFLCRITERIAV2.SET_ID), leftJoin),
                new JoinClause(versionPk, getTriggerUsersTable().getColumn(OMWFLTRIGGERUSERS.VERSION_ID), leftJoin),
                new JoinClause(criteriaPk, getCriteriaValueTable().getColumn(MWFLCRITERIAVALUEV2.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPk, getSourceFieldCriteriaTable().getColumn(MWFLSOURCEFIELDCRITERIAV2.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPk, getFieldCriteriaTable().getColumn(MWFLFIELDCRITERIAV2.CRITERIA_ID), leftJoin)
        );
    }

    /**
     * Converts the row to model.
     *
     * @param workflowRow   the workflow row
     * @param dataContainer the data container
     * @return the workflow V2
     */
    private WorkflowV2 convertRowToModel(final Row workflowRow, final DataContainer dataContainer) {
        final WorkflowV2 workflow = new WorkflowV2();
        final Long workflowId = workflowRow.getPKValue();
        final Table versionTable = getVersionTable();
        final Long orgId = workflowRow.getOrgId();
        final Row versionRow = dataContainer.getRow(versionTable, new WhereClause(Criteria.eq(versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), workflowId)));
        final WorkflowVersion workflowVersion = new WorkflowVersion();
        final Long versionId = versionRow.getPKValue();
        final short initialNodeId = versionRow.get(OMWFLVERSION.INITIAL_NODE_ID);

        workflowVersion.setVersionStatus(VersionStatus.get(versionRow.get(OMWFLVERSION.STATUS)));
        workflowVersion.setVersionNumber(versionRow.get(OMWFLVERSION.VERSION_NUMBER));
        workflowVersion.setInitialNodeId(initialNodeId);
        workflowVersion.setId(versionId);
        workflowVersion.setOrgId(versionRow.getOrgId());
        workflowVersion.setPublishedTime(versionRow.get(OMWFLVERSION.PUBLISHED_TIME));
        workflowVersion.setWorkflowId(workflowId);

        workflow.setId(workflowId);
        workflow.setOrgId(orgId);
        workflow.setOwnerId(workflowRow.get(OMWFLWORKFLOWV2.OWNER_ID));
        workflow.setTemplateId(workflowRow.get(OMWFLWORKFLOWV2.TEMPLATE_ID));
        workflow.setName(workflowRow.get(OMWFLWORKFLOWV2.NAME));
        workflow.setDescription(workflowRow.get(OMWFLWORKFLOWV2.DESCRIPTION));
        workflow.setWorkflowVersion(workflowVersion);
        workflow.setActive(workflowRow.get(OMWFLWORKFLOWV2.IS_ACTIVE));
        setNodes(orgId, workflow, versionId, workflowRow, dataContainer);

        return workflow;
    }

    /**
     * Converts the list row to model.
     *
     * @param workflowRow   the workflow row
     * @param dataContainer the data container
     * @return the workflow list DTO
     */
    private WorkflowListDTO convertListRowToModel(final Row workflowRow, final DataContainer dataContainer) {
        final WorkflowListDTO workflow = new WorkflowListDTO();
        final Long workflowId = workflowRow.getPKValue();
        final Table versionTable = getVersionTable();
        final Table nodeTable = getNodeTable();
        final Row versionRow = dataContainer.getRow(versionTable,
                new WhereClause(Criteria.eq(versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), workflowId)));
        final long totalActions = dataContainer.getRows(nodeTable,
                        new WhereClause(Criteria.eq(nodeTable.getColumn(OMWFLNODE.VERSION_ID), versionRow.getPKValue())))
                .count();

        workflow.setId(workflowId);
        workflow.setOrgId(workflowRow.getOrgId());
        workflow.setName(workflowRow.get(OMWFLWORKFLOWV2.NAME));
        workflow.setActive(workflowRow.get(OMWFLWORKFLOWV2.IS_ACTIVE));
        workflow.setModifiedTime(workflowRow.get(OMWFLWORKFLOWV2.MODIFIED_TIME));
        workflow.setTriggerSource(TriggerSource.get(workflowRow.get(OMWFLWORKFLOWV2.TRIGGER_SOURCE)));
        workflow.setTriggerEvent(TriggerEvent.get(workflowRow.get(OMWFLWORKFLOWV2.TRIGGER_EVENT)));
        workflow.setTotalActions((int) totalActions);

        return workflow;
    }

    /**
     * Sets the nodes.
     *
     * @param orgId         the organization id
     * @param workflow      the workflow V2
     * @param versionId     the version id
     * @param workflowRow   the workflow row
     * @param dataContainer the data container
     */
    private void setNodes(final Long orgId, final WorkflowV2 workflow, final Long versionId,
                          final Row workflowRow, final DataContainer dataContainer) {
        final Table nodeTable = getNodeTable();
        final Table edgeTable = getEdgeTable();
        final Map<String, WorkflowNode> nodes = WorkflowNode.newNodeMap();
        final Map<Long, String> idToClientKey = new HashMap<>();

        dataContainer.getRows(nodeTable, new WhereClause(Criteria.eq(nodeTable.getColumn(OMWFLNODE.VERSION_ID), versionId)))
                .forEach(nodeRow -> {
                    final WorkflowNode node = convertNodeRow(orgId, workflow, versionId, nodeRow, workflowRow, dataContainer);
                    final String clientKey = node.getClientKey();

                    loadEdgesForNode(node, dataContainer, edgeTable);
                    nodes.put(clientKey, node);
                    idToClientKey.put(node.id(), clientKey);
                });

        workflow.setIdToClientKey(idToClientKey);
        workflow.setNodes(nodes);
    }

    /**
     * Converts the node row to model.
     *
     * @param orgId         the organization id
     * @param workflow      the workflow V2
     * @param versionId     the version id
     * @param nodeRow       the node row
     * @param workflowRow   the workflow row
     * @param dataContainer the data container
     * @return the workflow node
     */
    private WorkflowNode convertNodeRow(final Long orgId, final WorkflowV2 workflow, final Long versionId,
                                        final Row nodeRow, final Row workflowRow, final DataContainer dataContainer) {
        final WorkflowNode node = new WorkflowNode();
        final Long nodeId = nodeRow.getPKValue();
        final NodeType nodeType = NodeType.get(nodeRow.get(OMWFLNODE.NODE_TYPE));

        node.setId(nodeId);
        node.setClientKey(nodeRow.get(OMWFLNODE.CLIENT_KEY));
        node.setNodeType(nodeType);
        node.setVersionId(versionId);
        node.setOrgId(orgId);

        switch (nodeType) {
            case TRIGGER -> {
                if (Objects.nonNull(workflowRow)) {
                    final TriggerConfiguration triggerConfiguration = new TriggerConfiguration();
                    final Table triggerUsersTable = getTriggerUsersTable();

                    dataContainer.getRows(triggerUsersTable, new WhereClause(Criteria.eq(triggerUsersTable.getColumn(OMWFLTRIGGERUSERS.VERSION_ID), versionId)))
                            .forEach(triggerUserRow -> triggerConfiguration.addUserId(triggerUserRow.get(OMWFLTRIGGERUSERS.USER_ID)));

                    triggerConfiguration.setTriggerScope(TriggerScope.get(workflowRow.get(OMWFLWORKFLOWV2.TRIGGER_SCOPE)));
                    triggerConfiguration.setTriggerSource(TriggerSource.get(workflowRow.get(OMWFLWORKFLOWV2.TRIGGER_SOURCE)));
                    triggerConfiguration.setTriggerEvent(TriggerEvent.get(workflowRow.get(OMWFLWORKFLOWV2.TRIGGER_EVENT)));

                    node.setConfiguration(triggerConfiguration);
                    workflow.setTriggerSourceId(workflowRow.get(OMWFLWORKFLOWV2.TRIGGER_SOURCE));
                }
            }
            case INSTANT_CONDITION -> {
                final ConditionConfiguration conditionConfiguration = new ConditionConfiguration();

                conditionConfiguration.setCriteriaSets(loadCriteriaSets(nodeId, dataContainer));
                conditionConfiguration.setNodeEvent(loadNodeEvent(nodeId, dataContainer));

                node.setConfiguration(conditionConfiguration);
            }
            case IF_ELSE_CONDITION -> {
                final IfElseConfiguration ifElseConfiguration = new IfElseConfiguration();
                final ConditionConfiguration conditionConfiguration = new ConditionConfiguration();

                conditionConfiguration.setCriteriaSets(loadCriteriaSets(nodeId, dataContainer));

                ifElseConfiguration.setNodeEvent(loadNodeEvent(nodeId, dataContainer));
                ifElseConfiguration.setCondition(conditionConfiguration);

                node.setConfiguration(ifElseConfiguration);
            }
            case WAIT_FOR_CONDITION -> {
                final WaitForConditionConfiguration waitForConditionConfiguration = new WaitForConditionConfiguration();
                final ConditionConfiguration conditionConfiguration = new ConditionConfiguration();

                conditionConfiguration.setCriteriaSets(loadCriteriaSets(nodeId, dataContainer));

                waitForConditionConfiguration.setNodeEvent(loadNodeEvent(nodeId, dataContainer));
                waitForConditionConfiguration.setSpan(loadSpanConfiguration(nodeId, dataContainer));
                waitForConditionConfiguration.setCondition(conditionConfiguration);

                node.setConfiguration(waitForConditionConfiguration);
            }
            case ACTION -> node.setConfiguration(loadActionConfiguration(nodeId, dataContainer));
            case DELAY -> {
                final DelayConfiguration delayConfiguration = new DelayConfiguration();

                delayConfiguration.setSpan(loadSpanConfiguration(nodeId, dataContainer));
                node.setConfiguration(delayConfiguration);
            }
        }

        return node;
    }

    /**
     * Loads the edges for the node.
     *
     * @param node          the workflow node
     * @param dataContainer the data container
     * @param edgeTable     the edge table
     */
    private void loadEdgesForNode(final WorkflowNode node, final DataContainer dataContainer, final Table edgeTable) {
        dataContainer.getRows(edgeTable, new WhereClause(Criteria.eq(edgeTable.getColumn(MWFLNODEEDGE.FROM_NODE_ID), node.id())))
                .forEach(edgeRow -> {
                    final EdgeType edgeType = EdgeType.get(edgeRow.get(MWFLNODEEDGE.EDGE_TYPE));
                    final WorkflowNodeEdge workflowNodeEdge = new WorkflowNodeEdge();

                    workflowNodeEdge.setId(edgeRow.getPKValue());
                    workflowNodeEdge.setFromNodeId(edgeRow.get(MWFLNODEEDGE.FROM_NODE_ID));
                    workflowNodeEdge.setEdgeType(edgeType);
                    workflowNodeEdge.setToNodeId(edgeRow.get(MWFLNODEEDGE.TO_NODE_ID));

                    switch (edgeType) {
                        case NEXT -> node.setNextEdge(workflowNodeEdge);
                        case FALLBACK -> node.setFallbackEdge(workflowNodeEdge);
                    }
                });
    }

    /**
     * Loads the action configuration.
     *
     * @param nodeId        the node id
     * @param dataContainer the data container
     * @return the action configuration
     */
    private ActionConfiguration loadActionConfiguration(final Long nodeId, final DataContainer dataContainer) {
        final Table actionTable = getActionTable();
        final Row actionRow = dataContainer.getRow(actionTable, new WhereClause(Criteria.eq(actionTable.getColumn(OMWFLACTIONSV2.NODE_ID), nodeId)));

        if (Objects.isNull(actionRow)) {
            return null;
        }
        final ActionConfiguration actionConfiguration = new ActionConfiguration();

        actionConfiguration.setId(actionRow.getPKValue());
        actionConfiguration.setOrgId(actionRow.getOrgId());
        actionConfiguration.setTriggerActionSource(triggerActionSourceService.get(actionRow.get(OMWFLACTIONSV2.ACTION_SOURCE)));
        actionConfiguration.setTriggerActionEvent(triggerActionEventService.get(actionRow.get(OMWFLACTIONSV2.ACTION_EVENT)));
        actionConfiguration.setFieldMapping(actionRow.get(OMWFLACTIONSV2.FIELD_MAPPING));

        return actionConfiguration;
    }

    /**
     * Loads the span configuration.
     *
     * @param nodeId        the node id
     * @param dataContainer the data container
     * @return the span configuration
     */
    private SpanConfiguration loadSpanConfiguration(final Long nodeId, final DataContainer dataContainer) {
        final Table spanTable = getSpanConfigTable();
        return Optional.ofNullable(dataContainer.getRow(spanTable, new WhereClause(Criteria.eq(spanTable.getColumn(OMWFLNODESPANCONFIG.NODE_ID), nodeId))))
                .map(spanRow -> {
                    final SpanConfiguration spanConfiguration = new SpanConfiguration();
                    final SpanType spanType = SpanType.get(spanRow.get(OMWFLNODESPANCONFIG.TYPE));

                    spanConfiguration.setId(spanRow.getPKValue());
                    spanConfiguration.setOrgId(spanRow.getOrgId());
                    spanConfiguration.setAmount(spanRow.get(OMWFLNODESPANCONFIG.AMOUNT));
                    spanConfiguration.setType(spanType);
                    spanConfiguration.setSkipWeekends(spanRow.get(OMWFLNODESPANCONFIG.SKIP_WEEKENDS));
                    spanConfiguration.setDueTime(spanRow.get(OMWFLNODESPANCONFIG.DUE_TIME));

                    switch (spanType) {
                        case PREDEFINED ->
                                spanConfiguration.setPredefinedSpanUnit(PredefinedSpanUnit.get(spanRow.get(OMWFLNODESPANCONFIG.UNIT)));
                        case CUSTOM ->
                                spanConfiguration.setCustomSpanUnit(CustomSpanUnit.get(spanRow.get(OMWFLNODESPANCONFIG.UNIT)));
                    }

                    return spanConfiguration;
                }).orElse(null);
    }

    /**
     * Loads the node event.
     *
     * @param nodeId    the node id
     * @param container the data container
     * @return the node event
     */
    private NodeEvent loadNodeEvent(final Long nodeId, final DataContainer container) {
        final Table eventTable = getNodeEventTable();
        final Row eventRow = container.getRow(eventTable, new WhereClause(Criteria.eq(eventTable.getColumn(OMWFLNODEEVENT.NODE_ID), nodeId)));

        if (Objects.isNull(eventRow)) {
            return null;
        }

        final NodeEvent nodeEvent = new NodeEvent();

        nodeEvent.setId(eventRow.getPKValue());
        nodeEvent.setOrgId(eventRow.getOrgId());
        nodeEvent.setContextType(EventContextType.get(eventRow.get(OMWFLNODEEVENT.CONTEXT_TYPE)));
        nodeEvent.setSourceNodeId(eventRow.get(OMWFLNODEEVENT.SOURCE_NODE_ID));
        nodeEvent.setNodeId(nodeId);

        return nodeEvent;
    }

    /**
     * Loads the criteria sets.
     *
     * @param nodeId        the node id
     * @param dataContainer the data container
     * @return the criteria sets
     */
    private Collection<WorkflowCriteriaSet> loadCriteriaSets(final Long nodeId, final DataContainer dataContainer) {
        final Table criteriaSetTable = getCriteriaSetTable();

        return dataContainer.getRows(criteriaSetTable, new WhereClause(Criteria.eq(criteriaSetTable.getColumn(OMWFLCRITERIASETV2.NODE_ID), nodeId)))
                .map(row -> convertCriteriaSetRow(row, dataContainer))
                .sorted(Comparator.comparing(WorkflowCriteriaSet::getOrder))
                .toList();
    }

    /**
     * Converts the criteria set row to model.
     *
     * @param criteriaSetRow the criteria set row
     * @param dataContainer  the data container
     * @return the criteria set
     */
    private WorkflowCriteriaSet convertCriteriaSetRow(final Row criteriaSetRow, final DataContainer dataContainer) {
        final WorkflowCriteriaSet criteriaSet = new WorkflowCriteriaSet();
        final Long criteriaSetId = criteriaSetRow.getPKValue();
        final Short connectorId = criteriaSetRow.get(OMWFLCRITERIASETV2.CONNECTOR_ID);

        if (Objects.nonNull(connectorId)) {
            criteriaSet.setConnector(CriteriaConnector.get(connectorId));
        }

        criteriaSet.setId(criteriaSetId);
        criteriaSet.setOrder(criteriaSetRow.get(OMWFLCRITERIASETV2.ORDER));
        criteriaSet.setCriteriaList(loadCriteria(criteriaSetId, dataContainer));

        return criteriaSet;
    }

    /**
     * Loads the criteria.
     *
     * @param criteriaSetId the criteria set id
     * @param dataContainer the data container
     * @return the criteria
     */
    private Collection<WorkflowCriteria> loadCriteria(final Long criteriaSetId, final DataContainer dataContainer) {
        final Table criteriaTable = getCriteriaTable();

        return dataContainer.getRows(criteriaTable, new WhereClause(Criteria.eq(criteriaTable.getColumn(OMWFLCRITERIAV2.SET_ID), criteriaSetId)))
                .map(row -> convertCriteriaRow(row, dataContainer))
                .sorted(Comparator.comparing(WorkflowCriteria::getOrder))
                .toList();
    }

    /**
     * Converts the criteria row to model.
     *
     * @param criteriaRow   the criteria row
     * @param dataContainer the data container
     * @return the criteria
     */
    private WorkflowCriteria convertCriteriaRow(final Row criteriaRow, final DataContainer dataContainer) {
        final WorkflowCriteria criteria = new WorkflowCriteria();
        final Long criteriaId = criteriaRow.getPKValue();
        final Short connectorId = criteriaRow.get(OMWFLCRITERIAV2.CONNECTOR_ID);

        criteria.setId(criteriaId);
        criteria.setOrder(criteriaRow.get(OMWFLCRITERIAV2.ORDER));

        if (Objects.nonNull(connectorId)) {
            criteria.setConnector(CriteriaConnector.get(connectorId));
        }

        criteria.setComparator(comparatorService.get(criteriaRow.get(OMWFLCRITERIAV2.COMPARATOR_ID)));
        criteria.setCriteriaField(loadCriteriaField(criteriaId, dataContainer));
        criteria.setCriteriaValues(loadCriteriaValues(criteriaId, dataContainer));

        return criteria;
    }

    /**
     * Loads the criteria field.
     *
     * @param criteriaId    the criteria id
     * @param dataContainer the data container
     * @return the criteria field
     */
    private WorkflowCriteriaField loadCriteriaField(final Long criteriaId, final DataContainer dataContainer) {
        final Table sourceFieldTable = getSourceFieldCriteriaTable();
        final Row sourceFieldRow = dataContainer.getRow(sourceFieldTable, new WhereClause(Criteria.eq(sourceFieldTable.getColumn(MWFLSOURCEFIELDCRITERIAV2.CRITERIA_ID), criteriaId)));
        final WorkflowCriteriaField field = new WorkflowCriteriaField();

        if (Objects.nonNull(sourceFieldRow)) {
            field.setFieldId(sourceFieldRow.get(MWFLSOURCEFIELDCRITERIAV2.FIELD_ID));
            field.setFieldGroup(FieldGroup.SOURCE_RELATED_FIELD);

            return field;
        }

        final Table fieldTable = getFieldCriteriaTable();
        final Row fieldRow = dataContainer.getRow(fieldTable, new WhereClause(Criteria.eq(fieldTable.getColumn(MWFLFIELDCRITERIAV2.CRITERIA_ID), criteriaId)));

        field.setFieldId(fieldRow.get(MWFLFIELDCRITERIAV2.FIELD_ID));
        field.setFieldGroup(FieldGroup.WORKFLOW_RELATED_FIELD);

        return field;
    }

    /**
     * Loads the criteria values.
     *
     * @param criteriaId    the criteria id
     * @param dataContainer the data container
     * @return the criteria values
     */
    private Collection<WorkflowCriteriaValue> loadCriteriaValues(final Long criteriaId, final DataContainer dataContainer) {
        final Table valueTable = getCriteriaValueTable();

        return dataContainer.getRows(valueTable, new WhereClause(Criteria.eq(valueTable.getColumn(MWFLCRITERIAVALUEV2.CRITERIA_ID), criteriaId)))
                .map(valueRow -> {
                    final WorkflowCriteriaValue value = new WorkflowCriteriaValue();

                    value.setId(valueRow.getPKValue());
                    value.setValue(valueRow.get(MWFLCRITERIAVALUEV2.VALUE));
                    value.setValueId(valueRow.get(MWFLCRITERIAVALUEV2.VALUE_ID));

                    return value;
                }).toList();
    }

    /**
     * Applies the sort criteria.
     *
     * @param sortField   the sort field
     * @param sortType    the sort type
     * @param selectQuery the select query
     */
    private void applySortCriteria(final String sortField, final String sortType, final SelectQuery selectQuery) {
        final Table table = getTable();
        final OrderByClause.OrderType orderType = WorkflowAttributesV2.DESC.equalsIgnoreCase(sortType)
                ? OrderByClause.OrderType.DESCENDING : OrderByClause.OrderType.ASCENDING;
        final Consumer<Column> consumer = (column) -> {
            selectQuery.addOrderByClause(new OrderByClause(column, orderType));
            selectQuery.addSelectClause(column);
        };

        switch (sortField) {
            case WorkflowAttributesV2.NAME -> consumer.accept(table.getColumn(OMWFLWORKFLOWV2.NAME));
            case WorkflowAttributesV2.TRIGGER_SOURCE ->
                    consumer.accept(table.getColumn(OMWFLWORKFLOWV2.TRIGGER_SOURCE));
            case WorkflowAttributesV2.TRIGGER_EVENT -> consumer.accept(table.getColumn(OMWFLWORKFLOWV2.TRIGGER_EVENT));
            case WorkflowAttributesV2.MODIFIED_TIME -> consumer.accept(table.getColumn(OMWFLWORKFLOWV2.MODIFIED_TIME));
        }
    }

    /**
     * Active published workflows matching org, trigger source, and event.
     *
     * @param orgId         organization id
     * @param triggerSource trigger source
     * @param triggerEvent  trigger event
     * @return matching workflows
     */
    public Collection<WorkflowV2> getWorkflowsByTrigger(final long orgId, final TriggerSource triggerSource, final TriggerEvent triggerEvent) {
        final Table table = getTable();
        final Table versionTable = getVersionTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table, versionTable, getTriggerUsersTable(), getNodeTable(), getEdgeTable(),
                getSpanConfigTable(), getNodeEventTable(), getActionTable(), getCriteriaSetTable(), getCriteriaTable(),
                getCriteriaValueTable(), getSourceFieldCriteriaTable(), getFieldCriteriaTable());
        selectQuery.addJoinClause(getJoinClauses());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.TRIGGER_SOURCE), triggerSource.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.TRIGGER_EVENT), triggerEvent.getId()))
                .and(Criteria.eq(versionTable.getColumn(OMWFLVERSION.STATUS), VersionStatus.PUBLISHED.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.IS_ACTIVE), true))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.IS_DELETED), false)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return List.of();
        }
        return dataContainer.getRows(table).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * Whether any active published V2 workflow exists for the trigger.
     *
     * @param orgId         organization id
     * @param triggerSource trigger source
     * @param triggerEvent  trigger event
     * @return true if at least one workflow exists
     */
    public boolean exists(final long orgId, final TriggerSource triggerSource, final TriggerEvent triggerEvent) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.addJoinClause(new JoinClause(table.getPKColumn(), getVersionTable().getColumn(OMWFLVERSION.WORKFLOW_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.TRIGGER_SOURCE), triggerSource.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.TRIGGER_EVENT), triggerEvent.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.IS_ACTIVE), true))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOWV2.IS_DELETED), false))
                .and(getPublishedVersionCriteria()));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Loads a workflow graph for a specific version (published or archived).
     *
     * @param orgId     organization id
     * @param versionId version id
     * @return workflow or null
     */
    public WorkflowV2 getByVersionId(final long orgId, final long versionId) {
        final Table table = getTable();
        final Table versionTable = getVersionTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table, versionTable, getTriggerUsersTable(), getNodeTable(), getEdgeTable(),
                getSpanConfigTable(), getNodeEventTable(), getActionTable(), getCriteriaSetTable(), getCriteriaTable(),
                getCriteriaValueTable(), getSourceFieldCriteriaTable(), getFieldCriteriaTable());
        selectQuery.addJoinClause(getJoinClauses());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(versionTable.getPKColumn(), versionId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return convertRowToModel(dataContainer.getRow(table), dataContainer);
    }

    /**
     * All version ids for a workflow (published and archived).
     *
     * @param orgId      organization id
     * @param workflowId workflow id
     * @return version ids
     */
    public Collection<Long> getAllVersionIds(final long orgId, final long workflowId) {
        final Table versionTable = getVersionTable();
        final SelectQuery selectQuery = SelectQuery.get(versionTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(versionTable.getColumn(OMWFLVERSION.WORKFLOW_ID), workflowId)));
        return orgDataStore(orgId).get(selectQuery).getRows(versionTable).map(Row::getPKValue).toList();
    }

    /**
     * <p>
     * Gets the workflow name for the given id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the workflow id
     * @return {@link String} of workflow name
     */
    public String getWorkflowName(final long orgId, final Long id) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, id, List.of(table.getColumn(OMWFLWORKFLOWV2.NAME)));

        return Objects.isNull(row) ? null : row.get(OMWFLWORKFLOW.NAME);
    }

    /**
     * <p>
     * Retrieves the workflow count.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represents the criteria used to filter the workflows
     * @return The total number of workflow entities that satisfy the specified criteria
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(OMWFLWORKFLOWV2.NAME))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link WorkflowV2} mapped for the given organization id
     * </p>
     *
     * @param orgId            Represents the organization ID for which the workflows are being fetched.
     * @param dropdownLimit    Represents the pagination limits, including the start index and the maximum
     *                         number of records to be retrieved.
     * @param dropdownCriteria Represents the criteria used for filtering the workflows, determining which
     *                         workflows should be included in the result.
     * @return {@link Collection} of {@link WorkflowV2}
     */
    public Collection<WorkflowV2> getWorkflows(final long orgId, final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column nameColumn = table.getColumn(OMWFLWORKFLOWV2.NAME);

        selectQuery.addSelectClause(nameColumn);
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        selectQuery.setWhereClause(new WhereClause(dropdownCriteria.build(nameColumn)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptyList() : dataContainer.getRows(table).map(row -> {
            final WorkflowV2 workflow = new WorkflowV2();

            workflow.setId(row.getPKValue());
            workflow.setName(row.get(OMWFLWORKFLOWV2.NAME));

            return workflow;

        }).toList();
    }

}
