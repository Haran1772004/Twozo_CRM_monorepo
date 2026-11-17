package com.tlc.analytics.config.manager.table;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.sql.resource.ANACOLUMNRELATION;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Column;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * The ColumnRelationManager class is responsible for managing the configuration and persistence of {@link ColumnRelation} entities.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see AbstractAnalyticsConfigManager
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColumnRelationManager extends AbstractAnalyticsConfigManager<ColumnRelation> {

    private static class Instance {
        private static final ColumnRelationManager INSTANCE = new ColumnRelationManager();
    }

    public static ColumnRelationManager INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ColumnRelation} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable() {
        return Table.get(ANACOLUMNRELATION.TABLE);
    }

    /**
     * <p>
     * Gets the {@link ColumnRelation} model class
     * </p>
     *
     * @return The {@link ColumnRelation}
     */
    @Override
    protected @NonNull Class<ColumnRelation> getModelClass() {
        return ColumnRelation.class;
    }

    /**
     * Fetches all {@link ColumnRelation} from the database.
     *
     * @return {@link Collection} of {@link ColumnRelation}, never null
     */
    public Collection<ColumnRelation> fetchAll() {
        final Table table = Table.get(ANACOLUMNRELATION.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<ColumnRelation> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildRelatedColumns(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] ColumnRelation from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link ColumnRelation} from the database for the given relation ID.
     *
     * @param relationId Represents the relation identifier
     * @return {@link Collection} of {@link ColumnRelation}, never null
     */
    public Collection<ColumnRelation> fetchByRelId(final Long relationId) {
        final Table table = Table.get(ANACOLUMNRELATION.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getColumn(ANACOLUMNRELATION.REL_ID), relationId));
        final Collection<ColumnRelation> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildRelatedColumns(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] ColumnRelation from DB for relationId [{}]", result.size(), relationId);
        return result;
    }

    /**
     * Fetches a single {@link ColumnRelation} from the database for the given ID.
     *
     * @param relatedColumnId Represents the entity identifier
     * @return {@link ColumnRelation} or null if not found
     */
    public ColumnRelation fetchByRelatedColumnId(final Long relatedColumnId) {
        final Table table = Table.get(ANACOLUMNRELATION.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getPKColumn(), relatedColumnId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                return buildRelatedColumns(row);
            }
        }

        LOGGER.info("CRM Analytics : No ColumnRelation found in DB for relatedColumnId [{}]", relatedColumnId);
        return null;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the related columns table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(ANACOLUMNRELATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds a {@link ColumnRelation} instance from the given row data.
     * </p>
     *
     * @param row The database row containing related column data
     * @return The constructed {@link ColumnRelation}
     */
    private ColumnRelation buildRelatedColumns(final Row row) {
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
