package com.tlc.crm.security.internal.permission.handler;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.internal.permission.manager.ReadableDataPermissionManager;
import com.tlc.crm.security.internal.permission.model.ColumnPermission;
import com.tlc.crm.security.internal.permission.model.ScopeColumnInfo;
import com.tlc.crm.security.internal.permission.model.SecureColumnValueResolver;
import com.tlc.crm.security.internal.permission.model.RestrictionConstraint;
import com.tlc.crm.security.internal.permission.model.SecureColumn;
import com.tlc.crm.security.internal.permission.model.TablePermission;
import com.tlc.crm.security.internal.permission.util.SecurityWhereClauseUtil;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.ScopeParticipant;
import com.tlc.crm.security.permission.model.ScopeCriteria;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.meta.FKDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link DataPermissionHandler} providing services to verify table and column permissions
 *
 * @author sundar
 * @version 1.0
 * @see DataPermissionHandler
 */
public class DataPermissionHandlerImpl implements DataPermissionHandler {

    private static final String COLUMN_WITH_ALIAS_FORMAT = "%s_%s";
    private static final byte LOCK_STRIPES_COUNT = 10;

    private final long orgId;
    private final long roleId;

    private final ReadableDataPermissionManager dataPermissionManager;
    private final RolePermissionService rolePermissionService;

    private final Map<SubCategory.Type, Map<Action.Type, RolePermission>> permissions;
    private final Map<Table, Map<Operation, Boolean>> tableActionMap;
    private final Map<Column, Map<Operation, Boolean>> columnActionMap;
    private final Map<Column, Table> columnRemoteTableMap;
    private final Map<Table, Map<ScopeParticipant, ScopeColumnInfo>> scopeColumnInfoMap;
    private final Map<Table, Map<ScopeParticipant, WhereClause>> scopeColumnCriteriaMap;
    private final Map<Table, Scope> scopeMap;
    private final Map<Table, Map<Operation, WhereClause>> tableWhereClauseMap;
    private final Map<Table, Map<Scope, WhereClause>> tableScopeWhereClauseMap;
    private final Map<Column, Object> defaultValueMap;
    private final Map<Column, Map<Operation, Map<Object, Boolean>>> columnDataActionMap;
    private final Map<Table, Map<Operation, Collection<Column>>> ignorableColumns;
    private final Map<Category.Type, Map<SubCategory.Type, Map<Action.Type, RolePermission>>> currentRolePermissions;
    private final Map<Table, WhereClause> tableFieldPermissionWhereClauseMap;
    private final Map<Table, Boolean> fieldPermissionWhereClauseInProgressMap;
    private final LockStore<Table> fieldPermissionWhereClauseLockStore;

    private boolean authorizationRequired;
    private RolePermission permission;
    private boolean hasPermission;

    public DataPermissionHandlerImpl(final SecurityContext securityContext) {
        final BaseCrmUser user = securityContext.getUser();

        this.orgId = user.orgId();
        this.roleId = user.getRoleId();
        this.rolePermissionService = RolePermissionService.getInstance();
        this.dataPermissionManager = ReadableDataPermissionManager.get();
        this.columnRemoteTableMap = new ConcurrentHashMap<>();
        this.permission = securityContext.getRolePermission();
        this.hasPermission = Objects.nonNull(permission);
        this.permissions = new EnumMap<>(SubCategory.Type.class);
        this.tableActionMap = new ConcurrentHashMap<>();
        this.columnActionMap = new ConcurrentHashMap<>();
        this.scopeColumnInfoMap = new ConcurrentHashMap<>();
        this.scopeColumnCriteriaMap = new ConcurrentHashMap<>();
        this.scopeMap = new ConcurrentHashMap<>();
        this.defaultValueMap = new ConcurrentHashMap<>();
        this.tableWhereClauseMap = new ConcurrentHashMap<>();
        this.tableScopeWhereClauseMap = new ConcurrentHashMap<>();
        this.columnDataActionMap = new ConcurrentHashMap<>();
        this.ignorableColumns = new ConcurrentHashMap<>();
        this.tableFieldPermissionWhereClauseMap = new ConcurrentHashMap<>();
        this.fieldPermissionWhereClauseInProgressMap = new ConcurrentHashMap<>();
        this.fieldPermissionWhereClauseLockStore = new LockStore<>(LOCK_STRIPES_COUNT);
        this.currentRolePermissions = rolePermissionService.getRolePermissions(orgId, roleId).getPermissions().stream().collect(
                Collectors.groupingBy(permission -> permission.getCategory().getType(),
                        Collectors.groupingBy(permission -> permission.getSubCategory().getType(),
                                Collectors.toMap(permission -> permission.getAction().getType(), Function.identity()))));

        setAuthorizationRequired();
    }

    /**
     * {@inheritDoc}
     *
     * @param table     Represents the table to check for the actions
     * @param operation Represents the operation
     * @return true if the table has the required actions; otherwise, false
     */
    @Override
    public boolean hasAccess(final Table table, final Operation operation) {

        return tableActionMap.computeIfAbsent(table, actionCheckTable -> new EnumMap<>(Operation.class))
                .computeIfAbsent(operation, tableOperation -> {
                    final Optional<TablePermission> optionalTablePermission = dataPermissionManager.getTablePermission(table);

                    if (optionalTablePermission.isEmpty()) {
                        return true;
                    }
                    final TablePermission tablePermission = optionalTablePermission.get();

                    if (tablePermission.isSecure()) {

                        if (!authorizationRequired || tablePermission.isIgnorable(operation)
                                || (hasPermission && (Action.Type.EDIT == permission.getAction().getType()
                                && (Operation.SELECT == operation || Operation.INSERT == operation)))) {
                            return true;
                        }

                        for (final Map.Entry<Category.Type, Map<SubCategory.Type, Collection<Action.Type>>> categoryActionsEntry
                                : tablePermission.getSubCategoryActions().entrySet()) {
                            final Category.Type tableCategoryType = categoryActionsEntry.getKey();

                            for (final Map.Entry<SubCategory.Type, Collection<Action.Type>> subCategoryActionsEntry
                                    : categoryActionsEntry.getValue().entrySet()) {
                                final SubCategory.Type tableSubCategoryType = subCategoryActionsEntry.getKey();
                                final Map<Action.Type, RolePermission> actionPermissionMap = rolePermissionService.getPermissionsByActionTypes(orgId,
                                        roleId, tableCategoryType, tableSubCategoryType, subCategoryActionsEntry.getValue());
                                final Action.Type actionType = fetchActionType(tableSubCategoryType, operation);

                                if (Objects.nonNull(actionType) && actionPermissionMap.containsKey(actionType) && !actionPermissionMap.get(actionType).isGranted()) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                });
    }

    /**
     * {@inheritDoc}
     *
     * @param column    Represents the column to check for the actions
     * @param operation Represents the operation
     * @return true if the table has the required actions; otherwise, false
     */
    @Override
    public boolean hasAccess(final Column column, final Operation operation) {

        return columnActionMap.computeIfAbsent(column, actionCheckColumn -> new EnumMap<>(Operation.class)).computeIfAbsent(operation, columnOperation ->
                switch (operation) {
                    case INSERT, UPDATE -> {
                        final Optional<ColumnPermission> optionalColumnPermission = dataPermissionManager.getColumnPermission(column);

                        if (optionalColumnPermission.isEmpty()) {
                            yield true;
                        }
                        final ColumnPermission columnPermission = optionalColumnPermission.get();

                        if (columnPermission.isSecure()) {
                            yield columnPermission.canWrite();
                        }
                        yield true;
                    }
                    case SELECT -> {
                        final Table remoteTable = columnRemoteTableMap.computeIfAbsent(column, fkColumn -> {
                            final Optional<FKDefinition> optionalFKDefinition = fkColumn.getTable().getTableDefinition()
                                    .getFKDefinitions().values().stream()
                                    .filter(fkDefinition -> Objects.equals(fkColumn.getColumnName(), fkDefinition.getLocal().getColumnName()))
                                    .findFirst();

                            return optionalFKDefinition.map(fkDefinition -> Table.get(fkDefinition.getReferenceTable())).orElseGet(() -> null);
                        });

                        yield Objects.isNull(remoteTable) || hasAccess(remoteTable, operation);
                    }
                    default -> true;
                });
    }

    /**
     * {@inheritDoc}
     *
     * @param column    the column to check for access
     * @param data      the data to check
     * @param operation the operation being performed
     * @return true if the column has the required access; otherwise, false
     */
    @Override
    public boolean hasAccess(final Column column, final Object data, final Operation operation) {

        return columnDataActionMap.computeIfAbsent(column, columnToCheck -> new EnumMap<>(Operation.class))
                .computeIfAbsent(operation, columnOperation -> new HashMap<>())
                .computeIfAbsent(data, dataToValidate -> {
                    final Optional<ColumnPermission> optionalColumnPermission = dataPermissionManager.getColumnPermission(column);

                    if (optionalColumnPermission.isEmpty()) {
                        return true;
                    }

                    if (hasAccess(column, operation) && authorizationRequired) {
                        return optionalColumnPermission.map(columnPermission -> columnPermission.canWrite(dataToValidate)).orElse(true);
                    }
                    return false;
                });
    }

    /**
     * {@inheritDoc}
     *
     * @param table            Represents the table containing the column
     * @param scopeParticipant Represents the {@link ScopeParticipant}
     * @return The {@link ScopeColumnInfo} containing the column and its required joins, or empty if not found
     */
    @Override
    public Optional<ScopeColumnInfo> fetchScopeColumn(final Table table, final ScopeParticipant scopeParticipant) {

        return Optional.ofNullable(scopeColumnInfoMap.computeIfAbsent(table, scopeColumnCheckTable -> new EnumMap<>(ScopeParticipant.class))
                .computeIfAbsent(scopeParticipant, scopeColumnCheckParticipant -> {
                    final Optional<TablePermission> optionalTablePermission = dataPermissionManager.getTablePermission(table);

                    if (optionalTablePermission.isEmpty()) {
                        return null;
                    }
                    final TablePermission tablePermission = optionalTablePermission.get();

                    if (tablePermission.hasScopeCriteria()) {
                        final Map<ScopeParticipant, ScopeCriteria> scopeCriteriaMap = tablePermission.getScopeCriteriaMap();

                        if (scopeCriteriaMap.containsKey(scopeParticipant)) {
                            final ScopeCriteria scopeCriteria = scopeCriteriaMap.get(scopeParticipant);
                            final Column criteriaColumn = scopeCriteria.getCriteriaColumn();

                            if (scopeCriteria.isJoinsRequired()) {
                                return ScopeColumnInfo.of(criteriaColumn, scopeCriteria.getJoinConfigs().getJoins(table));
                            } else if (Objects.nonNull(criteriaColumn)) {
                                return ScopeColumnInfo.of(table.getColumn(criteriaColumn.getColumnName()));
                            }
                        }
                    }
                    return null;
                }));
    }

    /**
     * {@inheritDoc}
     *
     * @param table               Represents the table containing the column
     * @param scopeParticipant    Represents the {@link ScopeParticipant}
     * @param columnAliasMap      Contains the column aliases
     * @param scopeCriteriaValues Contains the scope criteria values
     * @return The scope criteria {@link WhereClause}
     */
    @Override
    public Optional<WhereClause> fetchScopeCriteria(final Table table, final ScopeParticipant scopeParticipant,
                                                    final Map<String, Collection<String>> columnAliasMap, final Collection<Long> scopeCriteriaValues) {

        return Optional.ofNullable(scopeColumnCriteriaMap.computeIfAbsent(table, scopeParticipantMap -> new EnumMap<>(ScopeParticipant.class))
                .computeIfAbsent(scopeParticipant, scopeParticipantCriteria -> {
                    final Optional<TablePermission> optionalTablePermission = dataPermissionManager.getTablePermission(table);

                    if (optionalTablePermission.isEmpty()) {
                        return null;
                    }
                    final TablePermission tablePermission = optionalTablePermission.get();

                    if (tablePermission.hasScopeCriteria()) {
                        final Map<ScopeParticipant, ScopeCriteria> scopeParticipantCriteriaMap = tablePermission.getScopeCriteriaMap();
                        final ScopeCriteria scopeCriteria = scopeParticipantCriteriaMap.get(scopeParticipant);

                        if (Objects.isNull(scopeCriteria)) {
                            return null;
                        }

                        if (scopeCriteria.isJoinsRequired()) {
                            final Table scopeTable = Table.get(table.getName());
                            final SelectQuery selectQuery = SelectQuery.get(scopeTable);

                            selectQuery.addSelectClause(scopeTable.getPKColumn());
                            selectQuery.addJoinClause(scopeCriteria.getJoinConfigs().getJoins(scopeTable));

                            if (!scopeCriteriaValues.isEmpty()) {
                                selectQuery.setWhereClause(new WhereClause(Criteria.in(scopeCriteria.getCriteriaColumn(), scopeCriteriaValues)));
                            }

                            return new WhereClause(Criteria.in(table.getPKColumn(), selectQuery));
                        } else {

                            if (!scopeCriteriaValues.isEmpty()) {
                                return new WhereClause(Criteria.in(getCriteriaColumn(table, columnAliasMap, scopeCriteria.getCriteriaColumn(table)), scopeCriteriaValues));
                            }
                        }
                    }
                    return null;
                }));
    }

    /**
     * <p>
     * Gets the criteria column
     * </p>
     *
     * @param table            Represents the {@link Table}
     * @param columnAliasMap   Contains the column aliases
     * @param criteriaColumn  Represents the criteria {@link Column}
     * @return The criteria {@link Column}
     */
    private Column getCriteriaColumn(final Table table, final Map<String, Collection<String>> columnAliasMap, final Column criteriaColumn) {
        final String criteriaColumnName = criteriaColumn.getColumnName();

        if (columnAliasMap.containsKey(criteriaColumnName)) {
            final Collection<String> columnAliases = columnAliasMap.get(criteriaColumnName);

            for (final String columnAlias : columnAliases) {
                final String columnAliasWithColumnName = String.format(COLUMN_WITH_ALIAS_FORMAT, criteriaColumnName, columnAlias);

                return table.containsColumn(columnAliasWithColumnName) ? table.getColumn(columnAliasWithColumnName) : criteriaColumn;
            }
        }
        return criteriaColumn;
    }

    /**
     * {@inheritDoc}
     *
     * @param column Represents the column to inspect for the default value
     * @return The default value associated with the {@link Column}
     */
    @Override
    public Optional<Object> getDefaultValue(final Column column) {

        return Optional.ofNullable(defaultValueMap.computeIfAbsent(column, defaultValueColumn -> {
            final Optional<ColumnPermission> optionalColumnPermission = dataPermissionManager.getColumnPermission(column);

            return optionalColumnPermission.map(columnPermission -> columnPermission.getDefaultValue().orElseGet(() -> null)).orElseGet(() -> null);
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param table Represents the table to check for the scope
     * @return The {@link Scope} associated with the {@link Table}
     */
    @Override
    public Optional<Scope> fetchScope(final Table table) {
        return Optional.ofNullable(scopeMap.computeIfAbsent(table, scopeCheckTable -> {
            final Optional<TablePermission> optionalTablePermission = dataPermissionManager.getTablePermission(table);

            return optionalTablePermission.filter(tablePermission -> {
                final Pair<Category.Type, Pair<SubCategory.Type, Action.Type>> primarySubCategoryAction = tablePermission.getPrimarySubCategoryAction();

                if (Objects.isNull(primarySubCategoryAction)) {
                    return false;
                }
                final Pair<SubCategory.Type, Action.Type> subcategoryAction = primarySubCategoryAction.getValue();

                return rolePermissionService.getPermission(orgId, roleId, primarySubCategoryAction.getKey(), subcategoryAction.getKey(), subcategoryAction.getValue()).isGranted();
            }).map(this::fetchScope).orElseGet(() -> null);
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param table Represents the table
     * @return the related table names of the given table
     */
    @Override
    public Collection<String> getRelatedTables(final Table table) {
        return dataPermissionManager.getRelatedTables(table);
    }

    /**
     * {@inheritDoc}
     *
     * @param table     The table to be filtered
     * @param operation Represents the {@link Operation}
     * @return The generated {@link WhereClause}
     */
    @Override
    public Optional<WhereClause> getWhereClause(final Table table, final Operation operation) {
        return Optional.ofNullable(tableWhereClauseMap.computeIfAbsent(table, whereClauseCheckTable -> new ConcurrentHashMap<>())
                .computeIfAbsent(operation, whereClauseCheckOperation -> dataPermissionManager.getTablePermission(table)
                        .map(tablePermission -> SecurityWhereClauseUtil.getWhereClause(table, tablePermission, whereClauseCheckOperation)
                                .orElseGet(() -> null)).orElseGet(() -> null)));
    }

    /**
     * {@inheritDoc}
     *
     * @param table         The table to be filtered.
     * @param columnAliases Which contains the collection of column aliases of the table.
     * @return The generated {@link WhereClause}.
     */
    @Override
    public Optional<WhereClause> getScopeDependentWhereClause(final Table table, final Map<String, Collection<String>> columnAliases) {

        final Optional<TablePermission> optionalTablePermission = dataPermissionManager.getTablePermission(table);

        if (optionalTablePermission.isPresent()) {
            final TablePermission dataTablePermission = optionalTablePermission.get();
            final Pair<Category.Type, Pair<SubCategory.Type, Action.Type>> primarySubCategoryAction
                    = dataTablePermission.getPrimarySubCategoryAction();

            if (Objects.nonNull(primarySubCategoryAction)) {
                final Pair<SubCategory.Type, Action.Type> subCategoryAction = primarySubCategoryAction.getValue();
                final RolePermission permission = getPermission(primarySubCategoryAction.getKey(), subCategoryAction.getKey(),
                        subCategoryAction.getValue());

                return getScopeDependentWhereClause(table, permission.getScope(), Collections.emptyMap());
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param table         The table to be filtered
     * @param scope         Represents the scope of the subcategory.
     * @param columnAliases Which contains the collection of column aliases of the table
     * @return The generated {@link WhereClause}
     */
    @Override
    public Optional<WhereClause> getScopeDependentWhereClause(final Table table, final Scope scope, final Map<String, Collection<String>> columnAliases) {
        return Optional.ofNullable(tableScopeWhereClauseMap.computeIfAbsent(table, dataTable -> new EnumMap<>(Scope.class))
                .computeIfAbsent(scope, permissionScope -> {
                    final Optional<TablePermission> optionalTablePermission = dataPermissionManager.getTablePermission(table);
                    final Optional<WhereClause> optionalWhereClause = optionalTablePermission.map(tablePermission ->
                            SecurityWhereClauseUtil.getScopeDependentWhereClause(table, scope, tablePermission).orElseGet(() -> null));

                    return optionalWhereClause.map(whereClause -> applyAliasNameChanges(table, whereClause, columnAliases)).orElseGet(() -> null);
                }));
    }

    /**
     * <p>
     * Applies the alias name changes of the column in the given {@link WhereClause}.
     * </p>
     *
     * @param table         Represents the details of the table.
     * @param whereClause   Represents the retrieval criteria.
     * @param columnAliases Which contains the collection of column aliases of the table.
     * @return {@link WhereClause}.
     */
    private WhereClause applyAliasNameChanges(final Table table, final WhereClause whereClause, final Map<String, Collection<String>> columnAliases) {
        if (whereClause.isConnector()) {
            final WhereClause left = applyAliasNameChanges(table, whereClause.getLeft(), columnAliases);
            final WhereClause right = applyAliasNameChanges(table, whereClause.getRight(), columnAliases);

            return WhereClauseUtil.connect(left, right, whereClause.getConnector());
        } else {
            final Criteria criteria = whereClause.getCriteria();
            final Column column = criteria.getColumn();
            final String columnName = column.getColumnName();

            if (Objects.equals(table.getName(), column.getTableName()) && columnAliases.containsKey(columnName)) {
                final Optional<String> columnAlias = columnAliases.get(columnName).stream().findFirst();

                if (columnAlias.isPresent()) {
                    final Column criteriaColumn = table.getColumn(columnName, columnAlias.get());

                    return new WhereClause(new Criteria(criteriaColumn, criteria.getValue(), criteria.getOperator()));
                }
            }
            return whereClause;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param column the column to evaluate
     * @return true if the column should use the configured default value, false otherwise
     */
    @Override
    public boolean isReplaceableWithDefaultValue(final Column column) {
        final Optional<ColumnPermission> optionalColumnPermission = dataPermissionManager.getColumnPermission(column);

        return !authorizationRequired && optionalColumnPermission.map(ColumnPermission::isDefaultValueAccessible).orElse(false);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link RolePermission}
     */
    @Override
    public RolePermission getRolePermission() {
        return permission;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the authorization is required for the current permission, false otherwise
     */
    @Override
    public boolean isAuthorizationRequired() {
        return authorizationRequired;
    }

    /**
     * {@inheritDoc}
     *
     * @param table           Represent the table of the columns.
     * @param modifiedColumns Represents the modified columns.
     * @param operation       Represents the operations.
     * @return Ignorable or not.
     */
    @Override
    public boolean areIgnorableColumns(final Table table, final Collection<Column> modifiedColumns, final Operation operation) {
        final Map<Operation, Collection<Column>> ignorableColumnsByOperation = this.ignorableColumns.computeIfAbsent(table,
                tableToCheck -> new EnumMap<>(Operation.class));
        final Collection<Column> ignorableColumns = ignorableColumnsByOperation.computeIfAbsent(operation, operationToCheck -> new HashSet<>());

        for (final Column modifiedColumn : modifiedColumns) {
            if (ignorableColumns.contains(modifiedColumn)) {
                continue;
            }
            final Optional<ColumnPermission> columnPermission = dataPermissionManager.getColumnPermission(modifiedColumn);

            if (columnPermission.isEmpty()) {
                return false;
            }
            final Collection<RestrictionConstraint> restrictionConstraints = columnPermission.get().getRestrictionConstraints();

            for (final RestrictionConstraint restrictionConstraint : restrictionConstraints) {
                if (RestrictionConstraint.Type.IGNORE == restrictionConstraint.type()) {
                    final Operation constraintOperation = restrictionConstraint.operation();

                    if (operation == constraintOperation) {
                        ignorableColumns.add(modifiedColumn);
                        continue;
                    }
                    ignorableColumnsByOperation.computeIfAbsent(constraintOperation, restrictOperation -> new HashSet<>()).add(modifiedColumn);
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * Retrieves a mapping of tables to their respective secure columns.
     * </p>
     *
     * <p>
     *     This method processes the given collection of tables and identifies secure columns
     * based on the data permission configuration. Each table's permissions are checked
     * through the {@code dataPermissionManager}, and if permissions exist, secure columns
     * are extracted and grouped accordingly.
     * </p>
     *
     * @param tables A collection of {@link Table} instances for which secure columns are to be determined.
     * @return A {@link Map} where the keys are {@link Table} objects and the values
     *         are collections of {@link SecureColumn}, representing the secure columns for each table.
     */
    @Override
    public Map<Table, Collection<SecureColumn>> getSecureColumns(final Collection<Table> tables) {
        final Map<Table, Collection<SecureColumn>> groupedSecureColumns = new HashMap<>();

        for (final Table table : tables) {
            dataPermissionManager.getTablePermission(table).ifPresent(tablePermission ->
                tablePermission.getColumnPermissions().stream()
                        .filter(ColumnPermission::hasSecureValueResolver).forEach(columnPermission -> {
                            final SecureColumnValueResolver secureColumnValueResolver = columnPermission.getSecureValueResolver();
                            final Column column = table.getColumn(columnPermission.getColumnName());

                            groupedSecureColumns.computeIfAbsent(table, dataTable -> new HashSet<>()).add(new SecureColumn(column, secureColumnValueResolver));
                        })
            );
        }
        return groupedSecureColumns;
    }

    /**
     * {@inheritDoc}
     *
     * @param categoryType    {@link Category.Type} holds the category related to the action.
     * @param subCategoryType {@link SubCategory.Type} holds the subcategory type related to the action.
     * @param actionType      {@link Action.Type} holds the action type.
     * @return {@link RolePermission} holds the permissions of the action.
     */
    @Override
    public RolePermission getPermission(final Category.Type categoryType, final SubCategory.Type subCategoryType,
                                        final Action.Type actionType) {
        return currentRolePermissions.getOrDefault(categoryType, Collections.emptyMap())
                .getOrDefault(subCategoryType, Collections.emptyMap()).get(actionType);
    }

    /**
     * {@inheritDoc}
     *
     * @param table the table to be joined.
     * @return a map of table names to their corresponding list of {@link JoinConfigs}.
     */
    @Override
    public Map<String, JoinConfigs> getJoinDetails(final Table table) {
        return dataPermissionManager.getTablePermission(table).map(TablePermission::getTableJoinConfigs).orElseGet(Collections::emptyMap);
    }

    /**
     * {@inheritDoc}
     *
     * @param table the table required the supporting criteria.
     * @return a map of table names to their corresponding list of {@link Criteria}.
     */
    @Override
    public Map<String, Criteria> getSupportingCriteria(final Table table) {
        return dataPermissionManager.getTablePermission(table).map(TablePermission::getSupportingCriteria).orElseGet(Collections::emptyMap);
    }

    /**
     * <p>
     * Retrieves the entity ID column associated with the specified table.
     * </p>
     *
     * @param table the table for which to retrieve the entity ID column.
     * @return an {@link Optional} containing the entity ID {@link Column} if present, or {@code Optional.empty()} if not found.
     */
    @Override
    public Optional<Column> getEntityIdColumn(final Table table) {
        return dataPermissionManager.getTablePermission(table).map(TablePermission::getEntityIdColumn);
    }

    /**
     * <p>
     * Retrieves the alternative entity ID column associated with the specified table.
     * </p>
     *
     * @param table the table for which to retrieve the entity ID column.
     * @return an {@link Optional} containing the entity ID {@link Column} if present, or {@code Optional.empty()} if not found.
     */
    public Optional<Column> getAlternativeEntityIdColumn(final Table table) {
        return dataPermissionManager.getTablePermission(table).map(TablePermission::getAlternativeEntityIdColumn);
    }

    /**
     * Generates the criteria required to enforce field-level permissions for the specified table.
     * <p>
     * This method constructs a {@link WhereClause} that represents the security constraints
     * associated with the given {@link Table}. It is used to filter query results, ensuring
     * that the current context only accesses data permitted by the defined field permissions.
     * </p>
     *
     * @param table the table for which permissions are being evaluated; must not be null.
     * @return an {@link Optional} containing the {@link WhereClause} to apply, or {@code Optional.empty()} if no restrictions exist.
     */
    @Override
    public Optional<WhereClause> getFieldPermissionWhereClause(final Table table) {
        final ReentrantReadWriteLock tableLock = fieldPermissionWhereClauseLockStore.getLock(table);
        final ReentrantReadWriteLock.ReadLock readLock = tableLock.readLock();

        readLock.lock();
        try {
            final WhereClause existingWhereClause = tableFieldPermissionWhereClauseMap.get(table);

            if (Objects.nonNull(existingWhereClause)) {
                return Optional.of(existingWhereClause);
            }
        } finally {
            readLock.unlock();
        }

        final ReentrantReadWriteLock.WriteLock writeLock = tableLock.writeLock();
        writeLock.lock();

        try {
            final WhereClause existingWhereClause = tableFieldPermissionWhereClauseMap.get(table);

            if (Objects.nonNull(existingWhereClause)) {
                return Optional.of(existingWhereClause);
            }

            if (Boolean.TRUE.equals(fieldPermissionWhereClauseInProgressMap.get(table))) {
                return Optional.empty();
            }
            fieldPermissionWhereClauseInProgressMap.put(table, true);

            try {
                final Optional<WhereClause> optionalWhereClause = dataPermissionManager.getTablePermission(table)
                        .flatMap(tablePermission -> SecurityWhereClauseUtil.getFieldPermissionWhereClause(table, tablePermission));

                optionalWhereClause.ifPresent(whereClause -> tableFieldPermissionWhereClauseMap.putIfAbsent(table, whereClause));
                return optionalWhereClause;
            } finally {
                fieldPermissionWhereClauseInProgressMap.remove(table);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Sets the {@link RolePermission} into the {@link DataPermissionHandler}
     *
     * @param rolePermission Represents the role permission
     */
    public void setRolePermission(final RolePermission rolePermission) {
        this.permission = rolePermission;
        this.hasPermission = Objects.nonNull(rolePermission);

        setAuthorizationRequired();
    }

    /**
     * Fetches the appropriate {@link Action.Type} for the specified {@link Operation}
     *
     * @param subCategoryType Represents the type subcategory type relevant to the operation
     * @param operation       Represents the operation to obtain the {@link Action.Type}
     * @return The {@link Action.Type} associated with the {@link Operation}
     */
    private Action.Type fetchActionType(final SubCategory.Type subCategoryType, final Operation operation) {

        return switch (operation) {
            case SELECT -> switch (subCategoryType) {
                case DEALS, CONTACTS, COMPANIES, PRODUCTS, MANAGE_SALES_ACTIVITIES, MANAGE_ROLES, WEB_FORM_TRACKING,
                     CALL_LOGS -> Action.Type.VIEW;
                case CREATE_GOALS, EDIT_GOALS, DELETE_GOALS, RECALCULATE_GOALS,
                     CREATE_QUOTAS, EDIT_QUOTAS, DELETE_QUOTAS, TEAM_VIEW, HIERARCHY_VIEW,
                     COMMIT_DEAL, OVERRIDE -> null;
                default -> Action.Type.ACCESS;
            };
            case INSERT -> switch (subCategoryType) {
                case DEALS, CONTACTS, COMPANIES, PRODUCTS, MANAGE_SALES_ACTIVITIES, MANAGE_ROLES, WEB_FORM_TRACKING,
                     CALL_LOGS -> Action.Type.CREATE;
                case VIEW_GOALS, EDIT_GOALS, DELETE_GOALS, RECALCULATE_GOALS,
                     VIEW_QUOTAS, EDIT_QUOTAS, DELETE_QUOTAS, TEAM_VIEW, HIERARCHY_VIEW,
                     COMMIT_DEAL, OVERRIDE -> null;
                default -> Action.Type.ACCESS;
            };
            case UPDATE -> switch (subCategoryType) {
                case DEALS, CONTACTS, COMPANIES, PRODUCTS, MANAGE_SALES_ACTIVITIES, MANAGE_ROLES, WEB_FORM_TRACKING,
                     CALL_LOGS -> Action.Type.EDIT;
                case VIEW_GOALS, CREATE_GOALS, DELETE_GOALS, RECALCULATE_GOALS,
                     VIEW_QUOTAS, CREATE_QUOTAS, DELETE_QUOTAS, TEAM_VIEW, HIERARCHY_VIEW,
                     COMMIT_DEAL, OVERRIDE -> null;
                default -> Action.Type.ACCESS;
            };
            case DELETE -> switch (subCategoryType) {
                case DEALS, CONTACTS, COMPANIES, PRODUCTS, MANAGE_SALES_ACTIVITIES, MANAGE_ROLES, WEB_FORM_TRACKING,
                     CALL_LOGS -> Action.Type.DELETE;
                case VIEW_GOALS, EDIT_GOALS, CREATE_GOALS, RECALCULATE_GOALS,
                     VIEW_QUOTAS, EDIT_QUOTAS, CREATE_QUOTAS, TEAM_VIEW, HIERARCHY_VIEW,
                     COMMIT_DEAL, OVERRIDE -> null;
                default -> Action.Type.ACCESS;
            };
        };
    }

    /**
     * Fetches the appropriate {@link Scope} for the specified table permission
     *
     * @param tablePermission Represents the table permission
     * @return the {@link Scope} associated with the {@link TablePermission}
     */
    private Scope fetchScope(final TablePermission tablePermission) {

        if (hasPermission) {
            final SubCategory.Type subcategoryType = permission.getSubCategory().getType();

            if (tablePermission.isAssociatedWith(subcategoryType)) {
                return permission.getScope();
            }
        }
        final Map<Category.Type, Map<SubCategory.Type, Collection<Action.Type>>> categoryActions = tablePermission.getSubCategoryActions();

        if (categoryActions.size() == 1) {
            final Map.Entry<Category.Type, Map<SubCategory.Type, Collection<Action.Type>>> categoryActionsEntry = categoryActions.entrySet().stream().findAny().get();
            final Map<SubCategory.Type, Collection<Action.Type>> subCategoryActions = categoryActionsEntry.getValue();

            if (subCategoryActions.size() == 1) {
                final SubCategory.Type subCategoryType = subCategoryActions.keySet().stream().findAny().get();
                final Optional<RolePermission> optionalActionPermission = getActionPermission(categoryActionsEntry.getKey(), subCategoryType, Action.Type.ACCESS);

                if (optionalActionPermission.isPresent()) {
                    return optionalActionPermission.get().getScope();
                }
            }
        }
        return null;
    }

    /**
     * Gets the permissions grouped by the action types for the given subcategory type
     *
     * @param categoryType    Represents the category type
     * @param subCategoryType Represents the sub category type
     * @return the {@link Map} of permissions for the given subcategory type grouped by the action types
     */
    private Map<Action.Type, RolePermission> getActionPermissions(final Category.Type categoryType, final SubCategory.Type subCategoryType) {
        final Collection<RolePermission> rolePermissions = rolePermissionService.getPermissions(orgId, roleId, categoryType, subCategoryType);

        if (!rolePermissions.isEmpty()) {
            final Map<Action.Type, RolePermission> subCategoryRolePermissions = permissions.computeIfAbsent(subCategoryType,
                    subCategoryTypeToLoad -> new HashMap<>());

            for (final RolePermission rolePermission : rolePermissions) {
                subCategoryRolePermissions.putIfAbsent(rolePermission.getAction().getType(), rolePermission);
            }
            return Collections.unmodifiableMap(subCategoryRolePermissions);
        }
        return Collections.emptyMap();
    }

    /**
     * Gets the permission of the given subcategory and action types
     *
     * @param categoryType    Represents the type of the category
     * @param subCategoryType Represents the type of the subcategory
     * @param actionType      Represents the type of the action
     * @return the associated {@link RolePermission}
     */
    private Optional<RolePermission> getActionPermission(final Category.Type categoryType, final SubCategory.Type subCategoryType,
                                                         final Action.Type actionType) {
        return Optional.ofNullable(getActionPermissions(categoryType, subCategoryType).get(actionType));
    }

    /**
     * Sets the isAuthorizationRequired flag
     */
    private void setAuthorizationRequired() {

        if (hasPermission) {
            final SubCategory.Type subCategoryType = permission.getSubCategory().getType();
            final Action.Type actionType = permission.getAction().getType();

            this.authorizationRequired = !(((SubCategory.Type.WEB_FORM_TRACKING == subCategoryType && Action.Type.SUBMIT == actionType)
                    || (SubCategory.Type.MANAGE_WORKFLOWS == subCategoryType && Action.Type.EXECUTE == actionType))
                    && permission.isGranted());
        } else {
            this.authorizationRequired = true;
        }
    }
}