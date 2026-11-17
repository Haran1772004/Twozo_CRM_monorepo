package com.tlc.analytics.config.manager.table;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelationMapping;
import com.tlc.analytics.sql.resource.ANAMODULETABLEMAPPER;
import com.tlc.analytics.sql.resource.ANATABLEDETAILS;
import com.tlc.analytics.sql.resource.ANATABLERELATION;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * The TableManager class is responsible for managing the configuration and persistence of {@link TableDetail} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see AbstractAnalyticsConfigManager
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TableManager extends AbstractAnalyticsConfigManager<TableDetail> {
    private static class Instance {
        private static final TableManager INSTANCE = new TableManager();
    }

    /**
     * Returns singleton instance of {@link TableManager}.
     *
     * @return The {@link TableManager} instance.
     */
    public static TableManager INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link TableDetail} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable() {
        return Table.get(ANATABLEDETAILS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link TableDetail} model class
     * </p>
     *
     * @return The {@link TableDetail}
     */
    @Override
    protected @NonNull Class<TableDetail> getModelClass() {
        return TableDetail.class;
    }

    /**
     * Fetches the {@link Collection} of {@link TableDetail} from the database
     * for the given module ID by looking up mapped table IDs and fetching their details.
     *
     * @param moduleId Represents the module identifier
     * @return {@link Collection} of {@link TableDetail}, never null
     */
    public Collection<TableDetail> fetchByModuleId(final Integer moduleId) {
        final Table mapperTable = Table.get(ANAMODULETABLEMAPPER.TABLE);
        final Table tableDetailsTable = Table.get(ANATABLEDETAILS.TABLE);
        final Table relationTable = Table.get(ANATABLERELATION.TABLE);
        final Column tableDetailsTablePKColumn = tableDetailsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(mapperTable);

        selectQuery.addJoinClause(List.of(new JoinClause(mapperTable.getColumn(ANAMODULETABLEMAPPER.TABLE_ID), tableDetailsTablePKColumn),
                new JoinClause(tableDetailsTablePKColumn, relationTable.getColumn(ANATABLERELATION.PARENT_TABLE_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(mapperTable.getColumn(ANAMODULETABLEMAPPER.MODULE_ID), moduleId)));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Collection<TableDetail> result = buildTableDetailsFromRows(dataContainer, tableDetailsTable, relationTable);

        LOGGER.info("CRM Analytics : Fetched [{}] table details from DB for moduleId [{}]", result.size(), moduleId);
        return result;
    }

    /**
     * Fetches a single {@link TableDetail} from the database for the given table ID.
     *
     * @param tableId Represents the table identifier
     * @return {@link TableDetail} or null if not found
     */
    public TableDetail fetchByTableId(final Long tableId) {
        final Table tableDetailsTable = Table.get(ANATABLEDETAILS.TABLE);
        final Table relationTable = Table.get(ANATABLERELATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(tableDetailsTable);

        selectQuery.addJoinClause(new JoinClause(tableDetailsTable.getPKColumn(), relationTable.getColumn(ANATABLERELATION.PARENT_TABLE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(tableDetailsTable.getPKColumn(), tableId)));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(tableDetailsTable);

            if (Objects.nonNull(row)) {
                return buildTableDetail(row, tableId, fetchRelationsByParentTableId(dataContainer.getRows(relationTable)));
            }
        }

        LOGGER.info("CRM Analytics : No table detail found in DB for tableId [{}]", tableId);
        return null;
    }

    /**
     * <p>
     * Builds a {@link TableDetail} instance from the given row data and relations.
     * </p>
     *
     * @param row                  The database row containing table detail data
     * @param tableId              The table identifier
     * @param leftToRightRelations The collection of {@link TableRelationMapping} for this table
     * @return The constructed {@link TableDetail}
     */
    private TableDetail buildTableDetail(final Row row, final Long tableId, final Collection<TableRelationMapping> leftToRightRelations) {
        return TableDetail.builder()
                .id(tableId)
                .tableName(row.get(ANATABLEDETAILS.TABLE_NAME))
                .leftToRightRelations(leftToRightRelations)
                .build();
    }

    /**
     * Builds a collection of {@link TableDetail} from the given data container by
     * pre-grouping relations and iterating over table detail rows.
     *
     * @param dataContainer   The data container holding all fetched rows
     * @param tableDetailsTable The table details table reference
     * @param relationTable   The relation table reference
     * @return {@link Collection} of {@link TableDetail}, never null
     */
    private Collection<TableDetail> buildTableDetailsFromRows(final DataContainer dataContainer,
                                                               final Table tableDetailsTable,
                                                               final Table relationTable) {
        final Collection<TableDetail> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            final Map<Long, Collection<TableRelationMapping>> relationsMap = groupRelationsByParentTableId(dataContainer, relationTable);

            dataContainer.getRows(tableDetailsTable).forEach(row -> {
                final Long tableId = row.getPKValue();

                if (Objects.nonNull(tableId)) {
                    result.add(buildTableDetail(row, tableId, relationsMap.getOrDefault(tableId, List.of())));
                }
            });
        }

        return result;
    }

    /**
     * Builds a collection of {@link TableRelationMapping} from the given stream of relation rows.
     *
     * @param relationRows the stream of {@link Row} representing table relation records
     * @return {@link Collection} of {@link TableRelationMapping}, never null
     */
    private Collection<TableRelationMapping> fetchRelationsByParentTableId(final Stream<Row> relationRows) {
        if (Objects.isNull(relationRows)) {
            return List.of();
        }

        return relationRows.<TableRelationMapping>map(row -> TableRelationMapping.builder()
                        .id(row.getPKValue())
                        .parentTableId(row.get(ANATABLERELATION.PARENT_TABLE_ID))
                        .childTableId(row.get(ANATABLERELATION.CHILD_TABLE_ID))
                        .build())
                .toList();
    }

    /**
     * Pre-groups all relation rows by parentTableId into a Map for O(1) lookup,
     * avoiding repeated O(M) filtering inside loops.
     *
     * @param dataContainer The data container holding all fetched rows
     * @param relationTable The relation table reference
     * @return Map of parentTableId → Collection of {@link TableRelationMapping}
     */
    private Map<Long, Collection<TableRelationMapping>> groupRelationsByParentTableId(final DataContainer dataContainer,
                                                                                      final Table relationTable) {
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
     * Returns the {@link ReadableDataStore} instance for executing read queries.
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * Fetches all {@link TableDetail} grouped by module ID from the database.
     *
     * @return {@link Map} of moduleId to {@link Collection} of {@link TableDetail}, never null
     */
    public Map<Integer, Collection<TableDetail>> fetchAllGroupedByModuleId() {
        final Table mapperTable = Table.get(ANAMODULETABLEMAPPER.TABLE);
        final Table tableDetailsTable = Table.get(ANATABLEDETAILS.TABLE);
        final Table relationTable = Table.get(ANATABLERELATION.TABLE);
        final Column tableDetailsTablePKColumn = tableDetailsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(mapperTable);

        selectQuery.addJoinClause(List.of(new JoinClause(mapperTable.getColumn(ANAMODULETABLEMAPPER.TABLE_ID), tableDetailsTablePKColumn),
                new JoinClause(tableDetailsTablePKColumn, relationTable.getColumn(ANATABLERELATION.PARENT_TABLE_ID))));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Map<Integer, Collection<TableDetail>> result = new HashMap<>();

        if (Objects.nonNull(dataContainer)) {
            final Map<Long, Collection<TableRelationMapping>> relationsMap = groupRelationsByParentTableId(dataContainer, relationTable);
            final Map<Long, TableDetail> tableDetailCache = new HashMap<>();

            dataContainer.getRows(mapperTable).forEach(mapperRow -> {
                final Integer moduleId = mapperRow.get(ANAMODULETABLEMAPPER.MODULE_ID);
                final Long tableId = mapperRow.get(ANAMODULETABLEMAPPER.TABLE_ID);

                if (Objects.nonNull(moduleId) && Objects.nonNull(tableId)) {
                    final TableDetail tableDetail = tableDetailCache.computeIfAbsent(tableId, id -> {
                        final Row tableRow = dataContainer.getRow(tableDetailsTable, id);

                        if (Objects.isNull(tableRow)) {
                            return null;
                        }

                        return buildTableDetail(tableRow, id, relationsMap.getOrDefault(id, List.of()));
                    });

                    if (Objects.nonNull(tableDetail)) {
                        result.computeIfAbsent(moduleId, k -> new ArrayList<>()).add(tableDetail);
                    }
                }
            });
        }

        LOGGER.info("CRM Analytics : Fetched table details grouped by module ID, [{}] modules", result.size());
        return result;
    }
}
