package com.tlc.crm.workflow.api.manager;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;

import com.tlc.crm.entity.sql.resource.METFTYPECOMPARATOR;

import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.sql.resource.MWFLCOMPARATOR;
import com.tlc.crm.workflow.sql.resource.MWFLFIELDTYPECOMPARATOR;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the {@link WorkflowComparator} related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowComparatorManager extends AbstractReadableConfigManager<WorkflowComparator> {

    private WorkflowComparatorManager() {
    }

    private static class Instance {
        private static final WorkflowComparatorManager INSTANCE = new WorkflowComparatorManager();
    }

    public static WorkflowComparatorManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link Table} of comparator.
     *
     * @return The table of comparator.
     */
    @Override
    public Table getTable() {
        return Table.get(MWFLCOMPARATOR.TABLE);
    }

    /**
     * Fetches the table of the type comparator.
     *
     * @return Field type comparator table.
     */
    private Table getTypeComparatorTable() {
        return Table.get(MWFLFIELDTYPECOMPARATOR.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param row Refers the row that holds the information of {@link WorkflowComparator}.
     * @return The comparator.
     */
    @Override
    public WorkflowComparator convertRowToModel(final Row row) {
        return new WorkflowComparator(row.getPKValue(), row.get(MWFLCOMPARATOR.NAME), row.get(MWFLCOMPARATOR.ALIAS_NAME),
                row.get(MWFLCOMPARATOR.HAS_VALUE), WorkflowComparator.Category.get(row.get(MWFLCOMPARATOR.CATEGORY)));
    }

    /**
     * Fetches all the {@link WorkflowComparator}.
     *
     * @return The collection of {@link WorkflowComparator}.
     */
    public Map<Long, Collection<WorkflowComparator>> getAll() {
        final Table comparatorTable = getTable();
        final Table typeComparatorTable = getTypeComparatorTable();
        final SelectQuery selectQuery = SelectQuery.get(comparatorTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(comparatorTable, typeComparatorTable));
        selectQuery.addSelectClause(comparatorTable, typeComparatorTable);

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(typeComparatorTable).collect(Collectors.groupingBy(typeComparatorRow ->
                typeComparatorRow.get(MWFLFIELDTYPECOMPARATOR.FIELD_TYPE_ID), Collectors.mapping(typeComparatorRow -> {
            final Row comparatorRow = dataContainer.getRow(comparatorTable, (Long) typeComparatorRow.get(METFTYPECOMPARATOR.COMPARATOR_ID));

            return convertRowToModel(comparatorRow);
        }, Collectors.toCollection(ArrayList::new))));
    }

    /**
     * Gets the {@link Collection} of comparators for the given field type id.
     *
     * @param fieldTypeId Represents the field type id
     * @return The collection of {@link WorkflowComparator} associated with the field type.
     */
    public Collection<WorkflowComparator> getComparatorsByFieldType(final Long fieldTypeId) {
        final Table comparatorTable = getTable();
        final Table typeComparatorTable = getTypeComparatorTable();
        final SelectQuery selectQuery = SelectQuery.get(comparatorTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(comparatorTable, typeComparatorTable));
        selectQuery.addSelectClause(comparatorTable, typeComparatorTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(typeComparatorTable.getColumn(MWFLFIELDTYPECOMPARATOR.FIELD_TYPE_ID), fieldTypeId)));

        return readableDataStore().get(selectQuery).getRows(comparatorTable).map(this::convertRowToModel).toList();
    }
}
