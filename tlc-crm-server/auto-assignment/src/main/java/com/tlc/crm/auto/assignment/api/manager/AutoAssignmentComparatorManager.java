package com.tlc.crm.auto.assignment.api.manager;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.sql.resource.MASRCOMPARATOR;
import com.tlc.crm.auto.assignment.sql.resource.MASRFIELDTYPECOMPARATOR;

import com.tlc.crm.entity.sql.resource.METFTYPECOMPARATOR;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the {@link AutoAssignmentComparator} related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentComparatorManager extends AbstractReadableConfigManager<AutoAssignmentComparator> {

    private AutoAssignmentComparatorManager() {
    }

    private static class Instance {
        private static final AutoAssignmentComparatorManager COMPARATOR_MANAGER = new AutoAssignmentComparatorManager();
    }

    public static AutoAssignmentComparatorManager getInstance() {
        return Instance.COMPARATOR_MANAGER;
    }

    /**
     * Gets the {@link Table} of comparator.
     *
     * @return The table of comparator.
     */
    @Override
    public Table getTable() {
        return Table.get(MASRCOMPARATOR.TABLE);
    }

    /**
     * Fetches the table of the type comparator.
     *
     * @return Field type comparator table.
     */
    private Table getTypeComparatorTable() {
        return Table.get(MASRFIELDTYPECOMPARATOR.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param row Refers the row that holds the information of {@link AutoAssignmentComparator}.
     * @return The comparator.
     */
    @Override
    public AutoAssignmentComparator convertRowToModel(final Row row) {
        final AutoAssignmentComparator assignmentComparator = new AutoAssignmentComparator();

        assignmentComparator.setId(row.getPKValue());
        assignmentComparator.setName(row.get(MASRCOMPARATOR.NAME));
        assignmentComparator.setAliasName(row.get(MASRCOMPARATOR.ALIAS_NAME));
        assignmentComparator.setHasValue(row.get(MASRCOMPARATOR.HAS_VALUE));

        return assignmentComparator;
    }

    /**
     * Fetches all the {@link AutoAssignmentComparator}.
     *
     * @return The collection of {@link AutoAssignmentComparator}.
     */
    public Map<Long, Collection<AutoAssignmentComparator>> getAll() {
        final Table comparatorTable = getTable();
        final Table typeComparatorTable = getTypeComparatorTable();
        final SelectQuery selectQuery = SelectQuery.get(comparatorTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(comparatorTable, typeComparatorTable));
        selectQuery.addSelectClause(comparatorTable, typeComparatorTable);

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(typeComparatorTable).collect(Collectors.groupingBy(typeComparatorRow ->
                typeComparatorRow.get(MASRFIELDTYPECOMPARATOR.FIELD_TYPE_ID), Collectors.mapping(typeComparatorRow -> {
            final Row comparatorRow = dataContainer.getRow(comparatorTable, (Long) typeComparatorRow.get(METFTYPECOMPARATOR.COMPARATOR_ID));

            return convertRowToModel(comparatorRow);
        }, Collectors.toCollection(ArrayList::new))));
    }

    /**
     * Gets the {@link Collection} of comparators for the given field type id.
     *
     * @param fieldTypeId Represents the field type id.
     * @return The collection of {@link AutoAssignmentComparator} associated with the field type.
     */
    public Collection<AutoAssignmentComparator> getComparatorsByFieldType(final Long fieldTypeId) {
        final Table comparatorTable = getTable();
        final Table typeComparatorTable = getTypeComparatorTable();
        final SelectQuery selectQuery = SelectQuery.get(comparatorTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(comparatorTable, typeComparatorTable));
        selectQuery.addSelectClause(comparatorTable, typeComparatorTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(typeComparatorTable.getColumn(MASRFIELDTYPECOMPARATOR.FIELD_TYPE_ID), fieldTypeId)));

        return readableDataStore().get(selectQuery).getRows(comparatorTable).map(this::convertRowToModel).toList();
    }
}
