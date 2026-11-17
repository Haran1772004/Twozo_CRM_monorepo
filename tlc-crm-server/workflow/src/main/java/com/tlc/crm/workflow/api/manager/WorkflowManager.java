package com.tlc.crm.workflow.api.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

import com.tlc.crm.workflow.sql.resource.MWFLSOURCEFIELDCRITERIA;
import com.tlc.crm.workflow.sql.resource.OMWFLACTIONS;
import com.tlc.crm.workflow.sql.resource.OMWFLCRITERIASET;
import com.tlc.crm.workflow.sql.resource.MWFLCRITERIAVALUE;
import com.tlc.crm.workflow.sql.resource.MWFLFIELDCRITERIA;
import com.tlc.crm.workflow.sql.resource.OMWFLCRITERIA;
import com.tlc.crm.workflow.sql.resource.OMWFLWORKFLOW;
import com.tlc.crm.workflow.api.model.filter.WorkflowFilter;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link Workflow} related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowManager extends AbstractConfigManager<Workflow> {

    private final WorkflowComparatorService comparatorService;
    private final TriggerActionSourceService triggerActionSourceService;
    private final TriggerActionEventService triggerActionEventService;
    private final WorkflowFilterManager workflowFilterManager;

    private WorkflowManager() {
        this.comparatorService = WorkflowComparatorService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
    }

    private static class Instance {
        private static final WorkflowManager INSTANCE = new WorkflowManager();
    }

    /**
     * Gets the {@link WorkflowManager} Instance.
     *
     * @return {@link WorkflowManager}
     */
    public static WorkflowManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link Table} of workflow.
     *
     * @return The workflow table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMWFLWORKFLOW.TABLE);
    }

    /**
     * Gets the {@link Table} of criteria.
     *
     * @return The workflow criteria table.
     */
    private Table getCriteriaTable() {
        return Table.get(OMWFLCRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of criteria set.
     *
     * @return The workflow criteria set table.
     */
    private Table getCriteriaSetTable() {
        return Table.get(OMWFLCRITERIASET.TABLE);
    }

    /**
     * Gets the {@link Table} of criteria value.
     *
     * @return The workflow criteria value table.
     */
    private Table getCriteriaValueTable() {
        return Table.get(MWFLCRITERIAVALUE.TABLE);
    }

    /**
     * Gets the {@link Table} of source field criteria.
     *
     * @return The source field criteria table.
     */
    private Table getSourceFieldCriteriaTable() {
        return Table.get(MWFLSOURCEFIELDCRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of workflow field criteria.
     *
     * @return The workflow field criteria table.
     */
    private Table getWorkflowFieldCriteriaTable() {
        return Table.get(MWFLFIELDCRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of workflow action.
     *
     * @return The workflow action table.
     */
    public Table getActionTable() {
        return Table.get(OMWFLACTIONS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Refers the organization id.
     * @param ids   Refers the workflow ids.
     * @return The collection of {@link Workflow}.
     */
    @Override
    public Collection<Workflow> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Set<Long> workflowIds = new HashSet<>(ids);
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), workflowIds)));

        if (orgDataStore(orgId).get(countQuery) == workflowIds.size()) {
            final Table criteriaTable = getCriteriaTable();
            final Table criteriaSetTable = getCriteriaSetTable();
            final Table sourceFieldCriteriaTable = getSourceFieldCriteriaTable();
            final Table workflowFieldCriteriaTable = getWorkflowFieldCriteriaTable();
            final Table actionTable = getActionTable();
            final Table criteriaValueTable = getCriteriaValueTable();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.addJoinClause(getWorkflowJoinClauses());
            selectQuery.addSelectClause(table, criteriaTable, criteriaSetTable, criteriaValueTable, sourceFieldCriteriaTable, workflowFieldCriteriaTable, actionTable);
            selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids))
                    .and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.IS_DELETED), false)));

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (dataContainer.isEmpty()) {
                throw ErrorCode.get(WorkflowErrorCodes.WORKFLOW_MARKED_DELETED, "i18n.workflow.flagged.deleted");
            } else {
                return dataContainer.getRows(table).map(row -> convertRowToModel(row, dataContainer)).toList();
            }
        } else {
            throw ErrorCode.get(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, "i18n.workflow.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Refers the organization id.
     * @param workflows Refers the collection of {@code Workflow}
     */
    @Override
    public void delete(final Long orgId, final Collection<Workflow> workflows) {
        final Collection<Long> ids = workflows.stream().map(Workflow::id).filter(Objects::nonNull).toList();
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOW.IS_DELETED), true);
        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOW.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of ids.
     * @return The collection of {@link Workflow}.
     */
    @Override
    public Collection<Workflow> partialGet(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param workflow Refers the {@link Workflow}.
     * @return The workflow.
     */
    @Override
    public Workflow create(final Workflow workflow) {
        final DataContainer dataContainer = DataContainer.create();

        loadWorkflowDetailsIntoContainer(true, workflow, dataContainer);
        orgDataStore(workflow.orgId()).commitChanges(dataContainer);

        return workflow;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id.
     * @param workflows Holds the details of the {@link Workflow}.
     * @return The response of update action.
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<Workflow> workflows) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> ids = workflows.stream().map(Workflow::id).filter(Objects::nonNull).toList();
        final DataContainer entityContainer = getDataContainer(orgId, ids);
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();

        workflows.forEach(workflow -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadWorkflowDetailsIntoContainer(false, workflow, entityContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(workflow.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Workflow update failed Org ID : {} ID : {} reason : {}", orgId, workflow.id(), errorCode.getMessage(), errorCode);
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
     * Gets the count of total workflow of given organization.
     *
     * @param orgId Refers the organization id.
     * @return The total workflow count.
     */
    public Long getWorkflowCount(final long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * Updates the status of the {@link Workflow}.
     *
     * @param orgId    Refers the organization id.
     * @param id       Refers the workflow id.
     * @param isActive Refers the workflow status to be changed.
     */
    public void updateStatus(final long orgId, final long id, final boolean isActive) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOW.IS_ACTIVE), isActive);
        updateQuery.addUpdateColumn(table.getColumn(OMWFLWORKFLOW.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * Converts the row into the {@link Workflow} for the list view.
     *
     * @param row           Holds the workflow information.
     * @param dataContainer A container that holds the updated information of {@link Workflow}.
     * @return {@link Workflow}.
     */
    private Workflow convertListRowToModel(final Row row, final DataContainer dataContainer) {
        final Workflow workflow = new Workflow();
        final Long workflowId = row.getPKValue();
        final Table actionTable = getActionTable();
        final Number actionCount = dataContainer.getRows(actionTable,
                new WhereClause(Criteria.eq(actionTable.getColumn(OMWFLACTIONS.WORKFLOW_ID), workflowId))).count();

        workflow.setId(workflowId);
        workflow.setName(row.get(OMWFLWORKFLOW.NAME));
        workflow.setTriggerSource(TriggerSource.get(row.get(OMWFLWORKFLOW.TRIGGER_SOURCE)));
        workflow.setTriggerEvent(TriggerEvent.get(row.get(OMWFLWORKFLOW.TRIGGER_EVENT)));
        workflow.setModifiedTime(row.get(OMWFLWORKFLOW.MODIFIED_TIME));
        workflow.setActive(row.get(OMWFLWORKFLOW.IS_ACTIVE));
        workflow.setTotalActions(actionCount.intValue());

        return workflow;
    }

    /**
     * Fetches the {@link DataContainer} of workflow for the given ids.
     *
     * @param orgId       Refers the organization id.
     * @param workflowIds Refers the workflow ids.
     * @return The container filled with the workflow details.
     */
    private DataContainer getDataContainer(final long orgId, final Collection<Long> workflowIds) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table, getCriteriaTable(), getCriteriaSetTable(), getCriteriaValueTable(),
                getSourceFieldCriteriaTable(), getWorkflowFieldCriteriaTable(), getActionTable());
        selectQuery.addJoinClause(getWorkflowJoinClauses());
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), workflowIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Gets the collection of {@link Workflow} for the given trigger source and trigger event.
     *
     * @param orgId         Refers the organization id.
     * @param triggerSource Refers the {@link TriggerSource}.
     * @param triggerEvent  Refers the {@link TriggerEvent}.
     * @return The collection of {@link Workflow}.
     */
    public Collection<Workflow> getWorkflowByTrigger(final long orgId, final TriggerSource triggerSource,
                                                     final TriggerEvent triggerEvent) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table, getCriteriaTable(), getCriteriaSetTable(), getCriteriaValueTable(),
                getSourceFieldCriteriaTable(), getWorkflowFieldCriteriaTable(), getActionTable());
        selectQuery.addJoinClause(getWorkflowJoinClauses());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMWFLWORKFLOW.TRIGGER_SOURCE), triggerSource.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.TRIGGER_EVENT), triggerEvent.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.IS_ACTIVE), true))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.IS_DELETED), false)));
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMWFLWORKFLOW.CREATED_TIME), OrderByClause.OrderType.DESCENDING));

        final DataContainer workflowContainer = orgDataStore(orgId).get(selectQuery);

        return workflowContainer.getRows(table).map(row -> convertRowToModel(row, workflowContainer)).toList();
    }

    /**
     * <p>
     * Checks whether the workflow exists or not
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param triggerSource Represents the {@link TriggerSource}
     * @param triggerEvent  Represents the {@link TriggerEvent}
     * @return true if the workflow exists, false otherwise
     */
    public boolean exists(final long orgId, final TriggerSource triggerSource, final TriggerEvent triggerEvent) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMWFLWORKFLOW.TRIGGER_SOURCE), triggerSource.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.TRIGGER_EVENT), triggerEvent.getId()))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.IS_ACTIVE), true))
                .and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.IS_DELETED), false)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Gets the required collection of {@link JoinClause} of workflow.
     *
     * @return The {@link List} of workflow {@link JoinClause}.
     */
    private List<JoinClause> getWorkflowJoinClauses() {
        final Table criteriaTable = getCriteriaTable();
        final Column workflowPkColumn = getTable().getPKColumn();
        final Column criteriaPkColumn = criteriaTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        return List.of(new JoinClause(workflowPkColumn, getActionTable().getColumn(OMWFLACTIONS.WORKFLOW_ID)),
                new JoinClause(workflowPkColumn, getCriteriaSetTable().getColumn(OMWFLCRITERIASET.WORKFLOW_ID), leftJoin),
                new JoinClause(workflowPkColumn, criteriaTable.getColumn(OMWFLCRITERIA.WORKFLOW_ID), leftJoin),
                new JoinClause(criteriaPkColumn, getCriteriaValueTable().getColumn(MWFLCRITERIAVALUE.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPkColumn, getSourceFieldCriteriaTable().getColumn(MWFLSOURCEFIELDCRITERIA.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPkColumn, getWorkflowFieldCriteriaTable().getColumn(MWFLFIELDCRITERIA.CRITERIA_ID), leftJoin));
    }

    /**
     * Loads the workflow details into the data container.
     *
     * @param isCreateEvent A flag which indicates whether the action is related to create or update event.
     * @param workflow      Refers the {@link Workflow} details.
     * @param dataContainer Holds the updated information of workflow values.
     */
    private void loadWorkflowDetailsIntoContainer(final boolean isCreateEvent, final Workflow workflow,
                                                  final DataContainer dataContainer) {
        final DataContainer existingWorkflowContainer = isCreateEvent ? null : getDataContainer(workflow.orgId(), List.of(workflow.id()));

        loadWorkflowDetailsIntoContainer(isCreateEvent, workflow, existingWorkflowContainer, dataContainer);
    }

    /**
     * Loads the workflow details into the data container.
     *
     * @param isCreateEvent             A flag which indicates whether the action is related to create or update event.
     * @param workflow                  Refers the {@link Workflow} attributes.
     * @param existingWorkflowContainer Holds the information of existing workflow attributes.
     * @param dataContainer             Holds the updated information of workflow values.
     */
    private void loadWorkflowDetailsIntoContainer(final boolean isCreateEvent, final Workflow workflow,
                                                  final DataContainer existingWorkflowContainer, final DataContainer dataContainer) {
        final Table workflowTable = getTable();
        final Long id = workflow.id();
        final Long orgId = workflow.orgId();
        final Row workflowRow = isCreateEvent ? new Row(workflowTable) : existingWorkflowContainer.getRow(workflowTable, id);
        final long currentTime = Instant.now().toEpochMilli();

        if (!isCreateEvent && Objects.isNull(workflowRow)) {
            throw ErrorCode.get(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, "i18n.workflow.not.found");
        }

        if (workflowRow.isNewRow()) {
            workflowRow.setOrgId(orgId);
            workflow.setId(workflowRow.getPKValue());
            workflowRow.set(OMWFLWORKFLOW.CREATED_TIME, currentTime);
        }
        loadCriteriaSetsIntoContainer(isCreateEvent, workflow, existingWorkflowContainer, dataContainer);
        loadWorkflowActionsIntoContainer(isCreateEvent, workflow, existingWorkflowContainer, dataContainer);

        workflowRow.set(OMWFLWORKFLOW.OWNER_ID, workflow.getOwnerId());
        workflowRow.set(OMWFLWORKFLOW.NAME, workflow.getName());
        workflowRow.set(OMWFLWORKFLOW.DESCRIPTION, workflow.getDescription());
        workflowRow.set(OMWFLWORKFLOW.IS_ACTIVE, workflow.isActive());
        workflowRow.set(OMWFLWORKFLOW.TEMPLATE_ID, workflow.getTemplateId());
        workflowRow.set(OMWFLWORKFLOW.TRIGGER_SOURCE, workflow.getTriggerSource().getId());
        workflowRow.set(OMWFLWORKFLOW.TRIGGER_EVENT, workflow.getTriggerEvent().getId());
        workflowRow.set(OMWFLWORKFLOW.MODIFIED_TIME, currentTime);

        dataContainer.updateRow(workflowRow);

        if (!isCreateEvent) {
            deleteUnmappedCriteriaSet(workflow, existingWorkflowContainer, dataContainer);
        }
    }

    /**
     * Deletes the unmapped {@link WorkflowCriteriaSet} of workflow.
     *
     * @param workflow                  Holds the attributes of {@link Workflow}.
     * @param existingWorkflowContainer Holds the information of existing workflow attributes.
     * @param dataContainer             Holds the updated information of workflow values.
     */
    private void deleteUnmappedCriteriaSet(final Workflow workflow, final DataContainer existingWorkflowContainer,
                                           final DataContainer dataContainer) {
        final Table criteriaSetTable = getCriteriaSetTable();
        final Collection<WorkflowCriteriaSet> criteriaSets = workflow.getCriteriaSets();
        final WhereClause whereClause = new WhereClause(Criteria.eq(criteriaSetTable.getColumn(OMWFLCRITERIASET.WORKFLOW_ID), workflow.id()));
        final Stream<Row> criteriaSetRows = existingWorkflowContainer.getRows(criteriaSetTable,
                Objects.nonNull(criteriaSets) && !criteriaSets.isEmpty() ? whereClause
                        .and(Criteria.notIn(criteriaSetTable.getPKColumn(), criteriaSets.stream().map(WorkflowCriteriaSet::getId)
                                .collect(Collectors.toUnmodifiableSet()))) : whereClause);

        criteriaSetRows.forEach(dataContainer::deleteRow);
    }

    /**
     * Loads the {@link WorkflowCriteriaSet} into the data container.
     *
     * @param isCreateEvent             A flag which indicates whether the action is related to create or update event.
     * @param workflow                  Refers the {@link Workflow} attributes.
     * @param existingWorkflowContainer Holds the information of existing workflow attributes.
     * @param dataContainer             Holds the updated information of workflow values.
     */
    private void loadCriteriaSetsIntoContainer(final boolean isCreateEvent, final Workflow workflow,
                                               final DataContainer existingWorkflowContainer, final DataContainer dataContainer) {
        final Collection<WorkflowCriteriaSet> criteriaSets = workflow.getCriteriaSets();

        if (Objects.nonNull(criteriaSets)) {
            final Table criteriaSetTable = getCriteriaSetTable();
            final Counter order = new Counter(1);

            for (final WorkflowCriteriaSet workflowCriteriaSet : criteriaSets) {
                final CriteriaConnector connector = workflowCriteriaSet.getConnector();
                final Long criteriaSetId = workflowCriteriaSet.getId();
                final Row criteriaSetRow = Objects.isNull(criteriaSetId) ? new Row(criteriaSetTable)
                        : existingWorkflowContainer.getRow(criteriaSetTable, criteriaSetId);

                if (criteriaSetRow.isNewRow()) {
                    workflowCriteriaSet.setId(criteriaSetRow.getPKValue());
                    criteriaSetRow.setOrgId(workflow.orgId());
                }

                if (Objects.nonNull(connector)) {
                    criteriaSetRow.set(OMWFLCRITERIASET.CONNECTOR_ID, connector.getId());
                }
                criteriaSetRow.set(OMWFLCRITERIASET.WORKFLOW_ID, workflow.id());
                criteriaSetRow.set(OMWFLCRITERIASET.ORDER, order.getAndIncrement());
                dataContainer.updateRow(criteriaSetRow);

                loadCriteriaIntoContainer(isCreateEvent, workflow, workflowCriteriaSet, existingWorkflowContainer, dataContainer);
            }
        }
    }

    /**
     * Loads the {@link WorkflowCriteria} into the data container.
     *
     * @param isCreateEvent             A flag which indicates whether the action is related to create or update event.
     * @param workflow                  Refers the {@link Workflow} attributes.
     * @param workflowCriteriaSet       Refers the {@link WorkflowCriteriaSet} details.
     * @param existingWorkflowContainer Holds the information of existing workflow attributes.
     * @param dataContainer             Holds the updated information of workflow values.
     */
    private void loadCriteriaIntoContainer(final boolean isCreateEvent, final Workflow workflow, final WorkflowCriteriaSet workflowCriteriaSet,
                                           final DataContainer existingWorkflowContainer, final DataContainer dataContainer) {
        final Collection<WorkflowCriteria> criteriaList = workflowCriteriaSet.getCriteriaList();
        final Table criteriaTable = getCriteriaTable();
        final Long workflowId = workflow.id();
        final Counter order = new Counter(1);

        for (final WorkflowCriteria workflowCriteria : criteriaList) {
            final CriteriaConnector connector = workflowCriteria.getConnector();
            final Long criteriaId = workflowCriteria.getId();
            final Row criteriaRow = Objects.isNull(criteriaId) ? new Row(criteriaTable)
                    : existingWorkflowContainer.getRow(criteriaTable, criteriaId);

            if (criteriaRow.isNewRow()) {
                workflowCriteria.setId(criteriaRow.getPKValue());
                criteriaRow.setOrgId(workflow.orgId());
            }

            if (Objects.nonNull(connector)) {
                criteriaRow.set(OMWFLCRITERIA.CONNECTOR_ID, connector.getId());
            }

            criteriaRow.set(OMWFLCRITERIA.WORKFLOW_ID, workflowId);
            criteriaRow.set(OMWFLCRITERIA.SET_ID, workflowCriteriaSet.getId());
            criteriaRow.set(OMWFLCRITERIA.COMPARATOR_ID, workflowCriteria.getComparator().getId());
            criteriaRow.set(OMWFLCRITERIA.ORDER, order.getAndIncrement());

            dataContainer.updateRow(criteriaRow);

            loadFieldDetailsIntoContainer(isCreateEvent, workflowCriteria, existingWorkflowContainer, dataContainer);
            loadValueIntoContainer(isCreateEvent, workflowCriteria, existingWorkflowContainer, dataContainer);
        }

        if (!isCreateEvent) {
            final Stream<Row> criteriaRows = existingWorkflowContainer.getRows(criteriaTable,
                    new WhereClause(Criteria.eq(criteriaTable.getColumn(OMWFLCRITERIA.WORKFLOW_ID), workflowId))
                            .and(Criteria.eq(criteriaTable.getColumn(OMWFLCRITERIA.SET_ID), workflowCriteriaSet.getId()))
                            .and(Criteria.notIn(criteriaTable.getPKColumn(), criteriaList.stream()
                                    .map(WorkflowCriteria::getId).collect(Collectors.toUnmodifiableSet()))));

            criteriaRows.filter(Objects::nonNull).forEach(dataContainer::deleteRow);
        }
    }

    /**
     * Loads the {@link WorkflowCriteriaValue} into the data container.
     *
     * @param isCreateEvent             A flag which indicates whether the action is related to create or update event.
     * @param workflowCriteria          Refers the {@link WorkflowCriteria} details.
     * @param existingWorkflowContainer Holds the information of existing workflow attributes.
     * @param dataContainer             Holds the updated information of workflow values.
     */
    private void loadValueIntoContainer(final boolean isCreateEvent, final WorkflowCriteria workflowCriteria,
                                        final DataContainer existingWorkflowContainer, final DataContainer dataContainer) {
        final Collection<WorkflowCriteriaValue> criteriaValues = workflowCriteria.getCriteriaValues();

        if (Objects.nonNull(criteriaValues)) {
            final Table criteriaValueTable = getCriteriaValueTable();
            final Long criteriaId = workflowCriteria.getId();

            for (final WorkflowCriteriaValue workflowCriteriaValue : criteriaValues) {
                final Row criteriaValueRow;

                if (isCreateEvent) {
                    criteriaValueRow = new Row(criteriaValueTable);
                } else {
                    final Row oldCriteriaValueRow = existingWorkflowContainer.getRow(criteriaValueTable,
                            new WhereClause(Criteria.eq(criteriaValueTable.getColumn(MWFLCRITERIAVALUE.CRITERIA_ID), criteriaId))
                                    .and(Criteria.eq(criteriaValueTable.getPKColumn(), workflowCriteriaValue.getId())));

                    criteriaValueRow = Objects.nonNull(oldCriteriaValueRow) ? oldCriteriaValueRow : new Row(criteriaValueTable);
                }

                criteriaValueRow.set(MWFLCRITERIAVALUE.CRITERIA_ID, criteriaId);
                criteriaValueRow.set(MWFLCRITERIAVALUE.VALUE, workflowCriteriaValue.getValue());
                criteriaValueRow.set(MWFLCRITERIAVALUE.VALUE_ID, workflowCriteriaValue.getValueId());
                workflowCriteriaValue.setId(criteriaValueRow.getPKValue());

                dataContainer.updateRow(criteriaValueRow);
            }

            if (!isCreateEvent) {
                final Stream<Row> criteriaValueRows = existingWorkflowContainer.getRows(criteriaValueTable,
                        new WhereClause(Criteria.eq(criteriaValueTable.getColumn(MWFLCRITERIAVALUE.CRITERIA_ID), criteriaId))
                                .and(Criteria.notIn(criteriaValueTable.getPKColumn(), criteriaValues.stream()
                                        .map(WorkflowCriteriaValue::getId).collect(Collectors.toUnmodifiableSet()))));

                criteriaValueRows.filter(Objects::nonNull).forEach(dataContainer::deleteRow);
            }
        }
    }

    /**
     * Loads the {@link WorkflowCriteriaField} into the data container.
     *
     * @param isCreateEvent             A flag which indicates whether the action is related to create or update event.
     * @param workflowCriteria          Refers the {@link WorkflowCriteria} details.
     * @param existingWorkflowContainer Holds the information of existing workflow attributes.
     * @param dataContainer             Holds the updated information of workflow values.
     */
    private void loadFieldDetailsIntoContainer(final boolean isCreateEvent, final WorkflowCriteria workflowCriteria,
                                               final DataContainer existingWorkflowContainer, final DataContainer dataContainer) {
        final WorkflowCriteriaField workflowCriteriaField = workflowCriteria.getCriteriaField();
        final Long criteriaId = workflowCriteria.getId();
        final Consumer<Table> fieldCriteria = (fieldCriteriaTable) -> {
            final Row fieldCriteriaRow;

            if (isCreateEvent) {
                fieldCriteriaRow = new Row(fieldCriteriaTable);
            } else {
                final Row oldCriterisFieldRow = existingWorkflowContainer.getRow(fieldCriteriaTable,
                        new WhereClause(Criteria.eq(fieldCriteriaTable.getColumn(MWFLFIELDCRITERIA.CRITERIA_ID), criteriaId)));

                fieldCriteriaRow = Objects.nonNull(oldCriterisFieldRow) ? oldCriterisFieldRow : new Row(fieldCriteriaTable);
            }
            fieldCriteriaRow.set(MWFLFIELDCRITERIA.FIELD_ID, workflowCriteriaField.getFieldId());
            fieldCriteriaRow.set(MWFLFIELDCRITERIA.CRITERIA_ID, criteriaId);

            dataContainer.updateRow(fieldCriteriaRow);
        };

        fieldCriteria.accept(FieldGroup.WORKFLOW_RELATED_FIELD == workflowCriteriaField.getFieldGroup()
                ? getWorkflowFieldCriteriaTable() : getSourceFieldCriteriaTable());
    }

    /**
     * Loads the {@link WorkflowAction} into the data container.
     *
     * @param isCreateEvent             A flag which indicates whether the action is related to create or update event.
     * @param workflow                  Holds the {@link Workflow} attributes.
     * @param existingWorkflowContainer Holds the information of existing workflow attributes.
     * @param dataContainer             Holds the updated information of workflow values.
     */
    private void loadWorkflowActionsIntoContainer(final boolean isCreateEvent, final Workflow workflow,
                                                  final DataContainer existingWorkflowContainer,
                                                  final DataContainer dataContainer) {
        final Table actionTable = getActionTable();
        final Collection<WorkflowAction> workflowActions = workflow.getActions();
        final Long workflowId = workflow.id();
        final Counter order = new Counter(1);

        for (final WorkflowAction workflowAction : workflowActions) {
            final Long actionId = workflowAction.getId();
            final Row actionRow = Objects.isNull(actionId) ? new Row(actionTable) : existingWorkflowContainer.getRow(actionTable, actionId);

            if (actionRow.isNewRow()) {
                actionRow.setOrgId(workflow.orgId());
                workflowAction.setId(actionRow.getPKValue());
            }
            actionRow.set(OMWFLACTIONS.WORKFLOW_ID, workflowId);
            actionRow.set(OMWFLACTIONS.ACTION_SOURCE, workflowAction.getTriggerActionSource().id());
            actionRow.set(OMWFLACTIONS.ACTION_EVENT, workflowAction.getTriggerActionEvent().id());
            actionRow.set(OMWFLACTIONS.FIELD_MAPPING, workflowAction.getFieldMappingAsString());
            actionRow.set(OMWFLACTIONS.ORDER, order.getAndIncrement());

            dataContainer.updateRow(actionRow);
        }

        if (!isCreateEvent) {
            final Stream<Row> actionRows = existingWorkflowContainer.getRows(actionTable,
                    new WhereClause(Criteria.eq(actionTable.getColumn(OMWFLACTIONS.WORKFLOW_ID), workflowId))
                            .and(Criteria.notIn(actionTable.getPKColumn(), workflowActions.stream()
                                    .map(WorkflowAction::getId).collect(Collectors.toUnmodifiableSet()))));

            actionRows.forEach(dataContainer::deleteRow);
        }
    }

    /**
     * Converts the row into the {@link Workflow} attributes.
     *
     * @param row           Which has the workflow details.
     * @param dataContainer Holds the updated information of workflow.
     * @return The workflow details.
     */
    private Workflow convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Workflow workflow = new Workflow();
        final Long workflowId = row.getPKValue();

        workflow.setId(workflowId);
        workflow.setOrgId(row.getOrgId());
        workflow.setOwnerId(row.get(OMWFLWORKFLOW.OWNER_ID));
        workflow.setActive(row.get(OMWFLWORKFLOW.IS_ACTIVE));
        workflow.setTemplateId(row.get(OMWFLWORKFLOW.TEMPLATE_ID));
        workflow.setName(row.get(OMWFLWORKFLOW.NAME));
        workflow.setDescription(row.get(OMWFLWORKFLOW.DESCRIPTION));
        workflow.setTriggerSource(TriggerSource.get(row.get(OMWFLWORKFLOW.TRIGGER_SOURCE)));
        workflow.setTriggerEvent(TriggerEvent.get(row.get(OMWFLWORKFLOW.TRIGGER_EVENT)));
        workflow.setCriteriaSets(getCriteriaSets(workflowId, dataContainer));
        workflow.setActions(getActions(workflowId, dataContainer));

        return workflow;
    }

    /**
     * Gets the collection of {@link WorkflowCriteriaSet}.
     *
     * @param workflowId    Refers the workflow id.
     * @param dataContainer A container that holds the workflow information.
     * @return The collection of {@link WorkflowCriteriaSet}.
     */
    private Collection<WorkflowCriteriaSet> getCriteriaSets(final Long workflowId, final DataContainer dataContainer) {
        final Table criteriaSetTable = getCriteriaSetTable();
        final Stream<Row> criteriaSetRows = dataContainer.getRows(criteriaSetTable,
                new WhereClause(Criteria.eq(criteriaSetTable.getColumn(OMWFLCRITERIASET.WORKFLOW_ID), workflowId)));

        return criteriaSetRows.map(criteriaSetRow -> convertRowToCriteriaSet(criteriaSetRow, dataContainer)).sorted(Comparator.comparing(WorkflowCriteriaSet::getOrder)).toList();
    }

    /**
     * Converts the row into {@link WorkflowCriteriaSet}.
     *
     * @param criteriaSetRow Refers the row of the criteria set.
     * @param dataContainer  The container that holds the workflow related information.
     * @return The {@link WorkflowCriteriaSet} details.
     */
    private WorkflowCriteriaSet convertRowToCriteriaSet(final Row criteriaSetRow, final DataContainer dataContainer) {
        final Table criteriaTable = getCriteriaTable();
        final Table criteriaValueTable = getCriteriaValueTable();
        final Table sourceFieldCriteriaTable = getSourceFieldCriteriaTable();
        final Table fieldCriteriaTable = getWorkflowFieldCriteriaTable();
        final Long criteriaSetPkValue = criteriaSetRow.getPKValue();
        final Stream<Row> criteriaRows = dataContainer.getRows(criteriaTable,
                new WhereClause(Criteria.eq(criteriaTable.getColumn(OMWFLCRITERIA.SET_ID), criteriaSetPkValue)));
        final Short connectorId = criteriaSetRow.get(OMWFLCRITERIASET.CONNECTOR_ID);
        final WorkflowCriteriaSet workflowCriteriaSet = new WorkflowCriteriaSet();

        if (Objects.nonNull(connectorId)) {
            workflowCriteriaSet.setConnector(CriteriaConnector.get(connectorId));
        }

        workflowCriteriaSet.setOrder(criteriaSetRow.get(OMWFLCRITERIASET.ORDER));
        workflowCriteriaSet.setId(criteriaSetPkValue);
        workflowCriteriaSet.setCriteriaList(criteriaRows.map(criteriaRow -> {
            final WorkflowCriteria workflowCriteria = new WorkflowCriteria();
            final WorkflowCriteriaField workflowCriteriaField = new WorkflowCriteriaField();
            final Long criteriaId = criteriaRow.getPKValue();
            final Short criteriaConnectorId = criteriaRow.get(OMWFLCRITERIA.CONNECTOR_ID);
            final Stream<Row> criteriaValues = dataContainer.getRows(criteriaValueTable,
                    new WhereClause(Criteria.eq(criteriaValueTable.getColumn(MWFLCRITERIAVALUE.CRITERIA_ID), criteriaId)));
            final Row sourceFieldCriteriaRow = dataContainer.getRow(sourceFieldCriteriaTable,
                    new WhereClause(Criteria.eq(sourceFieldCriteriaTable.getColumn(MWFLSOURCEFIELDCRITERIA.CRITERIA_ID), criteriaId)));

            if (Objects.isNull(sourceFieldCriteriaRow)) {
                final Row workflowFieldCriteriaRow = dataContainer.getRow(fieldCriteriaTable,
                        new WhereClause(Criteria.eq(fieldCriteriaTable.getColumn(MWFLFIELDCRITERIA.CRITERIA_ID), criteriaId)));

                if (Objects.nonNull(workflowFieldCriteriaRow)) {
                    workflowCriteriaField.setId(workflowFieldCriteriaRow.getPKValue());
                    workflowCriteriaField.setFieldId(workflowFieldCriteriaRow.get(MWFLFIELDCRITERIA.FIELD_ID));
                    workflowCriteriaField.setFieldGroup(FieldGroup.WORKFLOW_RELATED_FIELD);

                    workflowCriteria.setCriteriaField(workflowCriteriaField);
                }
            } else {
                workflowCriteriaField.setId(sourceFieldCriteriaRow.getPKValue());
                workflowCriteriaField.setFieldId(sourceFieldCriteriaRow.get(MWFLSOURCEFIELDCRITERIA.FIELD_ID));
                workflowCriteriaField.setFieldGroup(FieldGroup.SOURCE_RELATED_FIELD);

                workflowCriteria.setCriteriaField(workflowCriteriaField);
            }

            if (Objects.nonNull(criteriaConnectorId)) {
                workflowCriteria.setConnector(CriteriaConnector.get(criteriaConnectorId));
            }
            workflowCriteria.setOrder(criteriaRow.get(OMWFLCRITERIA.ORDER));
            workflowCriteria.setId(criteriaId);
            workflowCriteria.setComparator(comparatorService.get(criteriaRow.get(OMWFLCRITERIA.COMPARATOR_ID)));
            workflowCriteria.setCriteriaValues(criteriaValues.map(valueRow -> {
                final WorkflowCriteriaValue workflowCriteriaValue = new WorkflowCriteriaValue();

                workflowCriteriaValue.setId(valueRow.getPKValue());
                workflowCriteriaValue.setValue(valueRow.get(MWFLCRITERIAVALUE.VALUE));
                workflowCriteriaValue.setValueId(valueRow.get(MWFLCRITERIAVALUE.VALUE_ID));

                return workflowCriteriaValue;
            }).toList());

            return workflowCriteria;
        }).sorted(Comparator.comparing(WorkflowCriteria::getOrder)).toList());

        return workflowCriteriaSet;
    }

    /**
     * Gets the collection of {@link WorkflowAction}.
     *
     * @param workflowId    Refers the workflow id.
     * @param dataContainer The container that holds the workflow related information.
     * @return The collection of {@link WorkflowAction}.
     */
    private Collection<WorkflowAction> getActions(final Long workflowId, final DataContainer dataContainer) {
        final Table actionTable = getActionTable();
        final Stream<Row> actionRows = dataContainer.getRows(actionTable,
                new WhereClause(Criteria.eq(actionTable.getColumn(OMWFLACTIONS.WORKFLOW_ID), workflowId)));

        return actionRows.map(actionRow -> {
            final WorkflowAction workflowAction = new WorkflowAction();

            workflowAction.setId(actionRow.getPKValue());
            workflowAction.setTriggerActionSource(triggerActionSourceService.get(actionRow.get(OMWFLACTIONS.ACTION_SOURCE)));
            workflowAction.setTriggerActionEvent(triggerActionEventService.get(actionRow.get(OMWFLACTIONS.ACTION_EVENT)));
            workflowAction.setFieldMapping(actionRow.get(OMWFLACTIONS.FIELD_MAPPING));
            workflowAction.setOrder(actionRow.get(OMWFLACTIONS.ORDER));

            return workflowAction;
        }).sorted(Comparator.comparing(WorkflowAction::getOrder)).toList();
    }

    /**
     * <p>
     * Retrieves the total count of {@link Workflow}
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param workflowFilter It holds the {@link WorkflowFilter} details
     * @return The total count of {@link Workflow}
     */
    public Long getTotalCount(final long orgId, final WorkflowFilter workflowFilter) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMWFLWORKFLOW.ORG_ID), orgId));
        final WhereClause executionHistoryFilterWhereClause = Objects.isNull(workflowFilter) ? null : workflowFilterManager.getExecutionHistoryFilterWhereClause(workflowFilter);
        final Column workflowPkColumn = table.getPKColumn();

        selectQuery.addSelectClause(workflowPkColumn.distinct());
        selectQuery.addJoinClause(DmlUtil.getJoins(table, getActionTable()));
        selectQuery.setWhereClause(Objects.isNull(executionHistoryFilterWhereClause) ? whereClause : whereClause.and(executionHistoryFilterWhereClause));
        countQuery.setWhereClause(new WhereClause(Criteria.in(workflowPkColumn, selectQuery)).and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.IS_DELETED), false)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the {@link Collection} of workflow ids matches for given {@link WorkflowFilter}
     * </p>
     *
     * @param orgId          Refers an organization id
     * @param start          Refers the start value of workflow to be fetched
     * @param limit          Refers the limit value of workflow to be fetched
     * @param sortField      Refers the field by which the data is sorted
     * @param sortType       Refers the sort type
     * @param workflowFilter An entity which holds the {@link WorkflowFilter}
     * @return The collection of workflow ids
     */
    private Collection<Long> getWorkflowIds(final long orgId, final int start, final int limit, final String sortField,
                                            final String sortType, final WorkflowFilter workflowFilter) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final WhereClause executionHistoryfilterWhereClause = Objects.isNull(workflowFilter) ? null : workflowFilterManager.getExecutionHistoryFilterWhereClause(workflowFilter);

        selectQuery.addJoinClause(DmlUtil.getJoins(table, getActionTable()));
        selectQuery.addSelectClause(table.getPKColumn().distinct());
        selectQuery.setLimitClause(new LimitClause(start, limit));
        applySortCriteria(sortField, sortType, selectQuery);

        if (Objects.nonNull(executionHistoryfilterWhereClause)) {
            selectQuery.setWhereClause(executionHistoryfilterWhereClause);
        }

        return orgDataStore(orgId).get(selectQuery).getRows(table).filter(Objects::nonNull).map(Row::getPKValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
        final Table table = getTable();
        final OrderByClause.OrderType orderType = WorkflowAttributes.DESC.equalsIgnoreCase(sortType) ? OrderByClause.OrderType.DESCENDING : OrderByClause.OrderType.ASCENDING;

        final Consumer<Column> consumer = (column) -> {
            selectQuery.addOrderByClause(new OrderByClause(column, orderType));
            selectQuery.addSelectClause(column);
        };

        switch (sortField) {
            case "name" -> consumer.accept(table.getColumn(OMWFLWORKFLOW.NAME));
            case "triggerSource" -> consumer.accept(table.getColumn(OMWFLWORKFLOW.TRIGGER_SOURCE));
            case "triggerEvent" -> consumer.accept(table.getColumn(OMWFLWORKFLOW.TRIGGER_EVENT));
            case "modifiedTime" -> consumer.accept(table.getColumn(OMWFLWORKFLOW.MODIFIED_TIME));
        }
    }

    /**
     * Retrieves the Collection of {@link Workflow} for given orgId.
     *
     * @param orgId          Refers the organization id.
     * @param start          Refers the start value of workflow to be fetched.
     * @param limit          Refers the limit value of workflow to be fetched.
     * @param sortField      Refers the field by which the data is sorted.
     * @param sortType       Refers the sort type.
     * @param workflowFilter An entity which holds the attributes of {@link WorkflowFilter}
     * @return The collection of {@link Workflow}.
     */
    public Collection<Workflow> getWorkflowList(final long orgId, final int start, final int limit, final String sortField, final String sortType, final WorkflowFilter workflowFilter) {
        final Collection<Long> workflowIds = getWorkflowIds(orgId, start, limit, sortField, sortType, workflowFilter);

        if (!workflowIds.isEmpty()) {
            final Table table = getTable();
            final Table actionTable = getActionTable();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.addSelectClause(List.of(table.getColumn(OMWFLWORKFLOW.NAME), table.getColumn(OMWFLWORKFLOW.TRIGGER_SOURCE),
                    table.getColumn(OMWFLWORKFLOW.TRIGGER_EVENT), table.getColumn(OMWFLWORKFLOW.MODIFIED_TIME), actionTable.getPKColumn(),
                    actionTable.getColumn(OMWFLACTIONS.WORKFLOW_ID), table.getColumn(OMWFLWORKFLOW.IS_ACTIVE)));
            selectQuery.addJoinClause(DmlUtil.getJoins(table, actionTable));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), workflowIds)).and(Criteria.eq(table.getColumn(OMWFLWORKFLOW.IS_DELETED), false)));
            applySortCriteria(sortField, sortType, selectQuery);
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
            final Collection<Workflow> workflows = dataContainer.getRows(table).map(row -> convertListRowToModel(row, dataContainer)).toList();

            if (Objects.equals(WorkflowAttributes.ACTIONS, sortField)) {
                final Comparator<Workflow> comparator = Comparator.comparingInt(Workflow::getTotalActions);
                final Comparator<Workflow> workflowComparator = WorkflowAttributes.DESC.equalsIgnoreCase(sortType)
                        ? comparator.reversed() : comparator;

                return workflows.stream().sorted(workflowComparator).toList();
            }

            return workflows;
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Retrieves an {@link Optional} that holds the workflow action if it exists; otherwise, returns an empty Optional.
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param workflowId Represents the workflow id
     * @param actionId   Represents the action id
     * @return An {@code Optional} containing the workflow action if found, or an empty {@code Optional} if not
     */
    public Optional<WorkflowAction> getWorkflowAction(final long orgId, final Long workflowId, final Long actionId) {
        final Collection<WorkflowAction> workflowActions = getWorkflowActions(orgId, workflowId, List.of(actionId));

        return workflowActions.isEmpty() ? Optional.empty() : workflowActions.stream().findFirst();
    }

    /**
     * Gets the collection of {@link WorkflowAction}.
     *
     * @param orgId      Represents the organization id
     * @param workflowId Represents the workflow id
     * @param actionIds  Represents the action ids to be fetched.
     * @return The {@link WorkflowAction} details
     */
    public List<WorkflowAction> getWorkflowActions(final long orgId, final Long workflowId, final Collection<Long> actionIds) {
        final Table actionTable = getActionTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(actionTable, new WhereClause(Criteria
                .eq(actionTable.getColumn(OMWFLACTIONS.WORKFLOW_ID), workflowId)).and(Criteria.in(actionTable.getPKColumn(), actionIds)));

        return dataContainer.getRows(actionTable).map(this::convertRowToWorkflowAction)
                .sorted(Comparator.comparing(WorkflowAction::getOrder)).toList();
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link WorkflowAction} details
     * </p>
     *
     * @param workflowActionRow It holds the {@link WorkflowAction} details
     * @return The {@link WorkflowAction} details
     */
    private WorkflowAction convertRowToWorkflowAction(final Row workflowActionRow) {
        final WorkflowAction workflowAction = new WorkflowAction();

        workflowAction.setId(workflowActionRow.getPKValue());
        workflowAction.setOrder(workflowActionRow.get(OMWFLACTIONS.ORDER));
        workflowAction.setFieldMapping(workflowActionRow.get(OMWFLACTIONS.FIELD_MAPPING));
        workflowAction.setTriggerActionEvent(triggerActionEventService.get(workflowActionRow.get(OMWFLACTIONS.ACTION_EVENT)));
        workflowAction.setTriggerActionSource(triggerActionSourceService.get(workflowActionRow.get(OMWFLACTIONS.ACTION_SOURCE)));

        return workflowAction;
    }

    /**
     * Checks whether the action id is exists or not
     *
     * @param orgId    represents the organisation id
     * @param actionId Represents the action id
     * @return true if action id is exists, otherwise false
     */
    public boolean actionExists(final Long orgId, final Long actionId) {
        final Table actionTable = getActionTable();
        final CountQuery countQuery = CountQuery.get(actionTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(actionTable.getPKColumn(), actionId)));

        return orgDataStore(orgId).get(countQuery) > 0;
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
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMWFLWORKFLOW.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRow(table).get(OMWFLWORKFLOW.NAME);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Workflow} mapped for the given organization id
     * </p>
     *
     * @param orgId            Represents the organization ID for which the workflows are being fetched.
     * @param dropdownLimit    Represents the pagination limits, including the start index and the maximum
     *                         number of records to be retrieved.
     * @param dropdownCriteria Represents the criteria used for filtering the workflows, determining which
     *                         workflows should be included in the result.
     * @return {@link Collection} of {@link Workflow}
     */
    public Collection<Workflow> getWorkflows(final long orgId, final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column nameColumn = table.getColumn(OMWFLWORKFLOW.NAME);

        selectQuery.addSelectClause(nameColumn);
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        selectQuery.setWhereClause(new WhereClause(dropdownCriteria.build(nameColumn)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptyList() : dataContainer.getRows(table).map(row -> {
            final Workflow workflow = new Workflow();

            workflow.setId(row.getPKValue());
            workflow.setName(row.get(OMWFLWORKFLOW.NAME));

            return workflow;

        }).toList();
    }

    /**
     * <p>
     * Retrieves the workflow count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represents the criteria used to filter the workflows
     * @return The total number of workflow entities that satisfy the specified criteria
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(OMWFLWORKFLOW.NAME))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Get the partial workflow data by action id
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param actionId  Represents the action id
     * @return The workflow with partial data
     */
    public Workflow getByActionId(final Long orgId, final Long actionId) {
        final Table table = getTable();
        final Table actionTable = getActionTable();
        final SelectQuery selectQuery = SelectQuery.get(actionTable);

        selectQuery.addSelectClause(table.getColumn(OMWFLWORKFLOW.NAME));
        selectQuery.addJoinClause(new JoinClause(actionTable.getColumn(OMWFLACTIONS.WORKFLOW_ID), table.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(actionTable.getPKColumn(), actionId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(table);

        if (Objects.isNull(row)) {
            return null;
        }
        final Workflow workflow = new Workflow();

        workflow.setId(row.getPKValue());
        workflow.setName(row.get(OMWFLWORKFLOW.NAME));

        return workflow;
    }
}
