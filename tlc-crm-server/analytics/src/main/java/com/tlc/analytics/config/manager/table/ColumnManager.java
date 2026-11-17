package com.tlc.analytics.config.manager.table;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelationMapping;

import com.tlc.analytics.sql.resource.ANACOLUMNDETAILS;
import com.tlc.analytics.sql.resource.ANATABLEDETAILS;
import com.tlc.analytics.sql.resource.ANATABLERELATION;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * The ColumnManager class is responsible for managing the configuration and persistence of {@link ColumnDetail} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see AbstractAnalyticsConfigManager
 */
public class ColumnManager extends AbstractAnalyticsConfigManager<ColumnDetail> {

    private ColumnManager() {
    }

    private static class Instance {
        private static final ColumnManager INSTANCE = new ColumnManager();
    }

    /**
     * Returns singleton instance of {@link ColumnManager}.
     *
     * @return The {@link ColumnManager} instance.
     */
    public static ColumnManager INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ColumnDetail} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable() {
        return Table.get(ANACOLUMNDETAILS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link ColumnDetail} model class
     * </p>
     *
     * @return The {@link ColumnDetail}
     */
    @Override
    protected @NonNull Class<ColumnDetail> getModelClass() {
        return ColumnDetail.class;
    }

    /**
     * Fetches all {@link ColumnDetail} from the database.
     * <p>
     * Builds {@link TableDetail} with relations directly from the same query result
     * to avoid redundant DB lookups.
     * </p>
     *
     * @return {@link Collection} of {@link ColumnDetail}, never null
     */
    public Collection<ColumnDetail> fetchAll() {
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<ColumnDetail> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            final Map<Long, Collection<TableRelationMapping>> relationsMap = groupRelationsByParentTableId(dataContainer);
            final Map<Long, TableDetail> tableDetailCache = new HashMap<>();

            dataContainer.getRows(Table.get(ANACOLUMNDETAILS.TABLE)).forEach(row ->
                    result.add(buildColumnDetails(row, Objects.requireNonNull(tableDetailCache.computeIfAbsent(row.get(ANACOLUMNDETAILS.TABLE_ID), id ->
                            buildTableDetailFromDataContainer(dataContainer, id, relationsMap))))));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] column details from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link ColumnDetail} from the database for the given table ID.
     *
     * @param tableId Represents the table identifier
     * @return {@link Collection} of {@link ColumnDetail}, never null
     */
    public Collection<ColumnDetail> fetchByTableId(final Long tableId) {
        final Table columnTable = Table.get(ANACOLUMNDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(columnTable.getColumn(ANACOLUMNDETAILS.TABLE_ID), tableId));
        final Collection<ColumnDetail> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            final Map<Long, Collection<TableRelationMapping>> relationsMap = groupRelationsByParentTableId(dataContainer);
            final TableDetail tableDetail = buildTableDetailFromDataContainer(dataContainer, tableId, relationsMap);

            dataContainer.getRows(columnTable).forEach(row -> result.add(buildColumnDetails(row, tableDetail))
            );
        }

        LOGGER.info("CRM Analytics : Fetched [{}] column details from DB for tableId [{}]", result.size(), tableId);
        return result;
    }

    /**
     * Fetches a single {@link ColumnDetail} from the database for the given column ID.
     *
     * @param columnId Represents the column identifier
     * @return {@link ColumnDetail} or null if not found
     */
    public ColumnDetail fetchByColumnId(final Long columnId) {
        final Table columnTable = Table.get(ANACOLUMNDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(columnTable.getPKColumn(), columnId));

        if (Objects.isNull(dataContainer)) {
            LOGGER.info("CRM Analytics : No column detail found in DB for columnId [{}]", columnId);
            return null;
        }

        final Row columnRow = dataContainer.getRow(columnTable);

        if (Objects.isNull(columnRow)) {
            LOGGER.info("CRM Analytics : Column row not found in result for columnId [{}]", columnId);
            return null;
        }

        final Long tableId = columnRow.get(ANACOLUMNDETAILS.TABLE_ID);
        final Map<Long, Collection<TableRelationMapping>> relationsMap = groupRelationsByParentTableId(dataContainer);

        return buildColumnDetails(columnRow, Objects.requireNonNull(buildTableDetailFromDataContainer(dataContainer, tableId, relationsMap)));
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} with column-table-relation join.
     * Joins ANACOLUMNDETAILS → ANATABLEDETAILS → ANATABLERELATION in a single query
     * to avoid separate DB calls for table relations.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table columnTable = Table.get(ANACOLUMNDETAILS.TABLE);
        final Table tableDetailTable = Table.get(ANATABLEDETAILS.TABLE);
        final Table relationTable = Table.get(ANATABLERELATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(columnTable);

        selectQuery.addSelectClause(columnTable, tableDetailTable, relationTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(columnTable.getColumn(ANACOLUMNDETAILS.TABLE_ID), tableDetailTable.getPKColumn()),
                new JoinClause(tableDetailTable.getPKColumn(), relationTable.getColumn(ANATABLERELATION.PARENT_TABLE_ID))
        ));

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Pre-groups all relation rows by parentTableId into a Map for O(1) lookup,
     * avoiding repeated O(M) filtering inside loops.
     * </p>
     *
     * @param dataContainer The data container holding all fetched rows
     * @return Map of parentTableId → Collection of {@link TableRelationMapping}
     */
    private Map<Long, Collection<TableRelationMapping>> groupRelationsByParentTableId(final DataContainer dataContainer) {
        final Table relationTable = Table.get(ANATABLERELATION.TABLE);
        final Map<Long, Collection<TableRelationMapping>> relationsMap = new HashMap<>();

        dataContainer.getRows(relationTable).forEach(row -> {
            final Long parentTableId = row.get(ANATABLERELATION.PARENT_TABLE_ID);

            if (Objects.nonNull(parentTableId)) {
                relationsMap.computeIfAbsent(parentTableId, k -> new ArrayList<>())
                        .add(TableRelationMapping.builder()
                                .id(row.getPKValue())
                                .parentTableId(parentTableId)
                                .childTableId(row.get(ANATABLERELATION.CHILD_TABLE_ID))
                                .build());
            }
        });

        return relationsMap;
    }

    /**
     * <p>
     * Builds a {@link TableDetail} with its {@link TableRelationMapping} relations
     * directly from the already-fetched {@link DataContainer}, avoiding a separate DB call.
     * Uses pre-grouped relationsMap for O(1) lookup.
     * </p>
     *
     * @param dataContainer The data container from the query result
     * @param tableId       The table identifier
     * @param relationsMap  Pre-grouped relations by parentTableId
     * @return The constructed {@link TableDetail}, or null if not found
     */
    private TableDetail buildTableDetailFromDataContainer(final DataContainer dataContainer,
                                                          final Long tableId,
                                                          final Map<Long, Collection<TableRelationMapping>> relationsMap) {
        final Row tableRow = dataContainer.getRow(Table.get(ANATABLEDETAILS.TABLE), tableId);

        if (Objects.isNull(tableRow)) {
            return null;
        }

        return TableDetail.builder()
                .id(tableId)
                .tableName(tableRow.get(ANATABLEDETAILS.TABLE_NAME))
                .leftToRightRelations(relationsMap.getOrDefault(tableId, List.of()))
                .build();
    }

    /**
     * <p>
     * Builds a {@link ColumnDetail} instance from the given row data.
     * </p>
     *
     * @param row         The database row containing column data
     * @param tableDetail The associated {@link TableDetail}
     * @return The constructed {@link ColumnDetail}
     */
    private ColumnDetail buildColumnDetails(final Row row, final TableDetail tableDetail) {
        return ColumnDetail.builder()
                .id(row.getPKValue())
                .tableId(tableDetail.id())
                .dataType(row.get(ANACOLUMNDETAILS.DATA_TYPE_ID))
                .columnName(row.get(ANACOLUMNDETAILS.COLUMN_NAME))
                .tableDetail(tableDetail)
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
