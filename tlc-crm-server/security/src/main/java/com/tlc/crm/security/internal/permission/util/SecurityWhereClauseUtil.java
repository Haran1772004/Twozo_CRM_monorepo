package com.tlc.crm.security.internal.permission.util;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.handler.DataPermissionHandler;
import com.tlc.crm.security.internal.permission.model.TablePermission;
import com.tlc.crm.security.internal.permission.registry.DataPermissionHandlerRegistry;
import com.tlc.crm.security.model.Rule;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.ScopeParticipant;

import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for handling {@link WhereClause} related functionality
 *
 * @author Sundar
 * @version 1.0
 */
public final class SecurityWhereClauseUtil {

    private SecurityWhereClauseUtil() {
    }

    /**
     * Gets the {@link WhereClause} for filtering records based on the provided scope for the specified table
     *
     * @param user            The user used to retrieve the respective scope values for filtering records
     * @param scope           Represents the scope for which the filter should be applied
     * @param filterableTable Represents the table where the filter is to be applied
     * @param columnAliases   Which contains the collection of column aliases of the table
     * @return {@link WhereClause} for filtering the records
     */
    public static Optional<WhereClause> getScopeWhereClauses(final BaseCrmUser user, final Scope scope, final Table filterableTable,
                                                             final Map<String, Collection<String>> columnAliases) {
        final DataPermissionHandler dataPermissionHandler = DataPermissionHandlerRegistry.getInstance().get();
        final Optional<WhereClause> scopeDependentWhereClause = dataPermissionHandler.getScopeDependentWhereClause(filterableTable, scope, columnAliases);
        final Optional<WhereClause> optionalWhereClause = Optional.ofNullable(switch (scope) {
            case TEAM_QUOTAS, TEAMS_GOALS -> getTeamWhereClause(user, filterableTable, columnAliases);
            case TEAM_LEVEL_ACCESS -> getTeamAndUserWhereClause(user, filterableTable, columnAliases);
            case TERRITORY_ACCESS -> getTerritoryWhereClause(user, filterableTable, columnAliases);
            case RESTRICTED_ACCESS, USERS_GOALS, USERS_QUOTAS, QUOTAS_CREATED_BY_USER ->
                    getUserWhereClause(user, filterableTable, columnAliases);
            case REPORT_LEVEL_ACCESS, USER_LEVEL_ACCESS -> {
                final List<Long> userIds = List.of(user.id());

                yield WhereClauseUtil.connect(
                        getScopeCriteria(filterableTable, ScopeParticipant.USER, columnAliases, userIds).orElseGet(() -> null),
                        getScopeCriteria(filterableTable, ScopeParticipant.TEAM_USER, columnAliases, userIds).orElseGet(() -> null), Connector.OR
                );
            }
            default -> null;
        });

        return optionalWhereClause.map(whereClause -> scopeDependentWhereClause.map(whereClause::or).orElse(whereClause));
    }

    /**
     * Generates a {@link WhereClause} by validating the given {@link TablePermission} to filter records from the specified table
     *
     * @param table           The table to be filtered
     * @param tablePermission Represents the table permission to validate
     * @param operation       Represents the {@link Operation}
     * @return The generated {@link WhereClause}
     */
    public static Optional<WhereClause> getWhereClause(final Table table, final TablePermission tablePermission, final Operation operation) {

        if (tablePermission.hasFilters()) {
            return Optional.ofNullable(buildWhereClause(table, tablePermission.getFilters(operation)));
        }
        return Optional.empty();
    }

    /**
     * Generates a {@link WhereClause} by validating the given {@link TablePermission} to filter records from the specified table
     *
     * @param table           The table to be filtered
     * @param tablePermission Represents the table permission to validate
     * @return The generated {@link WhereClause}
     */
    public static Optional<WhereClause> getScopeDependentWhereClause(final Table table, final Scope scope, final TablePermission tablePermission) {

        if (tablePermission.hasScopeDependentFilters()) {
            return Optional.ofNullable(buildWhereClause(table, tablePermission.getScopeDependentFilters(scope)));
        }
        return Optional.empty();
    }

    /**
     * Constructs the filter criteria required to enforce field-level permission constraints.
     * <p>
     * This method inspects the provided {@link TablePermission} to determine if any field-specific
     * filters are active. If filters are present, it delegates to {@code buildWhereClause} to generate
     * the SQL logic restricted to the context of the given {@link Table}.
     * </p>
     *
     * @param table           the table definition to which the criteria apply; must not be null.
     * @param tablePermission the permission object containing potential field permission filters; must not be null.
     * @return an {@link Optional} containing the generated {@link WhereClause}, or {@code Optional.empty()}
     * if no field permission filters are defined.
     */
    public static Optional<WhereClause> getFieldPermissionWhereClause(final Table table, final TablePermission tablePermission) {

        if (tablePermission.hasFieldPermissionFilters()) {
            return Optional.ofNullable(buildWhereClause(table, tablePermission.getFieldPermissionFilters()));
        }
        return Optional.empty();
    }

    /**
     * Retrieves the territory {@link WhereClause} for the specified user and table.
     * This filter ensures that the table contains records either belonging to the current user or within the user's territory.
     *
     * @param user          The user whose territory is to be added to the WHERE clause filter
     * @param table         The table
     * @param columnAliases Contains the column aliases
     * @return The territory {@link WhereClause}
     */
    private static WhereClause getTerritoryWhereClause(final BaseCrmUser user, final Table table, final Map<String, Collection<String>> columnAliases) {
        final WhereClause userWhereClause = getUserWhereClause(user, table, columnAliases);

        if (Objects.nonNull(userWhereClause)) {
            final Optional<WhereClause> optionalTerritoryCriteria = getScopeCriteria(table, ScopeParticipant.TERRITORY, columnAliases, user.getTerritoryIds());

            if (optionalTerritoryCriteria.isEmpty()) {
                return userWhereClause;
            } else {
                final Collection<Long> userTerritoryIds = user.getTerritoryIds();

                return userTerritoryIds.isEmpty() ? userWhereClause
                        : userWhereClause.or(optionalTerritoryCriteria.get());
            }
        }
        return null;
    }

    /**
     * Retrieves the team {@link WhereClause} for the specified user and table.
     * This filter ensures that the table contains records either belonging to the user's team
     *
     * @param user          The user whose team is to be added to the WHERE clause filter
     * @param table         The table
     * @param columnAliases Contains the column aliases
     * @return The team {@link WhereClause}
     */
    private static WhereClause getTeamWhereClause(final BaseCrmUser user, final Table table, final Map<String, Collection<String>> columnAliases) {
        final Collection<Long> teamIds = user.getTeamIds();

        if (Objects.isNull(teamIds)) {
            return null;
        }
        final Optional<WhereClause> optionalTeamCriteria = getScopeCriteria(table, ScopeParticipant.TEAM, columnAliases, teamIds);

        return optionalTeamCriteria.orElseGet(() -> null);
    }

    /**
     * Retrieves the team and user {@link WhereClause} for the specified user and table.
     * This filter ensures that the table contains records either belonging to the user's team or the user.
     *
     * @param user          The user whose team and user ID are to be added to the WHERE clause filter
     * @param table         The table
     * @param columnAliases Contains the column aliases
     * @return The combined team and user {@link WhereClause}
     */
    private static WhereClause getTeamAndUserWhereClause(final BaseCrmUser user, final Table table, final Map<String, Collection<String>> columnAliases) {
        final WhereClause teamWhereClause = getTeamWhereClause(user, table, columnAliases);
        final WhereClause userWhereClause = getUserWhereClause(user, table, columnAliases);

        return WhereClauseUtil.connect(teamWhereClause, userWhereClause, Connector.OR);
    }

    /**
     * Retrieves the user {@link WhereClause} for the specified user and table.
     * This filter ensures that the table contains records either belonging to the current user.
     *
     * @param user          The user to be added to the WHERE clause filter
     * @param table         The table
     * @param columnAliases Contains the column aliases
     * @return The user {@link WhereClause}
     */
    private static WhereClause getUserWhereClause(final BaseCrmUser user, final Table table, final Map<String, Collection<String>> columnAliases) {
        final Optional<WhereClause> optionalUserCriteria = getScopeCriteria(table, ScopeParticipant.USER, columnAliases, List.of(user.id()));

        return optionalUserCriteria.orElseGet(() -> null);
    }

    /**
     * Retrieves the scope column from the specified table for the given scope participant
     *
     * @param table               The table from which to retrieve the scope column
     * @param scopeParticipant    The participant for the scope
     * @param columnAliasMap      Contains the column aliases
     * @param scopeCriteriaValues Contains the scope criteria values
     * @return The scope column
     */
    private static Optional<WhereClause> getScopeCriteria(final Table table, final ScopeParticipant scopeParticipant,
                                                          final Map<String, Collection<String>> columnAliasMap, final Collection<Long> scopeCriteriaValues) {
        final DataPermissionHandler dataPermissionHandler = DataPermissionHandlerRegistry.getInstance().get();

        return dataPermissionHandler.fetchScopeCriteria(table, scopeParticipant, columnAliasMap, scopeCriteriaValues);
    }

    /**
     * Retrieves filters to be applied to the data retrieved from the database
     *
     * @param tables         A collection of {@link Table} to fetch filters for
     * @param columnAliasMap Which contains the collection of column aliases grouped by the table names
     * @return A {@link Map} containing filters corresponding to the specified tables
     */
    public static Map<Table, WhereClause> getScopeWhereClauses(final Collection<Table> tables, final Map<String, Map<String, Collection<String>>> columnAliasMap) {
        final Map<Table, WhereClause> postFilters = new HashMap<>();
        final BaseCrmUser user = LoggedInUserInfo.getSecurityContext().getUser();
        final DataPermissionHandler dataPermissionHandler = DataPermissionHandlerRegistry.getInstance().get();

        for (final Table table : tables) {
            final Optional<Scope> optionalScope = dataPermissionHandler.fetchScope(table);

            optionalScope.ifPresent(scope -> {
                final Map<String, Collection<String>> columnWithAliases =
                        new HashMap<>(columnAliasMap.getOrDefault(table.getName(), Collections.emptyMap()));
                final Map<String, Collection<String>> columnAliasesByTableAlias =
                        columnAliasMap.getOrDefault(table.getAlias(), Collections.emptyMap());

                columnAliasesByTableAlias.forEach((column, aliases) ->
                    columnWithAliases.computeIfAbsent(column, columnAlias -> new HashSet<>()).addAll(aliases));
                final Optional<WhereClause> optionalWhereClause = getScopeWhereClauses(user, optionalScope.get(), table, columnWithAliases);

                optionalWhereClause.ifPresent(whereClause -> postFilters.putIfAbsent(table, whereClause));
            });
        }
        return postFilters;
    }


    /**
     * Builds the {@link WhereClause} for filtering records from the specified {@link Table},
     * based on the provided filters
     *
     * @param table   The table to be filtered
     * @param filters Contains a collection of {@link TablePermission.Filter}
     * @return The constructed {@link WhereClause} for the {@link Table}
     */
    private static WhereClause buildWhereClause(final Table table, final Collection<TablePermission.Filter> filters) {

        if (Objects.isNull(filters) || filters.isEmpty()) {
            return null;
        }
        WhereClause whereClause = null;
        final SecurityContext context = LoggedInUserInfo.getSecurityContext();

        for (final TablePermission.Filter filter : filters) {
            final Rule<?, SecurityContext> rule = filter.getRule();

            if (Objects.isNull(rule) || rule.test(context)) {
                final WhereClause currentWhereClause;

                if (filter.hasNestedFilter()) {
                    final Criteria criteria = filter.buildFilter(table);
                    final WhereClause innerWhereClause = buildWhereClause(table, filter.getNestedFilters());

                    currentWhereClause = Objects.isNull(criteria) ? innerWhereClause : WhereClauseUtil.connect(new WhereClause(criteria),
                            innerWhereClause, filter.getConnector());
                } else {
                    final Criteria criteria = filter.buildFilter(table);

                    currentWhereClause = Objects.isNull(criteria) ? null : new WhereClause(criteria);
                }

                if (Objects.isNull(whereClause)) {
                    whereClause = currentWhereClause;
                } else if (filter.hasConnector()) {
                    whereClause = WhereClauseUtil.connect(whereClause, currentWhereClause, filter.getConnector());
                }
            }
        }
        return whereClause;
    }
}