package com.tlc.crm.entity.filter.api.manager.impl;

import com.tlc.crm.entity.filter.api.manager.ComparatorManager;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.sql.resource.METFCOMPARATOR;
import com.tlc.crm.entity.sql.resource.METFSUBTYPECOMPARATOR;
import com.tlc.crm.entity.sql.resource.METFTYPECOMPARATOR;
import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the {@link Comparator} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractReadableConfigManager
 */
public class ComparatorManagerImpl extends AbstractReadableConfigManager<Comparator> implements ComparatorManager {

    private static class Instance {
        private static final ComparatorManagerImpl INSTANCE = new ComparatorManagerImpl();
    }

    private ComparatorManagerImpl() {
    }

    public static ComparatorManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return METFComparator table.
     */
    @Override
    public Table getTable() {
        return Table.get(METFCOMPARATOR.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the type comparator.
     * </p>
     *
     * @return METFTypeComparator table.
     */
    private Table getTypeComparatorTable() {
        return Table.get(METFTYPECOMPARATOR.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the subtype comparator.
     * </p>
     *
     * @return METFSubTypeComparator table.
     */
    private Table getSubTypeComparatorTable() {
        return Table.get(METFSUBTYPECOMPARATOR.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param row which contains the values of comparator.
     * @return the values of {@link Comparator}.
     */
    @Override
    public Comparator convertRowToModel(final Row row) {
        final Comparator comparator = new Comparator();

        comparator.setId(row.getPKValue());
        comparator.setName(row.get(METFCOMPARATOR.NAME));
        comparator.setKeyName(row.get(METFCOMPARATOR.KEY_NAME));
        comparator.setAliasName(row.get(METFCOMPARATOR.ALIAS_NAME));
        comparator.setValueNeeded(row.get(METFCOMPARATOR.IS_VALUE_NEEDED));
        comparator.setMultiValueAllowed(row.get(METFCOMPARATOR.IS_MULTI_VALUE_ALLOWED));
        comparator.setIsDefault(row.get(METFCOMPARATOR.IS_DEFAULT));

        return comparator;
    }

    /**
     * {@inheritDoc}
     *
     * @return collection of {@link Comparator} instance that holds the retrieved values.
     */
    @Override
    public Collection<Comparator> getAll() {
        final Table comparatorTable = getTable();
        final Table typeComparatorTable = getTypeComparatorTable();
        final Table subTypeComparatorTable = getSubTypeComparatorTable();
        final SelectQuery selectQuery = SelectQuery.get(comparatorTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(new JoinClause(comparatorTable.getPKColumn(), typeComparatorTable.getColumn(METFTYPECOMPARATOR.COMPARATOR_ID), leftJoin));
        selectQuery.addJoinClause(new JoinClause(comparatorTable.getPKColumn(), subTypeComparatorTable.getColumn(METFSUBTYPECOMPARATOR.COMPARATOR_ID), leftJoin));
        selectQuery.addSelectClause(comparatorTable, typeComparatorTable, subTypeComparatorTable);

        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Stream<Row> typeComparatorRows = dataContainer.getRows(typeComparatorTable);
        final Stream<Row> subTypeComparatorRows = dataContainer.getRows(subTypeComparatorTable);
        final Collection<Comparator> comparators = new ArrayList<>();
        final Collection<Long> comparatorIds = new HashSet<>();

        typeComparatorRows.forEach(typeComparatorRow -> {
            final Long comparatorId = typeComparatorRow.get(METFTYPECOMPARATOR.COMPARATOR_ID);
            final Row comparatorRow = dataContainer.getRow(comparatorTable, comparatorId);
            final Comparator comparator = convertRowToModel(comparatorRow);

            comparator.setSpecialization(Comparator.Specialization.TYPE);
            comparator.setSpecializationId(typeComparatorRow.get(METFTYPECOMPARATOR.FIELD_TYPE_ID));
            comparator.setOrder(typeComparatorRow.get(METFTYPECOMPARATOR.ORDER));
            comparators.add(comparator);
            comparatorIds.add(comparatorId);
        });

        subTypeComparatorRows.forEach(subTypeComparatorRow -> {
            final Long comparatorId = subTypeComparatorRow.get(METFSUBTYPECOMPARATOR.COMPARATOR_ID);
            final Row comparatorRow = dataContainer.getRow(comparatorTable, comparatorId);
            final Comparator comparator = convertRowToModel(comparatorRow);

            comparator.setSpecialization(Comparator.Specialization.SUB_TYPE);
            comparator.setSpecializationId(subTypeComparatorRow.get(METFSUBTYPECOMPARATOR.FIELD_SUB_TYPE_ID));
            comparator.setOrder(subTypeComparatorRow.get(METFSUBTYPECOMPARATOR.ORDER));
            comparators.add(comparator);
            comparatorIds.add(comparatorId);
        });
        final WhereClause whereClause = new WhereClause(Criteria.notIn(comparatorTable.getPKColumn(), comparatorIds));

        comparators.addAll(dataContainer.getRows(comparatorTable, whereClause).map(this::convertRowToModel).toList());
        return comparators;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id
     * @return The {@link Comparator}
     */
    @Override
    public Collection<Comparator> getComparatorsByType(final Long fieldTypeId) {
        final Table comparatorTable = getTable();
        final Table typeComparatorTable = getTypeComparatorTable();
        final SelectQuery selectQuery = SelectQuery.get(comparatorTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(comparatorTable, typeComparatorTable));
        selectQuery.addSelectClause(comparatorTable, typeComparatorTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(typeComparatorTable.getColumn(METFTYPECOMPARATOR.FIELD_TYPE_ID), fieldTypeId)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(typeComparatorTable).map(typeComparatorRow -> {
            final Row comparatorRow = dataContainer.getRow(comparatorTable, (Long) typeComparatorRow.get(METFTYPECOMPARATOR.COMPARATOR_ID));
            final Comparator comparator = convertRowToModel(comparatorRow);

            comparator.setSpecialization(Comparator.Specialization.TYPE);
            comparator.setSpecializationId(typeComparatorRow.get(METFTYPECOMPARATOR.FIELD_TYPE_ID));
            comparator.setOrder(typeComparatorRow.get(METFTYPECOMPARATOR.ORDER));
            return comparator;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldSubTypeId Represents the field subtype id
     * @return The {@link Comparator}
     */
    @Override
    public Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId) {
        final Table comparatorTable = getTable();
        final Table subTypeComparatorTable = getSubTypeComparatorTable();
        final SelectQuery selectQuery = SelectQuery.get(comparatorTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(comparatorTable, subTypeComparatorTable));
        selectQuery.addSelectClause(comparatorTable, subTypeComparatorTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(subTypeComparatorTable.getColumn(METFSUBTYPECOMPARATOR.FIELD_SUB_TYPE_ID), fieldSubTypeId)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(subTypeComparatorTable).map(subTypeComparatorRow -> {
            final Row comparatorRow = dataContainer.getRow(comparatorTable, (Long) subTypeComparatorRow.get(METFSUBTYPECOMPARATOR.COMPARATOR_ID));
            final Comparator comparator = convertRowToModel(comparatorRow);

            comparator.setSpecialization(Comparator.Specialization.SUB_TYPE);
            comparator.setSpecializationId(subTypeComparatorRow.get(METFSUBTYPECOMPARATOR.FIELD_SUB_TYPE_ID));
            comparator.setOrder(subTypeComparatorRow.get(METFSUBTYPECOMPARATOR.ORDER));
            return comparator;
        }).toList();
    }
}