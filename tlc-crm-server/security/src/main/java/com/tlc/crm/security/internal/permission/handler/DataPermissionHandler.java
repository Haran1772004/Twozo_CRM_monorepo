package com.tlc.crm.security.internal.permission.handler;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.crm.security.internal.permission.model.ScopeColumnInfo;
import com.tlc.crm.security.internal.permission.model.SecureColumn;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.ScopeParticipant;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Handler to authorize the tables and columns to achieve data access
 *
 * @author Sundar
 * @version 1.0
 */
public interface DataPermissionHandler {

    /**
     * Checks whether the table has the necessary accesses for the given operation
     *
     * @param table     Represents the table to check for the accesses
     * @param operation Represents the operation
     * @return true if the table has the required accesses; otherwise, false
     */
    boolean hasAccess(final Table table, final Operation operation);

    /**
     * Checks whether the column has the required access for the given operation. Note: This method does not guarantee table access; it directly assesses column access.
     *
     * @param column    The column to check for access
     * @param operation The operation being performed
     * @return true if the column has the necessary access; otherwise, false
     */
    boolean hasAccess(final Column column, final Operation operation);

    /**
     * Checks if the column has the necessary access for the given data and operation.
     * Note: This method directly assesses column access and does not guarantee table access.
     *
     * @param column    the column to check for access
     * @param data      the data to check
     * @param operation the operation being performed
     * @return true if the column has the required access; otherwise, false
     */
    boolean hasAccess(final Column column, final Object data, final Operation operation);

    /**
     * Gets the relevant scope column along with the required join clauses of the {@link ScopeParticipant} from the specified table
     *
     * @param table            Represents the table containing the column
     * @param scopeParticipant Represents the {@link ScopeParticipant}
     * @return The {@link ScopeColumnInfo} containing the column and its required joins, or empty if not found
     */
    Optional<ScopeColumnInfo> fetchScopeColumn(final Table table, final ScopeParticipant scopeParticipant);

    /**
     * <p>
     * Gets the relevant scope criteria of the {@link ScopeParticipant} from the specified table
     * </p>
     *
     * @param table               Represents the table containing the column
     * @param scopeParticipant    Represents the {@link ScopeParticipant}
     * @param columnAliasMap      Contains the column aliases
     * @param scopeCriteriaValues Contains the scope criteria values
     * @return The {@link WhereClause} associated with the {@link ScopeParticipant}
     */
    Optional<WhereClause> fetchScopeCriteria(final Table table, final ScopeParticipant scopeParticipant,
                                             final Map<String, Collection<String>> columnAliasMap, final Collection<Long> scopeCriteriaValues);

    /**
     * Gets the default value of the specified {@link Column}
     *
     * @param column Represents the column to inspect for the default value
     * @return The default value associated with the {@link Column}
     */
    Optional<Object> getDefaultValue(final Column column);

    /**
     * Fetches the {@link } of the specified {@link Table}
     *
     * @param table Represents the table to check for the scope
     * @return The {@link Scope} associated with the {@link Table}
     */
    Optional<Scope> fetchScope(final Table table);

    /**
     * Gets the related table names of the given table
     *
     * @param table Represents the table
     * @return the related table names of the given table
     */
    Collection<String> getRelatedTables(final Table table);

    /**
     * Generates a {@link WhereClause} by authorizing it to filter records from the specified table
     *
     * @param table     The table to be filtered
     * @param operation Represents the {@link Operation}
     * @return The generated {@link WhereClause}
     */
    Optional<WhereClause> getWhereClause(final Table table, final Operation operation);

    /**
     * <p>
     * Generates a {@link WhereClause} by authorizing it to filter records from the specified table based on scope.
     * </p>
     *
     * @param table         The table to be filtered.
     * @param columnAliases Which contains the collection of column aliases of the table.
     * @return The generated {@link WhereClause}.
     */
    Optional<WhereClause> getScopeDependentWhereClause(final Table table, final Map<String, Collection<String>> columnAliases);

    /**
     * <p>
     * Generates a {@link WhereClause} by authorizing it to filter records from the specified table based on scope.
     * </p>
     *
     * @param table         The table to be filtered.
     * @param scope         Represents the scope of the subcategory.
     * @param columnAliases Which contains the collection of column aliases of the table.
     * @return The generated {@link WhereClause}.
     */
    Optional<WhereClause> getScopeDependentWhereClause(final Table table, final Scope scope, final Map<String, Collection<String>> columnAliases);

    /**
     * Checks if the column can be replaced with the default value when access is denied
     *
     * @param column the column to evaluate
     * @return true if the column should use the configured default value, false otherwise
     */
    boolean isReplaceableWithDefaultValue(final Column column);

    /**
     * Gets the {@link RolePermission}
     *
     * @return {@link RolePermission}
     */
    RolePermission getRolePermission();

    /**
     * Determines whether the authorization is required for the current permission or not
     *
     * @return true if the authorization is required for the current permission, false otherwise
     */
    boolean isAuthorizationRequired();

    /**
     * <p>
     * Checks whether the given columns are ignorable or not.
     * </p>
     *
     * @param table           Represent the table of the columns.
     * @param modifiedColumns Represents the modified columns.
     * @param operation       Represents the operations.
     * @return Ignorable or not.
     */
    boolean areIgnorableColumns(final Table table, final Collection<Column> modifiedColumns, final Operation operation);

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
    Map<Table, Collection<SecureColumn>> getSecureColumns(final Collection<Table> tables);

    /**
     * <p>
     * Fetches the non private action {@link RolePermission} associated with the provided organization, role, subcategory type, and action type.
     * </p>
     *
     * @param categoryType    {@link Category.Type} holds the category related to the action.
     * @param subCategoryType {@link SubCategory.Type} holds the subcategory type related to the action.
     * @param actionType      {@link Action.Type} holds the action type.
     * @return {@link RolePermission} holds the permissions of the action.
     */
    RolePermission getPermission(final Category.Type categoryType, final SubCategory.Type subCategoryType, final Action.Type actionType);

    /**
     * Returns the join details required to join the specified table with another table.
     *
     * @param table the table to be joined.
     * @return a map of table names to their corresponding list of {@link JoinConfigs}.
     */
    Map<String, JoinConfigs> getJoinDetails(final Table table);

    /**
     * Returns the supporting criteria required to support the actual criteria.
     *
     * @param table the table required the supporting criteria.
     * @return a map of table names to their corresponding list of {@link Criteria}.
     */
    Map<String, Criteria> getSupportingCriteria(final Table table);

    /**
     * <p>
     * Retrieves the entity ID column associated with the specified table.
     * </p>
     *
     * @param table the table for which to retrieve the entity ID column.
     * @return an {@link Optional} containing the entity ID {@link Column} if present, or {@code Optional.empty()} if not found.
     */
    Optional<Column> getEntityIdColumn(final Table table);

    /**
     * <p>
     * Retrieves the alternative entity ID column associated with the specified table.
     * </p>
     *
     * @param table the table for which to retrieve the entity ID column.
     * @return an {@link Optional} containing the entity ID {@link Column} if present, or {@code Optional.empty()} if not found.
     */
    Optional<Column> getAlternativeEntityIdColumn(final Table table);

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
    Optional<WhereClause> getFieldPermissionWhereClause(final Table table);
}