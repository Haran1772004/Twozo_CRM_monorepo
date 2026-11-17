package com.tlc.crm.security.internal.permission.model;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.internal.permission.model.rule.converter.CriteriaRuleConverter;
import com.tlc.crm.security.internal.permission.model.rule.converter.PermissionRuleConverter;
import com.tlc.crm.security.model.Rule;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.Permission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.ScopeCriteria;
import com.tlc.crm.security.permission.model.ScopeParticipant;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.secure.JoinConfig;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Holds permission information for a table
 *
 * @author sundar
 * @version 1.0
 */
public class TablePermission implements Permission {

    private static final String IGNORABLE_OPERATIONS_KEY = "ignorableOperations";
    private static final String CRITERIA_KEY = "criteria";
    private static final String CONDITIONS_KEY = "conditions";
    private static final String OPERATIONS_KEY = "operations";
    private static final String SCOPE_DEPENDENT_CRITERIA_KEY = "scopeDependentCriteria";
    private static final String SCOPES = "scopes";
    private static final String SCOPE_CRITERIA = "scopeCriteria";
    private static final String PARTICIPANT = "participant";
    private static final String COLUMN = "column";
    private static final String TABLE = "table";
    private static final String JOIN_CONFIG = "joinConfig";
    private static final String SUPPORTING_CRITERIA = "supportingCriteria";
    private static final String FIELD_PERMISSION_CRITERIA = "fieldPermissionCriteria";
    private static final String REQUIRED_TABLES = "requiredTables";
    private static final String LEFT_COLUMN_DETAILS = "leftColumnDetails";
    private static final String RIGHT_COLUMN_DETAILS = "rightColumnDetails";
    private static final String OPERATOR = "operator";
    private static final String ENTITY_ID_COLUMN = "entityIdColumn";
    private static final String ALTERNATIVE_ENTITY_ID_COLUMN = "alternativeEntityIdColumn";

    private final boolean isSecure;
    private final String tableName;
    private final Map<Category.Type, Map<SubCategory.Type, Collection<Action.Type>>> permissionMetaData;
    private final Collection<Operation> ignorableOperations;
    private final Map<String, ColumnPermission> columnPermissionMap;
    private final Pair<Category.Type, Pair<SubCategory.Type, Action.Type>> primarySubcategoryAction;

    private Map<Operation, Collection<Filter>> filterMap;
    private Map<Scope, Collection<Filter>> scopeDependentFilters;
    private Collection<Filter> fieldPermissionFilters;
    private Map<String, JoinConfigs> tableJoinConfigs;
    private Map<String, Criteria> supportingCriteriaMap;
    private Column entityIdColumn;
    private Column alternativeEntityIdColumn;
    private Map<ScopeParticipant, ScopeCriteria> scopeCriteriaMap;

    public TablePermission(final String tableName, final PermissionMetaData permissionMetaData,
                           final JsonObject configJson, final boolean isSecure, final Map<String, ColumnPermission> columnPermissionMap) {
        this.tableName = tableName;
        this.permissionMetaData = Collections.unmodifiableMap(permissionMetaData.permissionDataMap());
        this.isSecure = isSecure;
        this.columnPermissionMap = Objects.isNull(columnPermissionMap) ? Collections.emptyMap() : Collections.unmodifiableMap(columnPermissionMap);
        this.ignorableOperations = new HashSet<>();
        this.primarySubcategoryAction = permissionMetaData.primaryPermissionActions().stream().findFirst().orElseGet(() -> null);

        if (Objects.nonNull(configJson)) {
            final JsonArray ignorableOperationsArray = configJson.optJsonArray(IGNORABLE_OPERATIONS_KEY);

            if (Objects.nonNull(ignorableOperationsArray)) {

                for (int index = 0; index < ignorableOperationsArray.size(); index++) {
                    ignorableOperations.add(Operation.valueOf(ignorableOperationsArray.getString(index)));
                }
            }

            if (configJson.containsKey(CRITERIA_KEY)) {
                final JsonObject criteriaJson = configJson.getJsonObject(CRITERIA_KEY);

                if (criteriaJson.containsKey(JOIN_CONFIG)) {
                    this.tableJoinConfigs = buildJoinConfigs(criteriaJson.getJsonObject(JOIN_CONFIG));
                }

                if (criteriaJson.containsKey(SUPPORTING_CRITERIA)) {
                    this.supportingCriteriaMap = buildSupportingCriteria(criteriaJson.getJsonArray(SUPPORTING_CRITERIA));
                }

                if (criteriaJson.containsKey(ENTITY_ID_COLUMN)) {
                    final JsonObject entityIdJson = criteriaJson.getJsonObject(ENTITY_ID_COLUMN);

                    this.entityIdColumn = Table.get(entityIdJson.getString(TABLE)).getColumn(entityIdJson.getString(COLUMN));
                }

                if (criteriaJson.containsKey(ALTERNATIVE_ENTITY_ID_COLUMN)) {
                    final JsonObject alternativeEntityIdColumn = criteriaJson.getJsonObject(ALTERNATIVE_ENTITY_ID_COLUMN);

                    this.alternativeEntityIdColumn = Table.get(alternativeEntityIdColumn.getString(TABLE)).getColumn(alternativeEntityIdColumn.getString(COLUMN));
                }


                if (criteriaJson.containsKey(CONDITIONS_KEY)) {
                    this.filterMap = new LinkedHashMap<>();
                    final Collection<Filter> filters = Filter.buildFilters(criteriaJson.getJsonArray(CONDITIONS_KEY));

                    if (criteriaJson.containsKey(OPERATIONS_KEY)) {
                        final JsonArray operations = criteriaJson.getJsonArray(OPERATIONS_KEY);
                        final int size = operations.size();

                        for (int index = 0; index < size; index++) {
                            filterMap.put(Operation.valueOf(operations.getString(index)), filters);
                        }
                    } else {
                        for (final Operation operation : Operation.values()) {
                            filterMap.put(operation, filters);
                        }
                    }
                }
            }

            if (configJson.containsKey(SCOPE_DEPENDENT_CRITERIA_KEY)) {
                this.scopeDependentFilters = new LinkedHashMap<>();
                final JsonArray criteriaArray = configJson.getJsonArray(SCOPE_DEPENDENT_CRITERIA_KEY);
                final int criteriaSize = criteriaArray.size();

                for (int index = 0; index < criteriaSize; index++) {
                    final JsonObject criteriaJson = criteriaArray.getJsonObject(index);
                    final Filter filter = new Filter(criteriaJson);
                    final JsonArray scopeArray = criteriaJson.getJsonArray(SCOPES);

                    scopeArray.forEach(scope -> scopeDependentFilters.computeIfAbsent(Scope.get((String) scope),
                            permissionScope -> new LinkedHashSet<>()).add(filter));
                }
            }

            if (configJson.containsKey(SCOPE_CRITERIA)) {
                buildScopeCriteria(configJson.getJsonArray(SCOPE_CRITERIA));
            }

            if (configJson.containsKey(FIELD_PERMISSION_CRITERIA)) {
                this.fieldPermissionFilters = new ArrayList<>();
                final JsonArray fieldPermissionCriteriaArray = configJson.getJsonArray(FIELD_PERMISSION_CRITERIA);
                final int size = fieldPermissionCriteriaArray.size();

                for (int index = 0; index < size; index++) {
                    final JsonObject criteriaJson = fieldPermissionCriteriaArray.getJsonObject(index);

                    fieldPermissionFilters.add(new Filter(criteriaJson));
                }
            }
        }
    }

    /**
     * <p>
     * Builds the supporting criteria map from the given {@link JsonArray}.
     * </p>
     *
     * @param jsonArray Holds the details of the supporting criteria.
     * @return Map of table name and its corresponding {@link Criteria}.
     */
    private Map<String, Criteria> buildSupportingCriteria(final JsonArray jsonArray) {
        final Map<String, Criteria> supportingCriteria = new HashMap<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject supportingCriteriaJson = jsonArray.getJsonObject(index);
            final JsonArray requiredTablesArray = supportingCriteriaJson.getJsonArray(REQUIRED_TABLES);
            final JsonObject criteriaJson = supportingCriteriaJson.getJsonObject(CRITERIA_KEY);
            final Criteria criteria = buildSupportingCriteria(criteriaJson);

            requiredTablesArray.forEach(table -> supportingCriteria.put((String) table, criteria));
        }

        return supportingCriteria;
    }

    /**
     * <p>
     * Builds the {@link Criteria} from the given {@link JsonObject}.
     * </p>
     *
     * @param criteriaJson Holds the details of the {@link Criteria}.
     * @return {@link Criteria}.
     */
    private Criteria buildSupportingCriteria(final JsonObject criteriaJson) {
        final Operator operator = Operator.getOperator(criteriaJson.getString(OPERATOR));
        final JsonObject leftColumnDetailsJson = criteriaJson.getJsonObject(LEFT_COLUMN_DETAILS);
        final JsonObject rightColumnDetailsJson = criteriaJson.getJsonObject(RIGHT_COLUMN_DETAILS);

        return new Criteria(
                Table.get(leftColumnDetailsJson.getString(TABLE)).getColumn(leftColumnDetailsJson.getString(COLUMN)),
                Table.get(rightColumnDetailsJson.getString(TABLE)).getColumn(rightColumnDetailsJson.getString(COLUMN)),
                operator
        );
    }

    /**
     * <p>
     * Builds the join configs from the given {@link JsonObject}.
     * </p>
     *
     * @param joinConfigJson Holds the details of the {@link JoinConfig}.
     * @return Table {@link JoinConfig}.
     */
    private Map<String, JoinConfigs> buildJoinConfigs(final JsonObject joinConfigJson) {
        final Map<String, JoinConfigs> tableJoinConfigs = new HashMap<>();

        for (final String table : joinConfigJson.keySet()) {
            tableJoinConfigs.put(table, new JoinConfigs(joinConfigJson.getJsonArray(table)));
        }
        return tableJoinConfigs;
    }

    /**
     * Gets the table name
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Gets the subcategory action associated with this permission
     *
     * @return {@link SubCategory.Type}
     */
    public Map<Category.Type, Map<SubCategory.Type, Collection<Action.Type>>> getSubCategoryActions() {
        return permissionMetaData;
    }

    /**
     * Checks whether the given {@link SubCategory.Type} is associated with this table permission or not
     *
     * @param subCategoryType Represents the {@link SubCategory.Type}
     * @return true if the given {@link SubCategory.Type} is associated with this table permission; false otherwise
     */
    public boolean isAssociatedWith(final SubCategory.Type subCategoryType) {
        for (final Map<SubCategory.Type, Collection<Action.Type>> subCategoryActions : permissionMetaData.values()) {
            final boolean isSubCategoryContains = subCategoryActions.containsKey(subCategoryType);

            if (isSubCategoryContains) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given operation is ignorable or not
     *
     * @param operation operation to be checked
     * @return true if the operation is ignorable; otherwise false
     */
    public boolean isIgnorable(final Operation operation) {
        return ignorableOperations.contains(operation);
    }

    /**
     * Indicates whether the table is secure
     *
     * @return true if the column is secure; otherwise false
     */
    public boolean isSecure() {
        return isSecure;
    }

    /**
     * Checks whether the table has column-level permissions or not
     *
     * @return true if the table has column-level permissions or not
     */
    public boolean hasColumnPermissions() {
        return Objects.nonNull(columnPermissionMap) && !columnPermissionMap.isEmpty();
    }

    /**
     * Gets the {@link ColumnPermission} corresponding to the {@link Column}
     *
     * @param columnName Represents the column
     * @return the {@link ColumnPermission}
     */
    public ColumnPermission getColumnPermission(final String columnName) {
        return Objects.isNull(columnPermissionMap) ? null : columnPermissionMap.get(columnName);
    }

    /**
     * Gets the column permissions associated with this permission
     *
     * @return A {@link Map} containing column names as keys and their respective permissions as values
     */
    public Collection<ColumnPermission> getColumnPermissions() {
        return columnPermissionMap.values();
    }

    /**
     * Gets the {@link Collection} of {@link Filter} associated with the table
     *
     * @return the {@link Collection} of {@link Filter} associated with the table
     */
    public Collection<Filter> getFilters(final Operation operation) {
        return filterMap.getOrDefault(operation, Collections.emptySet());
    }

    /**
     * Gets the {@link Collection} of {@link Filter} associated with the table and the scope.
     *
     * @return the {@link Collection} of {@link Filter} associated with the table and the scope.
     */
    public Collection<Filter> getScopeDependentFilters(final Scope scope) {
        return scopeDependentFilters.getOrDefault(scope, Collections.emptyList());
    }

    /**
     * Determines if the table has any filters associated with
     *
     * @return True if the table has any filters; otherwise, false
     */
    public boolean hasFilters() {
        return Objects.nonNull(filterMap) && !filterMap.isEmpty();
    }

    /**
     * Determines if the table has any scope-dependent filters associated with
     *
     * @return True if the table has any scope-dependent filters; otherwise, false
     */
    public boolean hasScopeDependentFilters() {
        return Objects.nonNull(scopeDependentFilters) && !scopeDependentFilters.isEmpty();
    }

    /**
     * Determines if there are active field permission filters.
     *
     * @return {@code true} if filters are present, {@code false} if null or empty.
     */
    public boolean hasFieldPermissionFilters() {
        return !(Objects.isNull(fieldPermissionFilters) || fieldPermissionFilters.isEmpty());
    }

    /**
     * Gets the list of field permission filters.
     *
     * @return the collection of filters, or an empty list if none exist (never null).
     */
    public Collection<Filter> getFieldPermissionFilters() {
        return Objects.isNull(fieldPermissionFilters) ? Collections.emptyList() : fieldPermissionFilters;
    }

    /**
     * <p>
     * Gets the primary subcategory action
     * </p>
     *
     * @return The pair containing key as category and value as subcategory and its action type
     */
    public Pair<Category.Type, Pair<SubCategory.Type, Action.Type>> getPrimarySubCategoryAction() {
        return primarySubcategoryAction;
    }

    /**
     * Determines if the table has any scope criteria's associated with
     *
     * @return True if the table has any scope criteria's; otherwise, false
     */
    public boolean hasScopeCriteria() {
        return Objects.nonNull(scopeCriteriaMap) && !scopeCriteriaMap.isEmpty();
    }

    /**
     * <p>
     * Gets the scope criteria map
     * </p>
     *
     * @return The map containing key as {@link ScopeParticipant} and value as {@link ScopeCriteria}
     */
    public Map<ScopeParticipant, ScopeCriteria> getScopeCriteriaMap() {
        return scopeCriteriaMap;
    }

    /**
     * <p>
     * Builds the {@link ScopeCriteria}
     * </p>
     *
     * @param scopeCriteriaArray The {@link JsonArray} containing scope criteria data
     */
    private void buildScopeCriteria(final JsonArray scopeCriteriaArray) {
        this.scopeCriteriaMap = new EnumMap<>(ScopeParticipant.class);
        final int scopeParticipantsSize = scopeCriteriaArray.size();

        for (int index = 0; index < scopeParticipantsSize; index++) {
            final JsonObject scopeCriteriaJson = scopeCriteriaArray.getJsonObject(index);
            final ScopeCriteria scopeCriteria = new ScopeCriteria();

            if (scopeCriteriaJson.containsKey(PARTICIPANT)) {
                scopeCriteria.setScopeParticipant(ScopeParticipant.valueOf(scopeCriteriaJson.getString(PARTICIPANT)));
            }

            if (scopeCriteriaJson.containsKey(TABLE) && scopeCriteriaJson.containsKey(COLUMN)) {
                final Table table = Table.get(scopeCriteriaJson.getString(TABLE));
                final String columnName = scopeCriteriaJson.getString(COLUMN);

                scopeCriteria.setCriteriaColumn(table.getColumn(columnName));
            }
            final boolean hasJoins = scopeCriteriaJson.containsKey(JOIN_CONFIG);

            scopeCriteria.setJoinsRequired(hasJoins);

            if (hasJoins) {
                scopeCriteria.setJoinConfigs(new JoinConfigs(scopeCriteriaJson.getJsonArray(JOIN_CONFIG)));
            }
            scopeCriteriaMap.put(scopeCriteria.getScopeParticipant(), scopeCriteria);
        }
    }

    /**
     * <p>
     * Provides the {@link JoinConfigs} based on the tables.
     * </p>
     *
     * @return {@link JoinConfigs} by table.
     */
    public Map<String, JoinConfigs> getTableJoinConfigs() {
        return tableJoinConfigs;
    }

    /**
     * <p>
     * Provides the supporting criteria based on the tables.
     * </p>
     *
     * @return {@link Criteria} by table.
     */
    public Map<String, Criteria> getSupportingCriteria() {
        return supportingCriteriaMap;
    }

    /**
     * <p>
     * Provides the entity id column for the table.
     * </p>
     *
     * @return {@link Column} representing the entity id column.
     */
    public Column getEntityIdColumn() {
        return this.entityIdColumn;
    }

    /**
     * <p>
     * Provides the alternative entity id column for the table.
     * </p>
     *
     * @return {@link Column} representing the alternative entity id column.
     */
    public Column getAlternativeEntityIdColumn() {
        return this.alternativeEntityIdColumn;
    }

    /**
     * Represents the Filter associated with the table
     *
     * @author sundar
     * @version 1.0
     */
    public static class Filter {

        private static final String VALUE_KEY = "value";
        private static final String OPERATOR_KEY = "operator";
        private static final String CONNECTOR_KEY = "connector";
        private static final String TABLE_KEY = "table";
        private static final String COLUMN_KEY = "column";
        private static final String APPLY_WHEN_KEY = "applyWhen";
        private static final String RULE_TYPE = "ruleType";

        private final String tableName;
        private final String columnName;
        private final Operator operator;
        private final SecureDefaultValue value;
        private final Connector connector;
        private final Rule<?, SecurityContext> rule;
        private final Collection<Filter> nestedFilters;

        private Filter(final JsonObject criteriaJson) {
            final JsonObject valueJson = criteriaJson.optJsonObject(VALUE_KEY);

            this.tableName = criteriaJson.optString(TABLE_KEY, null);
            this.columnName = criteriaJson.getString(COLUMN_KEY);
            this.operator = Operator.getOperator(criteriaJson.getString(OPERATOR_KEY));
            this.value = new SecureDefaultValue(Objects.isNull(valueJson) ? criteriaJson : valueJson);
            this.rule = criteriaJson.containsKey(APPLY_WHEN_KEY) ? buildRule(criteriaJson.getJsonObject(APPLY_WHEN_KEY)) : null;
            this.connector = criteriaJson.containsKey(CONNECTOR_KEY) ? Connector.valueOf(criteriaJson.getString(CONNECTOR_KEY)) : null;
            this.nestedFilters = criteriaJson.containsKey(CRITERIA_KEY) ? buildFilters(criteriaJson.getJsonArray(CRITERIA_KEY)) : Collections.emptyList();
        }

        /**
         * Gets the {@link Rule}
         *
         * @return the {@link Rule}
         */
        public Rule<?, SecurityContext> getRule() {
            return rule;
        }

        /**
         * Gets the {@link Connector}
         *
         * @return the {@link Connector}
         */
        public Connector getConnector() {
            return connector;
        }

        /**
         * Gets the {@link Collection} of {@link Filter}
         *
         * @return the {@link Collection} of {@link Filter}
         */
        public Collection<Filter> getNestedFilters() {
            return Collections.unmodifiableCollection(nestedFilters);
        }

        /**
         * Determines if the {@link Filter} has nested filters
         *
         * @return True if the table has criteria; otherwise, false
         */
        public boolean hasNestedFilter() {
            return !nestedFilters.isEmpty();
        }

        /**
         * Checks whether the {@link Filter} has connector or not
         *
         * @return True if the table has connector; otherwise, false
         */
        public boolean hasConnector() {
            return Objects.nonNull(connector);
        }

        /**
         * Builds the {@link Criteria}
         *
         * @param table the table to fetch the column
         * @return the {@link Criteria}
         */
        public Criteria buildFilter(final Table table) {
            final Object value = this.value.getValue().orElseGet(() -> null);

            if (operator.isMultiValueOperator() && Objects.isNull(value)) {
                return null;
            }
            final Table dataTable = Objects.isNull(tableName) ? table : Table.get(tableName);

            return new Criteria(dataTable.getColumn(columnName), value, operator);
        }

        /**
         * Builds the {@link Collection} of {@link Filter} using the given criteria array.
         *
         * @param criteriaArray Represents the criteria.
         * @return the {@link Collection} of filters
         */
        private static Collection<Filter> buildFilters(final JsonArray criteriaArray) {
            final Collection<Filter> criteria = new LinkedHashSet<>();
            final int criteriaSize = criteriaArray.size();

            for (int index = 0; index < criteriaSize; index++) {
                criteria.add(new Filter(criteriaArray.getJsonObject(index)));
            }
            return Collections.unmodifiableCollection(criteria);
        }

        /**
         * <p>
         * Builds the {@link Rule} to apply the filters of the table.
         * </p>
         *
         * @param applyWhenJson Holds the rule details.
         * @return {@link Rule}.
         */
        private Rule<?, SecurityContext> buildRule(final JsonObject applyWhenJson) {
            final Rule.Type ruleType = applyWhenJson.containsKey(RULE_TYPE) ? Rule.Type.valueOf(applyWhenJson.getString(RULE_TYPE))
                    : Rule.Type.PERMISSION;

            return switch (ruleType) {
                case CRITERIA -> CriteriaRuleConverter.getInstance().convert(applyWhenJson);
                case PERMISSION -> PermissionRuleConverter.getInstance().convert(applyWhenJson);
            };
        }
    }
}