package com.tlc.crm.workflow.api.manager.filter;

import com.tlc.crm.entity.filter.date.DateIntervals;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.filter.FilterGroup;
import com.tlc.crm.workflow.api.model.filter.WorkflowStatus;
import com.tlc.crm.workflow.api.model.filter.TriggerType;
import com.tlc.crm.workflow.api.model.filter.WorkflowFilter;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus;
import com.tlc.crm.workflow.api.model.history.filter.DateIntervalType;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilterType;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionStatusFilter;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilter;
import com.tlc.crm.workflow.sql.resource.OMWFLEXECUTIONHISTORY;
import com.tlc.crm.workflow.sql.resource.OMWFLACTIONS;
import com.tlc.crm.workflow.sql.resource.OMWFLWORKFLOW;
import com.tlc.crm.workflow.v2.api.model.ExecutionStatus;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.VersionStatus;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLACTIONSV2;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLNODE;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLVERSION;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLEXECUTIONV2;
import com.tlc.crm.workflow.v2.sql.resource.OMWFLWORKFLOWV2;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;

import com.tlc.l10n.temporal.interval.Interval;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * <p>
 * Builds the query for workflow filters
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowFilterQueryBuilder {

    private static final Long DEFAULT_FILTER = -1L;
    private static final String UNDERSCORE = "_";

    private WorkflowFilterQueryBuilder() {
    }

    private static class Instance {
        private static final WorkflowFilterQueryBuilder INSTANCE = new WorkflowFilterQueryBuilder();
    }

    /**
     * <p>
     * Gets the {@link WorkflowFilterQueryBuilder} Instance
     * </p>
     *
     * @return The {@link WorkflowFilterQueryBuilder}
     */
    public static WorkflowFilterQueryBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link Table} of workflow
     * </p>
     *
     * @return The workflow table
     */
    private Table getWorkflowTable() {
        return Table.get(OMWFLWORKFLOW.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of workflow execution history
     * </p>
     *
     * @return The workflow execution history table
     */
    private Table getWorkflowExecutionHistoryTable() {
        return Table.get(OMWFLEXECUTIONHISTORY.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of workflow action
     * </p>
     *
     * @return The workflow action table
     */
    private Table getActionTable() {
        return Table.get(OMWFLACTIONS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link WhereClause} based on the provided {@link WorkflowFilter}
     * </p>
     *
     * @param workflowFilter Represents the {@link WorkflowFilter}
     * @return A {@link WhereClause} representing the criteria specified in the {@link WorkflowFilter}
     */
    WhereClause getExecutionHistoryFilterWhereClause(final WorkflowFilter workflowFilter) {
        final Table workflowTable = getWorkflowTable();
        final Table actionTable = getActionTable();
        final Map<FilterGroup, Collection<Long>> criteria = workflowFilter.getCriteria();
        final String searchText = workflowFilter.getSearchText();
        WhereClause whereClause = null;

        for (final Map.Entry<FilterGroup, Collection<Long>> selectedCriteria : criteria.entrySet()) {
            final FilterGroup filterGroup = selectedCriteria.getKey();
            final List<Long> values = selectedCriteria.getValue().stream().toList();

            if (!values.contains(DEFAULT_FILTER)) {

                switch (filterGroup) {
                    case STATUS -> {
                        final Criteria statusCriteria = Criteria.eq(workflowTable.getColumn(OMWFLWORKFLOW.IS_ACTIVE),
                                WorkflowStatus.get(values.get(0).shortValue()).getId() == 1);

                        whereClause = Objects.isNull(whereClause) ? new WhereClause(statusCriteria) : whereClause.and(statusCriteria);
                    }
                    case TRIGGERS -> {
                        final Map<Short, Collection<Short>> triggers = new HashMap<>();
                        WhereClause triggerConditions = null;

                        values.stream().map(subField -> TriggerType.get(subField.shortValue()).name()).map(name -> name.split(UNDERSCORE))
                                .filter(index -> index.length >= 2).forEach(index -> triggers.computeIfAbsent(TriggerSource.valueOf(index[0]).getId(),
                                        event -> new ArrayList<>()).add(TriggerEvent.valueOf(index[1]).getId()));

                        for (final Map.Entry<Short, Collection<Short>> trigger : triggers.entrySet()) {
                            final WhereClause triggerWhereClause = new WhereClause(Criteria.eq(workflowTable.getColumn(OMWFLWORKFLOW.TRIGGER_SOURCE),
                                    trigger.getKey())).and(Criteria.in(workflowTable.getColumn(OMWFLWORKFLOW.TRIGGER_EVENT), trigger.getValue()));
                            triggerConditions = Objects.isNull(triggerConditions) ? triggerWhereClause : triggerConditions.or(triggerWhereClause);
                        }

                        whereClause = Objects.isNull(whereClause) ? triggerConditions : whereClause.and(triggerConditions);
                    }
                    case CREATED_BY -> {
                        final Criteria createdByCriteria = Criteria.in(workflowTable.getColumn(OMWFLWORKFLOW.OWNER_ID), values);

                        whereClause = Objects.isNull(whereClause) ? new WhereClause(createdByCriteria) : whereClause.and(createdByCriteria);
                    }
                    case ACTIONS -> {
                        final Criteria actionsCriteria = Criteria.in(actionTable.getColumn(OMWFLACTIONS.ACTION_EVENT), values);

                        whereClause = Objects.isNull(whereClause) ? new WhereClause(actionsCriteria) : whereClause.and(actionsCriteria);
                    }
                }
            }
        }

        if (Objects.nonNull(searchText)) {
            final Criteria searchCriteria = Criteria.contains(workflowTable.getColumn(OMWFLWORKFLOW.NAME), searchText);

            whereClause = Objects.isNull(whereClause) ? new WhereClause(searchCriteria) : whereClause.and(searchCriteria);
        }

        return whereClause;
    }

    /**
     * Gets the {@link WhereClause} based on the provided {@link WorkflowFilter} for workflow V2 tables.
     *
     * @param workflowFilter Represents the {@link WorkflowFilter}
     * @return A {@link WhereClause} representing the criteria specified in the {@link WorkflowFilter}
     */
    WhereClause buildWorkflowFilterWhereClause(final WorkflowFilter workflowFilter) {
        final Table workflowTable = Table.get(OMWFLWORKFLOWV2.TABLE);
        final Table versionTable = Table.get(OMWFLVERSION.TABLE);
        final Table nodeTable = Table.get(OMWFLNODE.TABLE);
        final Table actionTable = Table.get(OMWFLACTIONSV2.TABLE);
        final Map<FilterGroup, Collection<Long>> criteria = workflowFilter.getCriteria();
        final String searchText = workflowFilter.getSearchText();
        WhereClause whereClause = new WhereClause(Criteria.eq(versionTable.getColumn(OMWFLVERSION.STATUS), VersionStatus.PUBLISHED.getId()));

        for (final Map.Entry<FilterGroup, Collection<Long>> selectedCriteria : criteria.entrySet()) {
            final FilterGroup filterGroup = selectedCriteria.getKey();
            final List<Long> values = selectedCriteria.getValue().stream().toList();

            if (!values.contains(DEFAULT_FILTER)) {

                switch (filterGroup) {
                    case STATUS -> {
                        final Criteria statusCriteria = Criteria.eq(workflowTable.getColumn(OMWFLWORKFLOWV2.IS_ACTIVE),
                                WorkflowStatus.get(values.get(0).shortValue()).getId() == 1);

                        whereClause = whereClause.and(statusCriteria);
                    }
                    case TRIGGERS -> {
                        final Map<Short, Collection<Short>> triggers = new HashMap<>();
                        WhereClause triggerConditions = null;

                        values.stream().map(subField -> TriggerType.get(subField.shortValue()).name()).map(name -> name.split(UNDERSCORE))
                                .filter(index -> index.length >= 2).forEach(index -> triggers.computeIfAbsent(TriggerSource.valueOf(index[0]).getId(),
                                        event -> new ArrayList<>()).add(TriggerEvent.valueOf(index[1]).getId()));

                        for (final Map.Entry<Short, Collection<Short>> trigger : triggers.entrySet()) {
                            final WhereClause triggerWhereClause = new WhereClause(Criteria.eq(workflowTable.getColumn(OMWFLWORKFLOWV2.TRIGGER_SOURCE),
                                    trigger.getKey())).and(Criteria.in(workflowTable.getColumn(OMWFLWORKFLOWV2.TRIGGER_EVENT), trigger.getValue()));
                            triggerConditions = Objects.isNull(triggerConditions) ? triggerWhereClause : triggerConditions.or(triggerWhereClause);
                        }

                        if (Objects.nonNull(triggerConditions)) {
                            whereClause = whereClause.and(triggerConditions);
                        }
                    }
                    case CREATED_BY -> {
                        final Criteria createdByCriteria = Criteria.in(workflowTable.getColumn(OMWFLWORKFLOWV2.OWNER_ID), values);

                        whereClause = whereClause.and(createdByCriteria);
                    }
                    case ACTIONS -> {
                        final Criteria actionsCriteria = Criteria.in(actionTable.getColumn(OMWFLACTIONSV2.ACTION_EVENT), values);
                        final Criteria actionNodeTypeCriteria = Criteria.eq(nodeTable.getColumn(OMWFLNODE.NODE_TYPE), NodeType.ACTION.getId());

                        whereClause = whereClause.and(actionsCriteria).and(actionNodeTypeCriteria);
                    }
                }
            }
        }

        if (Objects.nonNull(searchText)) {
            final Criteria searchCriteria = Criteria.contains(workflowTable.getColumn(OMWFLWORKFLOWV2.NAME), searchText);

            whereClause = whereClause.and(searchCriteria);
        }

        return whereClause;
    }

    /**
     * <p>
     * Gets the {@link WhereClause} based on the provided {@link WorkflowExecutionHistoryFilter}
     * </p>
     *
     * @param workflowExecutionHistoryFilter Represents the {@link WorkflowExecutionHistoryFilter}
     * @return A {@link WhereClause} representing the criteria specified in the {@link WorkflowExecutionHistoryFilter}
     */
    WhereClause getExecutionHistoryFilterWhereClause(final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        final Table workflowTable = getWorkflowTable();
        final Table executionHistoryTable = getWorkflowExecutionHistoryTable();
        final Map<WorkflowExecutionHistoryFilterType, Long> criteria = workflowExecutionHistoryFilter.getCriteria();
        WhereClause whereClause = null;

        final BiFunction<WhereClause, Interval<Long, Long>, WhereClause> intervalWhereClause = (historyWhereClause, interval) -> {
            final Criteria dueDateCriteria = Criteria.between(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.WORKFLOW_EXECUTED_TIME), interval.start(), interval.end());

            return Objects.isNull(historyWhereClause) ? new WhereClause(dueDateCriteria) : historyWhereClause.and(dueDateCriteria);
        };

        for (final Map.Entry<WorkflowExecutionHistoryFilterType, Long> selectedCriteria : criteria.entrySet()) {
            final WorkflowExecutionHistoryFilterType workflowExecutionHistoryFilterType = selectedCriteria.getKey();
            final Long value = selectedCriteria.getValue();

            if (!value.equals(DEFAULT_FILTER)) {
                switch (workflowExecutionHistoryFilterType) {
                    case STATUS -> {
                        final WorkflowExecutionStatus workflowExecutionStatus = WorkflowExecutionStatus.valueOf(WorkflowExecutionStatusFilter.get(value.shortValue()).name());
                        final Criteria statusCriteria = Criteria.eq(executionHistoryTable.getColumn(OMWFLEXECUTIONHISTORY.STATUS), workflowExecutionStatus.getId());

                        whereClause = Objects.isNull(whereClause) ? new WhereClause(statusCriteria) : whereClause.and(statusCriteria);
                    }
                    case WORKFLOWS -> {
                        final Criteria workflowCriteria = Criteria.eq(workflowTable.getPKColumn(), value);

                        whereClause = Objects.isNull(whereClause) ? new WhereClause(workflowCriteria) : whereClause.and(workflowCriteria);
                    }
                    case DUE_DATE -> {
                        switch (DateIntervalType.get(value.shortValue())) {
                            case TODAY -> whereClause = intervalWhereClause.apply(whereClause, DateIntervals.TODAY.getInterval());
                            case YESTERDAY -> whereClause = intervalWhereClause.apply(whereClause, DateIntervals.YESTERDAY.getInterval());
                            case THIS_WEEK -> whereClause = intervalWhereClause.apply(whereClause, DateIntervals.THIS_WEEK.getInterval());
                            case LAST_WEEK -> whereClause = intervalWhereClause.apply(whereClause, DateIntervals.LAST_WEEK.getInterval());
                            case BETWEEN -> whereClause = intervalWhereClause.apply(whereClause, workflowExecutionHistoryFilter.getInterval());
                        }
                    }
                }
            }
        }

        return whereClause;
    }

    /**
     * History filters for {@code OMWFLEXECUTIONV2} joined to V2 workflow/version.
     *
     * @param workflowExecutionHistoryFilter filter payload
     * @return where clause or null
     */
    WhereClause buildExecutionHistoryFilterWhereClause(final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        final Table workflowTable = Table.get(OMWFLWORKFLOWV2.TABLE);
        final Table executionTable = Table.get(OMWFLEXECUTIONV2.TABLE);
        final Map<WorkflowExecutionHistoryFilterType, Long> criteria = workflowExecutionHistoryFilter.getCriteria();
        WhereClause whereClause = null;

        final BiFunction<WhereClause, Interval<Long, Long>, WhereClause> intervalWhereClause = (historyWhereClause, interval) -> {
            final Criteria dueDateCriteria = Criteria.between(executionTable.getColumn(OMWFLEXECUTIONV2.STARTED_TIME), interval.start(), interval.end());

            return Objects.isNull(historyWhereClause) ? new WhereClause(dueDateCriteria) : historyWhereClause.and(dueDateCriteria);
        };

        if (Objects.isNull(criteria)) {
            return null;
        }

        for (final Map.Entry<WorkflowExecutionHistoryFilterType, Long> selectedCriteria : criteria.entrySet()) {
            final WorkflowExecutionHistoryFilterType workflowExecutionHistoryFilterType = selectedCriteria.getKey();
            final Long value = selectedCriteria.getValue();

            if (!value.equals(DEFAULT_FILTER)) {
                switch (workflowExecutionHistoryFilterType) {
                    case STATUS -> {
                        final WorkflowExecutionStatusFilter statusFilter = WorkflowExecutionStatusFilter.get(value.shortValue());
                        final Criteria statusCriteria = switch (statusFilter) {
                            case SUCCESS ->
                                    Criteria.eq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.SUCCESS.getId());
                            case PENDING ->
                                    Criteria.eq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.PENDING.getId());
                            case STOPPED ->
                                    Criteria.eq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.STOPPED.getId());
                            case FAILURE ->
                                    Criteria.eq(executionTable.getColumn(OMWFLEXECUTIONV2.STATUS), ExecutionStatus.FAILED.getId());
                            case ALL -> null;
                        };

                        if (Objects.nonNull(statusCriteria)) {
                            whereClause = Objects.isNull(whereClause) ? new WhereClause(statusCriteria) : whereClause.and(statusCriteria);
                        }
                    }
                    case WORKFLOWS -> {
                        final Criteria workflowCriteria = Criteria.eq(workflowTable.getPKColumn(), value);

                        whereClause = Objects.isNull(whereClause) ? new WhereClause(workflowCriteria) : whereClause.and(workflowCriteria);
                    }
                    case DUE_DATE -> {
                        switch (DateIntervalType.get(value.shortValue())) {
                            case TODAY ->
                                    whereClause = intervalWhereClause.apply(whereClause, DateIntervals.TODAY.getInterval());
                            case YESTERDAY ->
                                    whereClause = intervalWhereClause.apply(whereClause, DateIntervals.YESTERDAY.getInterval());
                            case THIS_WEEK ->
                                    whereClause = intervalWhereClause.apply(whereClause, DateIntervals.THIS_WEEK.getInterval());
                            case LAST_WEEK ->
                                    whereClause = intervalWhereClause.apply(whereClause, DateIntervals.LAST_WEEK.getInterval());
                            case BETWEEN ->
                                    whereClause = intervalWhereClause.apply(whereClause, workflowExecutionHistoryFilter.getInterval());
                        }
                    }
                }
            }
        }

        return whereClause;
    }

}
