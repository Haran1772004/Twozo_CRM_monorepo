package com.tlc.crm.entity.search.api.manager.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.search.api.manager.SearchFieldManager;
import com.tlc.crm.entity.search.api.manager.SearchIndexManager;
import com.tlc.crm.entity.search.api.model.Entity;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.search.api.model.IndexField;
import com.tlc.crm.entity.search.api.model.PresetConfig;
import com.tlc.crm.entity.sql.resource.MCSENTITY;
import com.tlc.crm.entity.sql.resource.MCSFIELDS;
import com.tlc.crm.entity.sql.resource.MCSINDEX;
import com.tlc.crm.entity.sql.resource.MCSINDEXCOMPONENTS;
import com.tlc.crm.entity.sql.resource.MCSINDEXFIELDS;
import com.tlc.search.model.FieldProperty;

import com.tlc.search.model.IndexCustomAnalyzer;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the {@link IndexField} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchFieldManagerImpl implements SearchFieldManager {

    private static final String PARENT = "parent";
    private static final String MODEL = "model";
    private static final String IS_ARRAY_TYPE = "isArrayType";
    private static final String HAS_DEPENDENCY = "hasDependency";
    private static final String HAS_WRAPPER = "hasWrapper";
    private static final String IS_PRIMARY_ENTITY_ONLY_NEEDED = "isPrimaryEntityOnlyNeeded";
    private static final String CUSTOM_ANALYZER_ID = "customAnalyzerId";

    private final SearchIndexManager indexManager;

    private static class Instance {
        private static final SearchFieldManagerImpl INSTANCE = new SearchFieldManagerImpl();
    }

    private SearchFieldManagerImpl() {
        indexManager = SearchIndexManager.getInstance();
    }

    public static SearchFieldManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(MCSFIELDS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the entity.
     * </p>
     *
     * @return MCSEntity table.
     */
    private Table getEntityTable() {
        return Table.get(MCSENTITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the index fields.
     * </p>
     *
     * @return MCSIndexField table.
     */
    private Table getIndexFieldTable() {
        return Table.get(MCSINDEXFIELDS.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getAll() {
        return get(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getByIndex(final Long indexId) {
        return get(new WhereClause(Criteria.eq(indexManager.getTable().getPKColumn(), indexId)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getByIndexes(final Collection<Long> indexIds) {
        return get(new WhereClause(Criteria.in(indexManager.getTable().getPKColumn(), indexIds)));
    }

    /**
     * <p>
     * Fetches the collection of {@link IndexField} which satisfies the given criteria.
     * </p>
     *
     * @param whereClause {@link WhereClause} holds the retrieval criteria.
     * @return Collection of {@link IndexField}.
     */
    private Collection<IndexField> get(final WhereClause whereClause) {
        final Table indexTable = indexManager.getTable();
        final Table indexComponentsTable = indexManager.getIndexComponentsTable();
        final Table entityTable = getEntityTable();
        final String entityTableName = entityTable.getName();
        final Table parentEntityTable = Table.get(entityTableName, PARENT);
        final Table modelTable = Table.get(entityTableName, MODEL);
        final Table fieldTable = getTable();
        final Table indexFieldTable = getIndexFieldTable();
        final Column pkColumn = indexTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(indexFieldTable);

        selectQuery.addSelectClause(indexTable, indexComponentsTable, entityTable, parentEntityTable, fieldTable, indexFieldTable, modelTable);
        selectQuery.setWhereClause(whereClause);
        selectQuery.addJoinClause(List.of(new JoinClause(indexFieldTable.getColumn(MCSINDEXFIELDS.INDEX_ID), pkColumn),
                new JoinClause(indexFieldTable.getColumn(MCSINDEXFIELDS.PARENT_ENTITY_ID), parentEntityTable.getPKColumn()),
                new JoinClause(indexFieldTable.getColumn(MCSINDEXFIELDS.ENTITY_ID), entityTable.getPKColumn()),
                new JoinClause(indexFieldTable.getColumn(MCSINDEXFIELDS.FIELD_ID), fieldTable.getPKColumn()),
                new JoinClause(fieldTable.getColumn(MCSFIELDS.MODEL_ID), modelTable.getPKColumn(), JoinClause.JoinType.LEFT),
                new JoinClause(pkColumn, indexComponentsTable.getColumn(MCSINDEXCOMPONENTS.INDEX_ID), JoinClause.JoinType.LEFT)));
        final DataContainer dataContainer = DataStoreProvider.getReadableDataStore().get(selectQuery);
        final Map<Long, Index> indexes = dataContainer.getRows(indexTable).collect(Collectors.toMap(Row::getPKValue,
                row -> new Index(row.getPKValue(), row.get(MCSINDEX.DISPLAY_NAME), row.get(MCSINDEX.NAME),
                        indexManager.getAnalyzerComponent(dataContainer.getRow(indexComponentsTable)))));
        final Map<Long, Entity> entities = dataContainer.getRows(entityTable).collect(Collectors.toMap(Row::getPKValue,
                row -> new Entity(row.getPKValue(), row.get(MCSENTITY.NAME))));

        entities.putAll(dataContainer.getRows(parentEntityTable).collect(Collectors.toMap(Row::getPKValue,
                row -> new Entity(row.getPKValue(), row.get(MCSENTITY.NAME)))));
        entities.putAll(dataContainer.getRows(modelTable).collect(Collectors.toMap(Row::getPKValue,
                row -> new Entity(row.getPKValue(), row.get(MCSENTITY.NAME)))));

        return dataContainer.getRows(indexFieldTable).map(row -> convertRowToField(row, indexes, entities, dataContainer)).toList();
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link IndexField}.
     * </p>
     *
     * @param row      Holds the details of the {@link IndexField}.
     * @param indexes  {@link Index} of the field, It holds the details of the index.
     * @param entities {@link Entity} of the field, It holds the details of the entity.
     * @return Converted {@link IndexField}.
     */
    private IndexField convertRowToField(final Row row, final Map<Long, Index> indexes,
                                         final Map<Long, Entity> entities, final DataContainer dataContainer) {
        final Index index = indexes.get((Long) row.get(MCSINDEXFIELDS.INDEX_ID));
        final String config = row.get(MCSINDEXFIELDS.CONFIG);
        final JsonObject configJson = Objects.isNull(config) ? Json.object() : Json.object(config);
        final Row fieldRow = dataContainer.getRow(getTable(), (Long) row.get(MCSINDEXFIELDS.FIELD_ID));
        final Row indexFieldRow = dataContainer.getRow(getIndexFieldTable(), (Long) row.get(MCSINDEXFIELDS.ID));
        final FieldProperty.Type type = FieldProperty.Type.valueOf(indexFieldRow.get(MCSINDEXFIELDS.TYPE));
        final String analyzerName;

        if (FieldProperty.Type.TEXT == type) {
            analyzerName = configJson.containsKey(CUSTOM_ANALYZER_ID) ? IndexCustomAnalyzer.get(
                    configJson.getShort(CUSTOM_ANALYZER_ID)).getName() : index.analysisComponent().getDefaultAnalyzerName();
        } else {
            analyzerName = null;
        }
        final PresetConfig presetConfig = new PresetConfig(configJson.optBoolean(IS_ARRAY_TYPE),
                configJson.optBoolean(HAS_DEPENDENCY),
                configJson.optBoolean(HAS_WRAPPER),
                configJson.optBoolean(IS_PRIMARY_ENTITY_ONLY_NEEDED), analyzerName);

        return new IndexField(row.getPKValue(), fieldRow.get(MCSFIELDS.NAME),
                entities.get((Long) fieldRow.get(MCSFIELDS.MODEL_ID)), index,
                entities.get((Long) row.get(MCSINDEXFIELDS.PARENT_ENTITY_ID)),
                entities.get((Long) row.get(MCSINDEXFIELDS.ENTITY_ID)), configJson, type, presetConfig);
    }
}