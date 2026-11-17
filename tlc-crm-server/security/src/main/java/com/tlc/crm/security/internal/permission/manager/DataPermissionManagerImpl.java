package com.tlc.crm.security.internal.permission.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.security.internal.permission.model.ColumnPermission;
import com.tlc.crm.security.internal.permission.model.PermissionMetaData;
import com.tlc.crm.security.internal.permission.model.TablePermission;
import com.tlc.crm.security.model.Rule;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.IAMPACTION;
import com.tlc.crm.security.sql.resource.IAMPCATEGORY;
import com.tlc.crm.security.sql.resource.IAMPSUBCATEGORY;
import com.tlc.crm.security.sql.resource.IAMPSUBCATEGORYACTION;
import com.tlc.crm.security.sql.resource.MIAMPCOLUMN;
import com.tlc.crm.security.sql.resource.MIAMPRELATEDTABLE;
import com.tlc.crm.security.sql.resource.MIAMPTABLE;
import com.tlc.crm.security.sql.resource.MIAMPTABLESUBCATEGORYACTION;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.resource.FGSEQUENCEPATTERN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages the table and column permissions to achieve the data level permission
 *
 * @author sundar
 * @version 1.0
 */
public class DataPermissionManagerImpl implements DataPermissionManager {

    private static final String CRITERIA_KEY = "criteria";
    private static final String CONDITIONS_KEY = "conditions";
    private static final String SCOPE_DEPENDENT_CRITERIA_KEY = "scopeDependentCriteria";
    private static final String CATEGORY_KEY = "category";
    private static final String SUBCATEGORY_KEY = "subCategory";
    private static final String ACTION_KEY = "action";
    private static final String APPLY_WHEN_KEY = "applyWhen";
    private static final String WRITE_RESTRICTION_RULES_KEY = "writeRestrictionRules";
    private static final String COLUMN_PERMISSION_RULE_KEY = "columnPermissionRule";
    private static final String DATA_PERMISSION_RULE_KEY = "dataPermissionRule";
    private static final String DEFAULT_KEY = "default";
    private static final String VALUE_KEY = "value";
    private static final String MET_SOURCE_TABLE = "METSource";
    private static final String RULES = "rules";
    private static final String RULE_TYPE = "ruleType";

    private final Map<String, TablePermission> tablePermissionMap;
    private final Map<String, Collection<String>> relatedTables;

    private DataPermissionManagerImpl() {
        this.tablePermissionMap = new HashMap<>();
        this.relatedTables = new HashMap<>();
    }

    private static class Instance {
        private static final DataPermissionManager DATA_PERMISSION_MANAGER = new DataPermissionManagerImpl();
    }

    /**
     * Get the instance of {@link DataPermissionManager}
     *
     * @return the instance of {@link DataPermissionManager}
     */
    public static DataPermissionManager getInstance() {
        return Instance.DATA_PERMISSION_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param column Represents the column associated with the {@link ColumnPermission}
     * @return The {@link ColumnPermission}
     */
    @Override
    public Optional<ColumnPermission> getColumnPermission(final Column column) {
        final Optional<TablePermission> optionalTablePermission = getTablePermission(column.getTable());

        if (optionalTablePermission.isPresent()) {
            final TablePermission tablePermission = optionalTablePermission.get();

            if (tablePermission.hasColumnPermissions()) {
                return Optional.ofNullable(tablePermission.getColumnPermission(column.getColumnName()));
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param table Represents the table associated with the {@link TablePermission}
     * @return The {@link TablePermission}
     */
    @Override
    public Optional<TablePermission> getTablePermission(final Table table) {
        return Optional.ofNullable(tablePermissionMap.get(table.getName()));
    }

    /**
     * {@inheritDoc}
     *
     * @param table Represents the table
     * @return {@link Collection} of related table names
     */
    @Override
    public Collection<String> getRelatedTables(final Table table) {
        final String tableName = table.getName();

        return relatedTables.containsKey(tableName) ? Collections.unmodifiableCollection(relatedTables.get(tableName))
                : Collections.emptyList();
    }

    /**
     * Loads the table and column permissions
     */
    @Override
    public final void loadDataPermissions() {
        final Table tablePermissionTable = Table.get(MIAMPTABLE.TABLE);
        final Table columnPermissionTable = Table.get(MIAMPCOLUMN.TABLE);
        final Table relatedTable = Table.get(MIAMPRELATEDTABLE.TABLE);
        final Table subCategoryActionPermissionTable = Table.get(MIAMPTABLESUBCATEGORYACTION.TABLE);

        final Column primaryTableColumn = relatedTable.getColumn(MIAMPRELATEDTABLE.TABLE_ID);
        final Column tablePermissionPkColumn = tablePermissionTable.getPKColumn();
        final Column columnPermissionTableIdColumn = columnPermissionTable.getColumn(MIAMPCOLUMN.TABLE_ID);
        final Column subCategoryActionTableColumn = subCategoryActionPermissionTable.getColumn(MIAMPTABLESUBCATEGORYACTION.TABLE_ID);

        final SelectQuery dataPermissionSelectQuery = SelectQuery.get(tablePermissionTable);

        dataPermissionSelectQuery.addJoinClause(List.of(new JoinClause(tablePermissionPkColumn, columnPermissionTableIdColumn, JoinClause.JoinType.LEFT),
                new JoinClause(tablePermissionPkColumn, subCategoryActionTableColumn, JoinClause.JoinType.LEFT),
                new JoinClause(tablePermissionPkColumn, primaryTableColumn, JoinClause.JoinType.LEFT)));
        final ReadableDataStore readableDataStore = DataStoreProvider.getReadableDataStore();
        final DataContainer dataPermissionContainer = readableDataStore.get(dataPermissionSelectQuery);

        if (!dataPermissionContainer.isEmpty()) {
            final Table categoryTable = Table.get(IAMPCATEGORY.TABLE);
            final Table subCategoryTable = Table.get(IAMPSUBCATEGORY.TABLE);
            final Table actionTable = Table.get(IAMPACTION.TABLE);
            final Table subCategoryActionTable = Table.get(IAMPSUBCATEGORYACTION.TABLE);
            final Table fgSequencePatternTable = Table.get(FGSEQUENCEPATTERN.TABLE);

            final Column categoryIdColumn = subCategoryTable.getColumn(IAMPSUBCATEGORY.CATEGORY_ID);
            final Column subCaetgoryIdColumn = subCategoryActionTable.getColumn(IAMPSUBCATEGORYACTION.SUB_CATEGORY_ID);
            final Column actionIdColumn = subCategoryActionTable.getColumn(IAMPSUBCATEGORYACTION.ACTION_ID);

            final SelectQuery selectQuery = SelectQuery.get(categoryTable);

            selectQuery.addSelectClause(List.of(categoryTable.getColumn(IAMPCATEGORY.NAME), subCategoryTable.getColumn(IAMPSUBCATEGORY.NAME),
                    categoryIdColumn, actionTable.getColumn(IAMPACTION.NAME), subCaetgoryIdColumn, actionIdColumn));
            selectQuery.addJoinClause(List.of(new JoinClause(categoryTable.getPKColumn(), categoryIdColumn),
                    new JoinClause(subCategoryTable.getPKColumn(), subCaetgoryIdColumn),
                    new JoinClause(actionIdColumn, actionTable.getPKColumn())));

            final DataContainer permissionMetaDataContainer = readableDataStore.get(selectQuery);

            final Map<Long, String> categoryIdNameMap = permissionMetaDataContainer.getRows(categoryTable)
                    .collect(Collectors.toMap(Row::getPKValue, row -> row.get(IAMPCATEGORY.NAME)));
            final Map<Long, Long> categoryMap = permissionMetaDataContainer.getRows(subCategoryTable)
                    .collect(Collectors.toMap(Row::getPKValue, row -> row.get(IAMPSUBCATEGORY.CATEGORY_ID)));
            final Map<Long, String> subCategoryIdNameMap = permissionMetaDataContainer.getRows(subCategoryTable)
                    .collect(Collectors.toMap(Row::getPKValue, row -> row.get(IAMPSUBCATEGORY.NAME)));
            final Map<Long, String> actionIdNameMap = permissionMetaDataContainer.getRows(actionTable)
                    .collect(Collectors.toMap(Row::getPKValue, row -> row.get(IAMPACTION.NAME)));
            final Map<String, Long> sequencePatternMap = getSequencePatterns(fgSequencePatternTable)
                    .getRows(fgSequencePatternTable).collect(Collectors.toMap(row -> row.get(FGSEQUENCEPATTERN.PATTERN),
                            row -> row.get(FGSEQUENCEPATTERN.COMMIT_VALUE)));

            final Map<String, Action.Type> actionTypeMap = new HashMap<>();
            final Map<String, Category.Type> categoryTypeMap = new HashMap<>();
            final Map<String, SubCategory.Type> subCategoryTypeMap = new HashMap<>();
            final Map<Long, String> tableIdNameMap = new HashMap<>();

            dataPermissionContainer.indexRows(subCategoryActionTableColumn);
            dataPermissionContainer.indexRows(columnPermissionTableIdColumn);
            dataPermissionContainer.indexRows(primaryTableColumn);

            for (final Row tableRow : dataPermissionContainer.getRows(tablePermissionTable).toList()) {
                final Long tableId = tableRow.getPKValue();
                final String tableName = tableRow.get(MIAMPTABLE.TABLE_NAME);
                final String config = tableRow.get(MIAMPTABLE.CONFIG);
                final boolean isSecure = tableRow.get(MIAMPTABLE.IS_SECURE);
                final Map<String, ColumnPermission> columnPermissionMap = new HashMap<>();
                final Map<Category.Type, Map<SubCategory.Type, Collection<Action.Type>>> subCategoryActionMap = new EnumMap<>(Category.Type.class);
                final JsonObject configJson = buildTablePermissionConfig(config, categoryIdNameMap, subCategoryIdNameMap, actionIdNameMap, sequencePatternMap);

                dataPermissionContainer.getIndexedRows(columnPermissionTableIdColumn, tableId).forEach(columnPermissionRow -> {
                    final String columnName = columnPermissionRow.get(MIAMPCOLUMN.NAME);
                    final String columnPermissionConfig = columnPermissionRow.get(MIAMPCOLUMN.CONFIG);
                    final boolean columnIsSecure = columnPermissionRow.get(MIAMPCOLUMN.IS_SECURE);
                    final JsonObject columnPermissionConfigJson = Objects.isNull(columnPermissionConfig) ? Json.object()
                            : buildColumnPermissionConfig(columnPermissionConfig, categoryIdNameMap, subCategoryIdNameMap, actionIdNameMap, sequencePatternMap);

                    columnPermissionMap.put(columnName, new ColumnPermission(columnName, columnPermissionConfigJson, columnIsSecure));
                });

                final Collection<Pair<Category.Type, Pair<SubCategory.Type, Action.Type>>> primarySubcategoryActions = new ArrayList<>();

                dataPermissionContainer.getRows(subCategoryActionPermissionTable, new WhereClause(Criteria.eq(subCategoryActionTableColumn, tableId)))
                        .forEach(row -> {
                            final Row subCategoryActionRow = permissionMetaDataContainer.getRow(subCategoryActionTable,
                                    (Long) row.get(MIAMPTABLESUBCATEGORYACTION.SUBCATEGORY_ACTION_ID));
                            final Long actionId = subCategoryActionRow.get(IAMPSUBCATEGORYACTION.ACTION_ID);
                            final Long subCategoryId = subCategoryActionRow.get(IAMPSUBCATEGORYACTION.SUB_CATEGORY_ID);
                            final Long categoryId = categoryMap.get(subCategoryId);

                            final Action.Type actionType = actionTypeMap.computeIfAbsent(actionIdNameMap.get(actionId), Action.Type::get);
                            final Category.Type categoryType = categoryTypeMap.computeIfAbsent(categoryIdNameMap.get(categoryId), Category.Type::get);
                            final SubCategory.Type subCategoryType = subCategoryTypeMap.computeIfAbsent(subCategoryIdNameMap.get(subCategoryId), SubCategory.Type::get);
                            final boolean isPrimary = row.get(MIAMPTABLESUBCATEGORYACTION.IS_PRIMARY);

                            subCategoryActionMap.computeIfAbsent(categoryType, tableCategoryType -> new EnumMap<>(SubCategory.Type.class))
                                    .computeIfAbsent(subCategoryType, tableSubcategory -> new HashSet<>()).add(actionType);

                            if (isPrimary) {
                                primarySubcategoryActions.add(Pair.of(categoryType, Pair.of(subCategoryType, actionType)));
                            }
                        });
                tablePermissionMap.put(tableName, new TablePermission(tableName, new PermissionMetaData(subCategoryActionMap, primarySubcategoryActions), configJson, isSecure, columnPermissionMap));
                tableIdNameMap.put(tableId, tableName);
            }

            for (final Map.Entry<Long, String> tableIdNameEntry : tableIdNameMap.entrySet()) {
                final String tableName = tableIdNameEntry.getValue();

                dataPermissionContainer.getIndexedRows(primaryTableColumn, tableIdNameEntry.getKey())
                        .forEach(relatedTableRow -> {
                            final Long relatedTableId = relatedTableRow.get(MIAMPRELATEDTABLE.RELATED_TABLE_ID);

                            relatedTables.computeIfAbsent(tableName, primaryTableName -> new HashSet<>())
                                    .add(tableIdNameMap.get(relatedTableId));
                        });
            }
        }
    }

    /**
     * Builds the table permissions configuration
     *
     * @param config             The configuration represented in String format
     * @param categoryMap        A map containing category names grouped by their respective IDs
     * @param subCategoryMap     A map containing sub-category names grouped by their respective IDs
     * @param actionMap          A map containing action names grouped by their respective IDs
     * @param sequencePatternMap A map containing sub-category and action IDs grouped by their respective patterns
     * @return The table permissions configuration as a {@link JsonObject}
     */
    private JsonObject buildTablePermissionConfig(final String config, final Map<Long, String> categoryMap, final Map<Long, String> subCategoryMap,
                                                  final Map<Long, String> actionMap, final Map<String, Long> sequencePatternMap) {
        if (Objects.isNull(config)) {
            return null;
        }
        final BiConsumer<String, JsonObject> sequencePatternResolver = (criteriaKey, configJson) -> {
            if (configJson.containsKey(criteriaKey)) {
                final JsonArray criteriaArray = configJson.getJsonArray(criteriaKey);
                final int criteriaArraySize = criteriaArray.size();

                for (int index = 0; index < criteriaArraySize; index++) {
                    final JsonObject criteriaJson = criteriaArray.getJsonObject(index);
                    final String value = criteriaJson.getString(VALUE_KEY);

                    if (sequencePatternMap.containsKey(value)) {
                        criteriaJson.put(VALUE_KEY, sequencePatternMap.get(value));
                    }

                    if (criteriaJson.containsKey(APPLY_WHEN_KEY)) {
                        resolveSequencePatternValue(criteriaJson.getJsonObject(APPLY_WHEN_KEY), categoryMap, subCategoryMap, actionMap, sequencePatternMap);
                    }
                }
            }
        };
        final JsonObject configJson = Json.object(config);

        if (configJson.containsKey(CRITERIA_KEY)) {
            sequencePatternResolver.accept(CONDITIONS_KEY, configJson.getJsonObject(CRITERIA_KEY));
        }
        sequencePatternResolver.accept(SCOPE_DEPENDENT_CRITERIA_KEY, configJson);
        return configJson;
    }

    /**
     * Resolves the patterns used for subcategory and action with their respective names
     *
     * @param configJson         Represents the configuration {@link JsonObject}
     * @param categoryMap     A map containing category names grouped by their respective IDs
     * @param subCategoryMap     A map containing sub-category names grouped by their respective IDs
     * @param actionMap          A map containing action names grouped by their respective IDs
     * @param sequencePatternMap A map containing sub-category and action IDs grouped by their respective patterns
     */
    private void resolvePermissionMetaDataValues(final JsonObject configJson, final Map<Long, String> categoryMap, final Map<Long, String> subCategoryMap,
                                                 final Map<Long, String> actionMap, final Map<String, Long> sequencePatternMap) {
        final String categoryPattern = configJson.getString(CATEGORY_KEY);
        final String subCategoryPattern = configJson.getString(SUBCATEGORY_KEY);
        final String actionPattern = configJson.getString(ACTION_KEY);

        configJson.put(CATEGORY_KEY, categoryMap.get(sequencePatternMap.get(categoryPattern)));
        configJson.put(SUBCATEGORY_KEY, subCategoryMap.get(sequencePatternMap.get(subCategoryPattern)));
        configJson.put(ACTION_KEY, actionMap.get(sequencePatternMap.get(actionPattern)));
    }

    /**
     * Builds the column permissions configuration
     *
     * @param config             The configuration represented in String format
     * @param categoryMap        A map containing category names grouped by their respective IDs
     * @param subCategoryMap     A map containing sub-category names grouped by their respective IDs
     * @param actionMap          A map containing action names grouped by their respective IDs
     * @param sequencePatternMap A map containing sub-category and action IDs grouped by their respective patterns
     * @return The column permissions configuration as a {@link JsonObject}
     */
    private JsonObject buildColumnPermissionConfig(final String config, final Map<Long, String> categoryMap, final Map<Long, String> subCategoryMap,
                                                   final Map<Long, String> actionMap, final Map<String, Long> sequencePatternMap) {
        final JsonObject configJson = Json.object(config);
        final Consumer<JsonObject> sequencePatternResolver = json -> {

            if (json.containsKey(APPLY_WHEN_KEY)) {
                final JsonObject applyWhenJson = json.optJsonObject(APPLY_WHEN_KEY);

                resolveSequencePatternValue(applyWhenJson, categoryMap, subCategoryMap, actionMap, sequencePatternMap);
            }
        };

        if (configJson.containsKey(WRITE_RESTRICTION_RULES_KEY)) {
            final JsonObject writeRestrictionRulesJson = configJson.getJsonObject(WRITE_RESTRICTION_RULES_KEY);

            if (writeRestrictionRulesJson.containsKey(COLUMN_PERMISSION_RULE_KEY)) {
                sequencePatternResolver.accept(writeRestrictionRulesJson.getJsonObject(COLUMN_PERMISSION_RULE_KEY));
            }

            if (writeRestrictionRulesJson.containsKey(DATA_PERMISSION_RULE_KEY)) {
                sequencePatternResolver.accept(writeRestrictionRulesJson.getJsonObject(DATA_PERMISSION_RULE_KEY));
            }
        }

        if (configJson.containsKey(DEFAULT_KEY)) {
            sequencePatternResolver.accept(configJson.getJsonObject(DEFAULT_KEY));
        }
        return configJson;
    }

    /**
     * Retrieves subcategory and action patterns from the database
     *
     * @param fgSequencePatternTable Represents the sequence pattern table
     * @return A {@link DataContainer} containing patterns for subcategories and actions, along with their respective commit values
     */
    private DataContainer getSequencePatterns(final Table fgSequencePatternTable) {
        final SelectQuery patternSelectQuery = SelectQuery.get(fgSequencePatternTable);
        final Column tableNameColumn = fgSequencePatternTable.getColumn(FGSEQUENCEPATTERN.TABLE_NAME);

        patternSelectQuery.addSelectClause(List.of(fgSequencePatternTable.getColumn(FGSEQUENCEPATTERN.PATTERN),
                fgSequencePatternTable.getColumn(FGSEQUENCEPATTERN.COMMIT_VALUE), tableNameColumn));
        patternSelectQuery.setWhereClause(new WhereClause(Criteria.in(tableNameColumn, List.of(IAMPCATEGORY.TABLE, IAMPSUBCATEGORY.TABLE,
                IAMPACTION.TABLE, MET_SOURCE_TABLE))));

        return DataStoreProvider.getReadableDataStore().get(patternSelectQuery);
    }

    /**
     * Resolves the patterns used for subcategory and action with their respective names
     *
     * @param jsonObject         the JSON object to load with the resolved values
     * @param categoryMap     A map containing category names grouped by their respective IDs
     * @param subCategoryMap     A map containing sub-category names grouped by their respective IDs
     * @param actionMap          A map containing action names grouped by their respective IDs
     * @param sequencePatternMap A map containing sub-category and action IDs grouped by their respective patterns
     */
    private void resolveSequencePatternValue(final JsonObject jsonObject, final Map<Long, String> categoryMap,
                                             final Map<Long, String> subCategoryMap, final Map<Long, String> actionMap,
                                             final Map<String, Long> sequencePatternMap) {
        final Rule.Type ruleType = jsonObject.containsKey(RULE_TYPE) ? Rule.Type.valueOf(jsonObject.getString(RULE_TYPE))
                : Rule.Type.PERMISSION;

        if (Rule.Type.PERMISSION == ruleType) {
            if (jsonObject.containsKey(RULES)) {
                final JsonArray rulesArray = jsonObject.getJsonArray(RULES);
                final int rulesArraySize = rulesArray.size();

                for (int index = 0; index < rulesArraySize; index++) {
                    resolveSequencePatternValue(rulesArray.getJsonObject(index), categoryMap, subCategoryMap, actionMap, sequencePatternMap);
                }
            } else {
                resolvePermissionMetaDataValues(jsonObject, categoryMap, subCategoryMap, actionMap, sequencePatternMap);
            }
        }
    }
}