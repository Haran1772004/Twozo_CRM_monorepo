package com.tlc.analytics.config.manager.table;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.config.model.table.TableRelation;

import com.tlc.analytics.sql.resource.ANACOLUMNRELATION;
import com.tlc.analytics.sql.resource.ANATABLERELATION;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * The LookupManager class is responsible for managing the configuration and persistence of {@link TableRelation} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see AbstractAnalyticsConfigManager
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LookupManager extends AbstractAnalyticsConfigManager<TableRelation> {
    private static class Instance {
        private static final LookupManager INSTANCE = new LookupManager();
    }

    /**
     * Returns singleton instance of {@link LookupManager}.
     *
     * @return The {@link LookupManager} instance.
     */
    public static LookupManager INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link TableRelation} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable() {
        return Table.get(ANATABLERELATION.TABLE);
    }

    /**
     * <p>
     * Gets the {@link TableRelation} model class
     * </p>
     *
     * @return The {@link TableRelation}
     */
    @Override
    protected @NonNull Class<TableRelation> getModelClass() {
        return TableRelation.class;
    }

    /**
     * Fetches all {@link TableRelation} from the database.
     *
     * @return {@link Collection} of {@link TableRelation}, never null
     */
    public Collection<TableRelation> fetchAll() {
        final Table table = Table.get(ANATABLERELATION.TABLE);
        final Table columnRelationTable = Table.get(ANACOLUMNRELATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), columnRelationTable.getColumn(ANACOLUMNRELATION.REL_ID)));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Collection<TableRelation> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            final Map<Long, ColumnRelation> columnRelationsByRelId = groupColumnRelationsByRelId(dataContainer, columnRelationTable);

            dataContainer.getRows(table).forEach(row -> {
                final Long relationId = row.getPKValue();

                result.add(TableRelation.builder()
                        .id(relationId)
                        .parentTableId(row.get(ANATABLERELATION.PARENT_TABLE_ID))
                        .childTableId(row.get(ANATABLERELATION.CHILD_TABLE_ID))
                        .relTypeId(row.get(ANATABLERELATION.REL_TYPE))
                        .columnRelation(columnRelationsByRelId.get(relationId))
                        .build());
            });
        }

        LOGGER.info("CRM Analytics : Fetched [{}] TableRelation from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link TableRelation} from the database for the given parent table ID.
     *
     * @param parentTableId Represents the parent table identifier
     * @return {@link Collection} of {@link TableRelation}, never null
     */
    public Collection<TableRelation> fetchByParentTableId(final Long parentTableId) {
        final Table table = Table.get(ANATABLERELATION.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getColumn(ANATABLERELATION.PARENT_TABLE_ID), parentTableId));
        final Collection<TableRelation> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildTableRelation(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] TableRelation from DB for parentTableId [{}]", result.size(), parentTableId);
        return result;
    }

    /**
     * Fetches a single {@link TableRelation} from the database for the given ID.
     *
     * @param relationId Represents the entity identifier
     * @return {@link TableRelation} or null if not found
     */
    public TableRelation fetchByRelationId(final Long relationId) {
        final Table table = Table.get(ANATABLERELATION.TABLE);
        final Table columnRelationTable = Table.get(ANACOLUMNRELATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), columnRelationTable.getColumn(ANACOLUMNRELATION.REL_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), relationId)));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                final Row relatedColRow = dataContainer.getRow(columnRelationTable);

                return TableRelation.builder()
                        .id(relationId)
                        .parentTableId(row.get(ANATABLERELATION.PARENT_TABLE_ID))
                        .childTableId(row.get(ANATABLERELATION.CHILD_TABLE_ID))
                        .relTypeId(row.get(ANATABLERELATION.REL_TYPE))
                        .columnRelation(Objects.nonNull(relatedColRow) ? buildColumnRelations(relatedColRow) : null)
                        .build();
            }
        }

        LOGGER.info("CRM Analytics : No TableRelation found in DB for relationId [{}]", relationId);
        return null;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the table relation table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(ANATABLERELATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds a {@link TableRelation} instance from the given row data.
     * </p>
     *
     * @param row The database row containing table relation data
     * @return The constructed {@link TableRelation}
     */
    private TableRelation buildTableRelation(final Row row) {
        return TableRelation.builder()
                .id(row.getPKValue())
                .parentTableId(row.get(ANATABLERELATION.PARENT_TABLE_ID))
                .childTableId(row.get(ANATABLERELATION.CHILD_TABLE_ID))
                .relTypeId(row.get(ANATABLERELATION.REL_TYPE))
                .build();
    }

    /**
     * Pre-groups related column rows by relId into a Map for O(1) lookup.
     *
     * @param dataContainer      The data container holding all fetched rows
     * @param columnRelationTable The related column table reference
     * @return Map of relId → {@link ColumnRelation}
     */
    private Map<Long, ColumnRelation> groupColumnRelationsByRelId(final DataContainer dataContainer,
                                                                 final Table columnRelationTable) {
        final Map<Long, ColumnRelation> columnRelationsMap = new HashMap<>();

        dataContainer.getRows(columnRelationTable).forEach(row -> {
            final Long relId = row.get(ANACOLUMNRELATION.REL_ID);

            if (Objects.nonNull(relId)) {
                columnRelationsMap.put(relId, buildColumnRelations(row));
            }
        });

        return columnRelationsMap;
    }

    /**
     * <p>
     * Builds a {@link ColumnRelation} instance from the given row data.
     * </p>
     *
     * @param row The database row containing related column data
     * @return The constructed {@link ColumnRelation}
     */
    private ColumnRelation buildColumnRelations(final Row row) {
        return ColumnRelation.builder()
                .id(row.getPKValue())
                .relId(row.get(ANACOLUMNRELATION.REL_ID))
                .parentColId(row.get(ANACOLUMNRELATION.PARENT_COL_ID))
                .childColId(row.get(ANACOLUMNRELATION.CHILD_COL_ID))
                .joinCondition(row.get(ANACOLUMNRELATION.REL_CONDITION))
                .build();
    }


    /**
     * Returns the {@link ReadableDataStore} instance for executing read queries.
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }
}
