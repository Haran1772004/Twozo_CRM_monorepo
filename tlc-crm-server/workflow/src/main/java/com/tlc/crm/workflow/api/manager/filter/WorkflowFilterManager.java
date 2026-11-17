package com.tlc.crm.workflow.api.manager.filter;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.workflow.api.model.filter.FilterGroup;
import com.tlc.crm.workflow.api.model.filter.Type;
import com.tlc.crm.workflow.api.model.filter.WorkflowFilter;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilterType;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilter;
import com.tlc.crm.workflow.sql.resource.OMWFLFILTERVALUES;
import com.tlc.crm.workflow.sql.resource.OMWFLFILTER;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Manages the {@link WorkflowFilter} related DB actions
 * </p>
 *
 * @author Kavinila E, karthick V
 * @version 1.0
 */
public final class WorkflowFilterManager {

    private final WorkflowFilterQueryBuilder workflowFilterQueryBuilder;

    private static class Instance {
        private static final WorkflowFilterManager INSTANCE = new WorkflowFilterManager();
    }

    private WorkflowFilterManager() {
        this.workflowFilterQueryBuilder = WorkflowFilterQueryBuilder.getInstance();
    }

    /**
     * Gets the {@link WorkflowFilterManager} instance
     *
     * @return {@link WorkflowFilterManager}
     */
    public static WorkflowFilterManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link Table} of workflow filter
     *
     * @return The workflow filter table
     */
    public Table getTable() {
        return Table.get(OMWFLFILTER.TABLE);
    }

    /**
     * Gets the {@link Table} of filter value
     *
     * @return The filter value table
     */
    public Table getFilterValueTable() {
        return Table.get(OMWFLFILTERVALUES.TABLE);
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link WorkflowFilter} for the given org id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link DataContainer}
     */
    public DataContainer getWorkflowFilterContainer(final long orgId, final long userId, final Type type) {
        final Table workflowFilterTable = getTable();
        final Table filterValueTable = getFilterValueTable();
        final SelectQuery selectQuery = SelectQuery.get(workflowFilterTable);

        selectQuery.addSelectClause(workflowFilterTable, filterValueTable);
        selectQuery.addJoinClause(new JoinClause(workflowFilterTable.getPKColumn(), filterValueTable.getColumn(OMWFLFILTERVALUES.FILTER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(workflowFilterTable.getColumn(OMWFLFILTER.ORG_ID), orgId))
                .and(Criteria.eq(workflowFilterTable.getColumn(OMWFLFILTER.USER_ID), userId))
                .and(Criteria.eq(workflowFilterTable.getColumn(OMWFLFILTER.TYPE), type.getId())));
        selectQuery.addOrderByClause(new OrderByClause(workflowFilterTable.getColumn(OMWFLFILTER.FILTER_GROUP)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Gets the {@link WorkflowFilter} for the given org id and user id
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return {@link Collection} of {@link WorkflowFilter}
     */
    public WorkflowFilter getWorkflowFilter(final long orgId, final long userId, final Type type) {
        final DataContainer dataContainer = getWorkflowFilterContainer(orgId, userId, type);

        if (dataContainer.isEmpty()) {
            return null;
        } else {
            final Table filterTable = getTable();
            final Table filterValueTable = getFilterValueTable();
            final WorkflowFilter workflowFilter = new WorkflowFilter();
            final Map<FilterGroup, Collection<Long>> conditions = new LinkedHashMap<>();

            dataContainer.getRows(filterTable).forEach(filterRow -> {
                final FilterGroup filterGroup = FilterGroup.get(filterRow.get(OMWFLFILTER.FILTER_GROUP));
                final Collection<Long> values = dataContainer.getRows(filterValueTable,
                                new WhereClause(Criteria.eq(filterValueTable.getColumn(OMWFLFILTERVALUES.FILTER_ID), filterRow.getPKValue())))
                        .map(valueRow -> (Long) valueRow.get(OMWFLFILTERVALUES.VALUE)).collect(Collectors.toUnmodifiableSet());

                conditions.put(filterGroup, values);
            });
            workflowFilter.setCriteria(conditions);

            return workflowFilter;
        }
    }

    /**
     * <p>
     * Loads the {@link WorkflowFilter} details into the {@link DataContainer}
     * </p>
     *
     * @param workflowFilter It holds the {@link WorkflowFilter} details
     */
    public void loadWorkflowFilterIntoContainer(final WorkflowFilter workflowFilter) {
        final long orgId = workflowFilter.getOrgId();
        final long userId = workflowFilter.getUserId();
        final Type type = workflowFilter.getType();
        final DataContainer dataContainer = getWorkflowFilterContainer(orgId, userId, type);
        final DataContainer targetContainer = DataContainer.create();
        final Table filterTable = getTable();
        final Table filterValueTable = getFilterValueTable();
        final short typeId = type.getId();

        workflowFilter.getCriteria().forEach((filterGroup, values) -> {
            final Row filterRow = new Row(filterTable);
            final Long filterId = filterRow.getPKValue();

            filterRow.setOrgId(orgId);
            filterRow.set(OMWFLFILTER.USER_ID, userId);
            filterRow.set(OMWFLFILTER.TYPE, typeId);
            filterRow.set(OMWFLFILTER.FILTER_GROUP, filterGroup.getId());

            targetContainer.addNewRow(filterRow);

            for (final Long value : values) {
                final Row filterValueRow = new Row(filterValueTable);

                filterValueRow.set(OMWFLFILTERVALUES.FILTER_ID, filterId);
                filterValueRow.set(OMWFLFILTERVALUES.VALUE, value);
                filterValueRow.setOrgId(orgId);

                targetContainer.addNewRow(filterValueRow);
            }
        });

        if (!dataContainer.isEmpty()) {
            final Stream<Row> filterRows = dataContainer.getRows(filterTable, new WhereClause(Criteria.eq(filterTable.getColumn(OMWFLFILTER.ORG_ID), orgId))
                    .and(Criteria.eq(filterTable.getColumn(OMWFLFILTER.USER_ID), userId)));

            filterRows.forEach(targetContainer::deleteRow);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the {@link WhereClause} based on the provided {@link WorkflowFilter}
     * </p>
     *
     * @param workflowFilter {@link WorkflowFilter}
     * @return A {@link WhereClause} representing the criteria specified in the {@link WorkflowFilter}
     */
    public WhereClause getExecutionHistoryFilterWhereClause(final WorkflowFilter workflowFilter) {
        return workflowFilterQueryBuilder.getExecutionHistoryFilterWhereClause(workflowFilter);
    }

    /**
     * Gets the {@link WhereClause} based on the provided {@link WorkflowFilter} for workflow V2 tables.
     *
     * @param workflowFilter {@link WorkflowFilter}
     * @return A {@link WhereClause} representing the criteria specified in the {@link WorkflowFilter}
     */
    public WhereClause buildWorkflowFilterWhereClause(final WorkflowFilter workflowFilter) {
        return workflowFilterQueryBuilder.buildWorkflowFilterWhereClause(workflowFilter);
    }

    /**
     * <p>
     * Loads the {@link WorkflowExecutionHistoryFilter} rows into container
     * </p>
     *
     * @param workflowExecutionHistoryFilter An entity which holds the {@link WorkflowExecutionHistoryFilter} attributes
     */
    public void loadExecutionHistoryFilterRowIntoContainer(final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        final long orgId = workflowExecutionHistoryFilter.getOrgId();
        final long userId = workflowExecutionHistoryFilter.getUserId();
        final Type type = workflowExecutionHistoryFilter.getType();
        final Table filterTable = getTable();
        final DataContainer targetContainer = DataContainer.create();
        final Table filterValueTable = getFilterValueTable();
        final DataContainer dataContainer = getWorkflowFilterContainer(orgId, userId, type);
        final short typeId = type.getId();

        workflowExecutionHistoryFilter.getCriteria().forEach((filterGroup, values) -> {
            if (Objects.equals(WorkflowExecutionHistoryFilterType.WORKFLOWS, filterGroup)) {
                final Row filterRow = new Row(filterTable);
                final Row filterValueRow = new Row(filterValueTable);

                filterRow.setOrgId(orgId);
                filterRow.set(OMWFLFILTER.USER_ID, userId);
                filterRow.set(OMWFLFILTER.TYPE, typeId);
                filterRow.set(OMWFLFILTER.FILTER_GROUP, filterGroup.getId());

                filterValueRow.set(OMWFLFILTERVALUES.FILTER_ID, filterRow.getPKValue());
                filterValueRow.set(OMWFLFILTERVALUES.VALUE, values);

                targetContainer.addNewRow(filterRow);
                targetContainer.addNewRow(filterValueRow);
            }
        });

        if (!dataContainer.isEmpty()) {
            final Row row = dataContainer.getRow(filterTable, new WhereClause(Criteria.eq(filterTable.getColumn(OMWFLFILTER.ORG_ID), orgId))
                    .and(Criteria.eq(filterTable.getColumn(OMWFLFILTER.USER_ID), userId)));

            targetContainer.deleteRow(row);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the {@link WhereClause} based on the provided {@link WorkflowExecutionHistoryFilter}
     * </p>
     *
     * @return A {@link WhereClause} representing the criteria specified in the {@link WorkflowExecutionHistoryFilter}
     */
    public WhereClause getExecutionHistoryFilterWhereClause(final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        return workflowFilterQueryBuilder.getExecutionHistoryFilterWhereClause(workflowExecutionHistoryFilter);
    }

    /**
     * Filter clause against V2 execution tables.
     *
     * @param workflowExecutionHistoryFilter history filter
     * @return where clause
     */
    public WhereClause buildExecutionHistoryFilterWhereClause(final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter) {
        return workflowFilterQueryBuilder.buildExecutionHistoryFilterWhereClause(workflowExecutionHistoryFilter);
    }
}
