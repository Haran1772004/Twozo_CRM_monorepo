package com.tlc.crm.entity.search.api.manager.impl;

import com.tlc.crm.entity.sql.resource.MCSINDEXCOMPONENTS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.fields.manager.SourceSettingsManager;
import com.tlc.crm.entity.search.api.manager.SearchIndexManager;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.sql.resource.MCSINDEX;
import com.tlc.crm.fields.sql.resource.OMETSOURCESETTINGS;

import com.tlc.search.model.AnalysisComponent;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides the {@link Index} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchIndexManagerImpl implements SearchIndexManager {

    private final SourceSettingsManager sourceSettingsManager;

    private static class Instance {
        private static final SearchIndexManagerImpl INSTANCE = new SearchIndexManagerImpl();
    }

    private SearchIndexManagerImpl() {
        sourceSettingsManager = SourceSettingsManager.getInstance();
    }

    public static SearchIndexManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(MCSINDEX.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    public Table getIndexComponentsTable() {
        return Table.get(MCSINDEXCOMPONENTS.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Index> getAll() {
        final Collection<Index> indices = new ArrayList<>();
        final Table indexTable = getTable();
        final Table sourceSettingsTable = sourceSettingsManager.getTable();
        final Table indexComponentsTable = getIndexComponentsTable();
        final SelectQuery selectQuery = SelectQuery.get(indexTable);
        final Column sourceIdColumn = sourceSettingsTable.getColumn(OMETSOURCESETTINGS.SOURCE_ID);

        selectQuery.addSelectClause(indexTable, indexComponentsTable);
        selectQuery.addSelectClause(List.of(sourceIdColumn, sourceSettingsTable.getColumn(OMETSOURCESETTINGS.ALTERNATE_NAME)));
        selectQuery.addJoinClause(List.of(new JoinClause(indexTable.getColumn(MCSINDEX.SOURCE_ID),
                sourceSettingsTable.getColumn(OMETSOURCESETTINGS.SOURCE_ID), JoinClause.JoinType.LEFT), new JoinClause(
                indexTable.getPKColumn(), indexComponentsTable.getColumn(MCSINDEXCOMPONENTS.INDEX_ID), JoinClause.JoinType.LEFT)));
        final DataContainer dataContainer = DataStoreProvider.getReadableDataStore().get(selectQuery);

        dataContainer.getRows(indexTable).forEach(row -> {
            final Row sourceSettingsRow = dataContainer.getRow(sourceSettingsTable, new WhereClause(Criteria
                    .eq(sourceIdColumn, row.get(MCSINDEX.SOURCE_ID))));

            indices.add(new Index(row.getPKValue(), row.get(MCSINDEX.DISPLAY_NAME), row.get(MCSINDEX.NAME),
                    Objects.isNull(sourceSettingsRow) ? null : sourceSettingsRow.get(OMETSOURCESETTINGS.ALTERNATE_NAME),
                    row.get(MCSINDEX.SOURCE_ID), getAnalyzerComponent(dataContainer.getRow(indexComponentsTable))));
        });
        return indices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Index get(final Long id) {
        return get(new WhereClause(Criteria.eq(getTable().getPKColumn(), id)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Index getByName(final String name) {
        return get(new WhereClause(Criteria.eq(getTable().getColumn(MCSINDEX.NAME), name)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisComponent getAnalyzerComponent(final Row componentRow) {
        final AnalysisComponent analysisComponent;

        if (Objects.isNull(componentRow)) {
            analysisComponent = null;
        } else {
            analysisComponent = new AnalysisComponent(componentRow.get(MCSINDEXCOMPONENTS.CONFIG));
        }

        return analysisComponent;
    }

    /**
     * <p>
     * Fetches the {@link Index} for the given criteria.
     * </p>
     *
     * @param whereClause {@link WhereClause} holds the retrieval criteria.
     * @return {@link Index} holds the details of index.
     */
    private Index get(final WhereClause whereClause) {
        final Table indexTable = getTable();
        final Table indexComponentsTable = getIndexComponentsTable();
        final SelectQuery selectQuery = SelectQuery.get(indexTable);

        selectQuery.addSelectClause(indexTable, indexComponentsTable);
        selectQuery.addJoinClause(new JoinClause(indexTable.getPKColumn(),
                indexComponentsTable.getColumn(MCSINDEXCOMPONENTS.INDEX_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = DataStoreProvider.getReadableDataStore().get(selectQuery);
        final Row row = dataContainer.getRow(indexTable);

        return Objects.isNull(row) ? null : new Index(row.getPKValue(), row.get(MCSINDEX.DISPLAY_NAME), row.get(MCSINDEX.NAME),
                row.get(MCSINDEX.SOURCE_ID), getAnalyzerComponent(dataContainer.getRow(indexComponentsTable)));
    }
}