package com.tlc.crm.core.features.action.quota.list;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.sql.resource.OMQFFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.entity.list.filter.loader.impl.DefaultJoinLoader;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.WhereClauseBuilder;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;

/**
 * <p>
 * Process the team view list actions in the quota and Provides the implementation for the {@link QuotaSQLListInterpreter}.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class QuotaTeamViewSQLListInterpreter extends QuotaSQLListInterpreter {

    private final boolean hasTeamCriteria;
    private final boolean hasUserCriteria;

    public QuotaTeamViewSQLListInterpreter(final ListRequestData listRequestData, final Provider provider) {
        super(listRequestData, provider);
        hasTeamCriteria = criteriaJson.containsKey(QuotaListFields.TEAM_IDS);
        hasUserCriteria = criteriaJson.containsKey(QuotaListFields.USER_IDS);

        if (hasTeamCriteria && !hasUserCriteria) {
            primaryTable = getTeamQuotaTable();
            selectQuery = SelectQuery.get(primaryTable);
        }
        final Table teamTable = Table.get(OMCRMTEAM.TABLE);
        final Column quotaPKColumn = primaryTable.getPKColumn();
        final DefaultJoinLoader defaultJoinLoader = new DefaultJoinLoader(selectQuery, JoinClause.JoinType.LEFT);

        loadBaseSelectClause();

        if (hasUserCriteria) {
            selectQuery.addSelectClause(List.of(userQuotaTable.getColumn(OMQFUSERQUOTA.ID, QuotaListFields.USER_QUOTA_ID),
                    teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.ID, QuotaListFields.TEAM_USER_QUOTA_ID), userQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID)));
            defaultJoinLoader.load(quotaPKColumn, userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID));
            defaultJoinLoader.load(quotaPKColumn, teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID));
            defaultJoinLoader.load(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID));
            defaultJoinLoader.load(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), teamTable.getPKColumn());
            loadUserTeamSelectAndJoin(teamUserQuotaTable, teamQuotaTable);
        }

        if (hasTeamCriteria || hasUserCriteria) {
            selectQuery.addSelectClause(List.of(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), teamQuotaTable.getColumn(OMQFTEAMQUOTA.ID, QuotaListFields.TEAM_QUOTA_ID),
                    teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID, getAliasName(QuotaListFields.TEAM_QUOTA_BASE_QUOTA_ID,
                            QuotaListFields.TEAM_QUOTA))));

            if (hasUserCriteria) {
                defaultJoinLoader.load(quotaPKColumn, teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID));
            } else {
                defaultJoinLoader.load(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), getTable().getPKColumn());
            }
            defaultJoinLoader.load(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), teamTable.getPKColumn());
        }
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void orderByClause(final String field, final boolean isAsc) {
    }

    /**
     * {@inheritDoc}
     *
     * @param configuredField Represents the {@link ConfiguredField} details
     */
    @Override
    public void selectClause(final ConfiguredField configuredField) {

        if (configuredField.isEnabled()) {
            final FieldConfig fieldConfig = listStructure.getField(configuredField.fieldName());

            if (Objects.nonNull(fieldConfig)) {
                final String fieldName = fieldConfig.getName();

                if (fieldConfig.isPersistent()) {

                    if (hasUserCriteria) {
                        final DefaultJoinLoader defaultJoinLoader = new DefaultJoinLoader(selectQuery, JoinClause.JoinType.LEFT);

                        loadUserQuota(fieldConfig, defaultJoinLoader);
                    }

                    if (hasTeamCriteria || hasUserCriteria) {
                        final DefaultJoinLoader defaultJoinLoader = new DefaultJoinLoader(selectQuery, JoinClause.JoinType.LEFT);
                        final Table forecastSummaryTable = Table.get(OMQFFORECASTSUMMARY.TABLE);
                        final Table teamQuotaForecastSummary = Table.get(OMQFTEAMQUOTAFORECASTSUMMARY.TABLE);
                        final Table forecastCategoryTeamAliasTable = getAliasTable(forecastSummaryTable, QuotaListFields.TEAM_QUOTA);
                        final Table forecastCategoryTeamAliasDerivedTable = getDerivedTable(fieldConfig, forecastCategoryTeamAliasTable,
                                forecastCategoryTeamAliasTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID));

                        addOverrideValue(selectQuery, forecastCategoryTeamAliasDerivedTable, fieldConfig, QuotaListFields.TEAM_QUOTA);
                        addWonSelectClause(selectQuery, forecastCategoryTeamAliasDerivedTable, fieldName, QuotaListFields.TEAM_QUOTA);

                        defaultJoinLoader.load(teamQuotaTable.getPKColumn(), teamQuotaForecastSummary.getColumn(OMQFTEAMQUOTAFORECASTSUMMARY.TEAM_QUOTA_ID));
                        defaultJoinLoader.load(teamQuotaForecastSummary.getColumn(OMQFTEAMQUOTAFORECASTSUMMARY.SUMMARY_ID), forecastCategoryTeamAliasDerivedTable.getPKColumn());
                    }
                } else {

                    switch (fieldName) {
                        case QuotaListFields.USER_AND_TEAM -> {

                            if (hasTeamCriteria || hasUserCriteria) {
                                addTeamUserAndTeamSelect(selectQuery);
                            }

                            if (hasUserCriteria) {
                                addUserSelect(selectQuery, fieldName);
                            }
                        }

                        case QuotaListFields.QUOTA -> {

                            if (hasTeamCriteria || hasUserCriteria) {
                                selectQuery.addSelectClause(getTable().getColumn(OMQFQUOTA.TARGET, getAliasName(fieldName, QuotaListFields.TEAM_QUOTA)));
                            }

                            if (hasUserCriteria) {
                                selectQuery.addSelectClause(List.of(primaryTable.getColumn(OMQFQUOTA.TARGET, getAliasName(fieldName, QuotaListFields.USER_QUOTA)),
                                        teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TARGET, getAliasName(fieldName, QuotaListFields.TEAM_USER_QUOTA))));
                            }
                        }

                        case QuotaListFields.WON -> {

                            if (hasTeamCriteria || hasUserCriteria) {
                                selectQuery.addSelectClause(teamQuotaTable.getColumn(OMQFTEAMQUOTA.WON, getAliasName(QuotaListFields.QUOTA_WON_VALUE, QuotaListFields.TEAM_QUOTA)));
                                selectQuery.addSelectClause(teamQuotaTable.getColumn(OMQFTEAMQUOTA.WON, getAliasName(QuotaListFields.WON, QuotaListFields.TEAM_QUOTA)));
                            }

                            if (hasUserCriteria) {
                                selectQuery.addSelectClause(List.of(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.WON, getAliasName(QuotaListFields.QUOTA_WON_VALUE, QuotaListFields.TEAM_USER_QUOTA)),
                                        userQuotaTable.getColumn(OMQFUSERQUOTA.WON, getAliasName(QuotaListFields.QUOTA_WON_VALUE, QuotaListFields.USER_QUOTA))));
                                selectQuery.addSelectClause(List.of(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.WON, getAliasName(QuotaListFields.WON, QuotaListFields.TEAM_USER_QUOTA)),
                                        userQuotaTable.getColumn(OMQFUSERQUOTA.WON, getAliasName(QuotaListFields.WON, QuotaListFields.USER_QUOTA))));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The total number of hits.
     */
    @Override
    public long fetchTotalHits() {
        final ReadableDataStore readableDataStore = getReadableDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(primaryTable);
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table userQuotaTable = getUserQuotaTable();
        final Collection<Long> teamQuotaIds = new HashSet<>();
        final Collection<Long> userQuotaIds = new HashSet<>();
        final Collection<Long> teamUserQuotaIds = new HashSet<>();

        loadBaseJoins(selectQuery);
        selectQuery.addJoinClause(derivedQuery.getJoinClause());
        selectQuery.setWhereClause(derivedQuery.getWhereClause());
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        if (hasTeamCriteria) {
            final Collection<Long> selectedTeamIds = collectIdsFromJson(QuotaListFields.TEAM_IDS);

            dataContainer.getRows(teamQuotaTable).forEach(row -> {
                if (selectedTeamIds.contains(row.get(OMQFTEAMQUOTA.TEAM_ID))) {
                    teamQuotaIds.add(row.getPKValue());
                }
            });
        } else if (hasUserCriteria) {
            dataContainer.getRows(teamQuotaTable).forEach(row -> {
                if (Objects.isNull(row.get(OMQFTEAMQUOTA.TEAM_ID))) {
                    teamQuotaIds.add(row.getPKValue());
                }
            });
        }

        if (hasUserCriteria) {
            final Collection<Long> userIds = collectIdsFromJson(QuotaListFields.USER_IDS);

            dataContainer.getRows(userQuotaTable).forEach(row -> {
                final Long userId = row.get(OMQFUSERQUOTA.USER_ID);

                if (Objects.nonNull(userId) && userIds.contains(userId)) {
                    userQuotaIds.add(row.getPKValue());
                }
            });

            dataContainer.getRows(teamUserQuotaTable).forEach(row -> {
                final Long userId = row.get(OMQFTEAMUSERQUOTA.USER_ID);

                if (Objects.nonNull(userId) && userIds.contains(userId)) {
                    teamUserQuotaIds.add(row.getPKValue());
                }
            });
        }

        if (hasUserCriteria) {
            return userQuotaIds.size() + teamUserQuotaIds.size() + teamQuotaIds.size();
        } else if (hasTeamCriteria) {
            return teamQuotaIds.size();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of maps containing the mapped data
     */
    @Override
    public Collection<Map<String, Object>> getResult() {
        final LimitClause limitClause = derivedQuery.getLimitClause();
        final int start = limitClause.getStart();
        final int limit = limitClause.getNumberOfResults();
        final QuotaIdsHolder quotaIdsHolder = getQuotaIdsHolder(start, limit);

        if (quotaIdsHolder.isEmpty()) {
            return Collections.emptyList();
        }
        final long listDataFetchStartTime = System.currentTimeMillis();
        WhereClause whereClause = null;

        if (hasUserCriteria && hasTeamCriteria) {
            replaceTeamUserQuotaJoinWithDerivedTable(quotaIdsHolder.teamUserQuotaIds());
            whereClause = buildCriteriaForUserAndTeam(quotaIdsHolder);
        } else if (hasUserCriteria) {
            whereClause = buildCriteriaForUser(quotaIdsHolder);
        } else if (hasTeamCriteria) {
            whereClause = buildCriteriaForTeam(quotaIdsHolder);
        }

        if (Objects.isNull(whereClause)) {
            return Collections.emptyList();
        }

        selectQuery.setWhereClause(whereClause);
        applyOrderByClause(selectQuery);

        LOGGER.debug("Quota team view list action - [{}]: Going to fetch the data from the database, Org ID: {}, User ID: {}", provider, orgId, userId);
        final List<Map<String, Object>> data = getReadableDataStore(orgId).getData(selectQuery);

        for (final Map<String, Object> row : data) {
            if (!row.containsKey(QuotaListFields.USER_AND_TEAM) && row.containsKey(QuotaListFields.TEAM_QUOTA_ID)) {
                row.put(QuotaListFields.USER_AND_TEAM, row.get(QuotaListFields.TEAM_QUOTA_ID));
            }
            clearUnrequestedTeamQuotaId(row, quotaIdsHolder.teamQuotaIds());
        }

        LOGGER.debug("Quota team view action - [{}]: Data are fetched from the database in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - listDataFetchStartTime, orgId, userId);
        return data;
    }

    /**
     * Clears the team quota ID if it is not requested for the current page.
     *
     * @param row          The raw result row
     * @param teamQuotaIds The requested team quota IDs
     */
    private void clearUnrequestedTeamQuotaId(final Map<String, Object> row, final Collection<Long> teamQuotaIds) {
        final Object teamQuotaId = row.get(QuotaListFields.TEAM_QUOTA_ID);

        if (Objects.nonNull(teamQuotaId) && !teamQuotaIds.contains(teamQuotaId)) {
            row.put(QuotaListFields.TEAM_QUOTA_ID, null);
        }
    }

    /**
     * <p>
     * Filters by exact child table IDs to prevent cross-page data contamination.
     * </p>
     *
     * @param quotaIdsHolder Represents the {@link QuotaIdsHolder} containing paginated IDs
     * @return The combined {@link WhereClause} or null if all ID collections are empty
     */
    private WhereClause buildCriteriaForUserAndTeam(final QuotaIdsHolder quotaIdsHolder) {
        final Collection<Long> teamQuotaIds = quotaIdsHolder.teamQuotaIds();
        final Collection<Long> userQuotaIds = quotaIdsHolder.userQuotaIds();

        WhereClause userAndTeamCriteria = null;

        if (!teamQuotaIds.isEmpty()) {
            userAndTeamCriteria = new WhereClause(Criteria.in(getTeamQuotaTable().getPKColumn(), teamQuotaIds));
        }

        if (!userQuotaIds.isEmpty()) {
            final WhereClause userQuotaCriteria = new WhereClause(Criteria.in(getUserQuotaTable().getPKColumn(), userQuotaIds));

            if (Objects.nonNull(userAndTeamCriteria)) {
                userAndTeamCriteria = userAndTeamCriteria.or(userQuotaCriteria);
            } else {
                userAndTeamCriteria = userQuotaCriteria;
            }
        }
        return userAndTeamCriteria;
    }

    /**
     * <p>
     * Replaces the standard teamUserQuota LEFT JOIN with a derived table (subquery) that
     * filters by the paginated teamUserQuotaIds.
     * </p>
     *
     * @param teamUserQuotaIds The collection of team user quota IDs to include in the derived table
     */
    private void replaceTeamUserQuotaJoinWithDerivedTable(final Collection<Long> teamUserQuotaIds) {
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final JoinClause existingJoinClause = findJoinClauseByRemoteTable(teamUserQuotaTable);

        if (Objects.isNull(existingJoinClause)) {
            return;
        }
        final DerivedTable derivedTable = createDerivedTable(teamUserQuotaTable, teamUserQuotaIds);
        final JoinClause derivedJoinClause = new JoinClause(teamQuotaTable.getPKColumn(),
                derivedTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), JoinClause.JoinType.LEFT);

        selectQuery.replaceJoinClause(existingJoinClause, derivedJoinClause);
    }

    /**
     * <p>
     * Finds the existing {@link JoinClause} in the selectQuery whose remote table matches the given table.
     * </p>
     *
     * @param remoteTable The remote table to search for
     * @return The matching {@link JoinClause}, or null if not found
     */
    private JoinClause findJoinClauseByRemoteTable(final Table remoteTable) {
        for (final JoinClause joinClause : selectQuery.getJoinClause()) {

            if (Objects.equals(joinClause.getRemoteTable(), remoteTable)) {
                return joinClause;
            }
        }
        return null;
    }

    /**
     * <p>
     * Creates a derived table filtered by the given IDs. If the IDs collection is empty,
     * creates a derived table with a NULL PK condition so no rows are returned.
     * </p>
     *
     * @param table The base table to create a derived version of
     * @param ids   The collection of IDs to filter by; if empty, no rows will be returned
     * @return A {@link DerivedTable} filtered by the given IDs
     */
    private DerivedTable createDerivedTable(final Table table, final Collection<Long> ids) {
        final SelectQuery derivedSelectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();

        derivedSelectQuery.addSelectClause(table);
        derivedSelectQuery.setWhereClause(new WhereClause(ids.isEmpty() ? Criteria.eq(pkColumn, null)
                : Criteria.in(pkColumn, ids)));

        return DerivedTable.get(table.getName(), table.getAlias(), derivedSelectQuery);
    }

    /**
     * <p>
     * Builds combined WHERE clause criteria when only user criteria is present.
     * Filters by exact child table IDs to prevent cross-page data contamination.
     * </p>
     *
     * @param quotaIdsHolder Represents the {@link QuotaIdsHolder} containing paginated IDs
     * @return The combined {@link WhereClause} or null if all ID collections are empty
     */
    private WhereClause buildCriteriaForUser(final QuotaIdsHolder quotaIdsHolder) {
        final WhereClauseBuilder whereClauseBuilder = new WhereClauseBuilder();
        final Collection<Long> userQuotaIds = quotaIdsHolder.userQuotaIds();
        final Collection<Long> teamUserQuotaIds = quotaIdsHolder.teamUserQuotaIds();
        final Collection<Long> teamQuotaIds = quotaIdsHolder.teamQuotaIds();

        if (!userQuotaIds.isEmpty()) {
            whereClauseBuilder.append(new WhereClause(Criteria.in(getUserQuotaTable().getPKColumn(), userQuotaIds)), Connector.OR);
        }

        if (!teamUserQuotaIds.isEmpty()) {
            whereClauseBuilder.append(new WhereClause(Criteria.in(getTeamUserQuotaTable().getPKColumn(), teamUserQuotaIds)), Connector.OR);
        }

        if (!teamQuotaIds.isEmpty()) {
            whereClauseBuilder.append(new WhereClause(Criteria.in(getTeamQuotaTable().getPKColumn(), teamQuotaIds)), Connector.OR);
        }
        return whereClauseBuilder.getWhereClause();
    }

    /**
     * <p>
     * Builds combined WHERE clause criteria when only team criteria is present.
     * Filters by exact team quota IDs to return only team-level quota records.
     * </p>
     *
     * @param quotaIdsHolder Represents the {@link QuotaIdsHolder} containing paginated IDs
     * @return The combined {@link WhereClause} or null if team quota IDs collection is empty
     */
    private WhereClause buildCriteriaForTeam(final QuotaIdsHolder quotaIdsHolder) {
        final Collection<Long> teamQuotaIds = quotaIdsHolder.teamQuotaIds();

        if (!teamQuotaIds.isEmpty()) {
            return new WhereClause(Criteria.in(getTeamQuotaTable().getPKColumn(), teamQuotaIds));
        }
        return null;
    }

    /**
     * <p>
     * Loads the necessary JOIN clauses onto the given {@link SelectQuery} based on the current criteria flags.
     * </p>
     *
     * @param selectQuery The {@link SelectQuery} to which JOIN clauses will be added
     */
    private void loadBaseJoins(final SelectQuery selectQuery) {
        final Column quotaPKColumn = primaryTable.getPKColumn();
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table userQuotaTable = getUserQuotaTable();
        final DefaultJoinLoader defaultJoinLoader = new DefaultJoinLoader(selectQuery, JoinClause.JoinType.LEFT);

        if (hasUserCriteria) {
            defaultJoinLoader.load(quotaPKColumn, userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID));
            defaultJoinLoader.load(quotaPKColumn, teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID));
            defaultJoinLoader.load(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID));
        }

        if (hasTeamCriteria && !hasUserCriteria) {
            defaultJoinLoader.load(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), getTable().getPKColumn());
        }
    }

    /**
     * <p>
     * Fetches {@link QuotaIdsHolder} from team quota, user quota, and team user quota tables.
     * The pagination is applied to the combined set of child table records.
     * </p>
     *
     * @param start The starting index (1-based) for pagination.
     * @param limit The maximum number of records to return.
     * @return A {@link QuotaIdsHolder} containing the IDs for each table within the requested page.
     */
    private QuotaIdsHolder getQuotaIdsHolder(final int start, final int limit) {
        final long totalRecordFetchTime = System.currentTimeMillis();
        final ReadableDataStore orgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(primaryTable);
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table userQuotaTable = getUserQuotaTable();

        loadBaseJoins(selectQuery);
        selectQuery.setWhereClause(derivedQuery.getWhereClause());
        applyOrderByClause(selectQuery);
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final Collection<Long> teamQuotaIds = new LinkedHashSet<>();
        final Collection<Long> userQuotaIds = new LinkedHashSet<>();
        final Collection<Long> teamUserQuotaIds = new LinkedHashSet<>();
        final List<QuotaRowIdEntry> quotaEntries = new ArrayList<>();

        if (hasTeamCriteria) {
            addTeamQuotaRecords(dataContainer, teamQuotaTable, quotaEntries, collectIdsFromJson(QuotaListFields.TEAM_IDS));
        } else if (hasUserCriteria) {
            addTeamQuotaRecords(dataContainer, teamQuotaTable, quotaEntries, null);
        }

        if (hasUserCriteria) {
            addUserQuotaRecords(dataContainer, userQuotaTable, quotaEntries);
            addTeamUserQuotaRecords(dataContainer, teamUserQuotaTable, teamQuotaTable, quotaEntries,
                    collectIdsFromJson(QuotaListFields.USER_IDS));
        }
        quotaEntries.sort(Comparator.comparingDouble((QuotaRowIdEntry entry) -> getWonPercentage(entry, dataContainer)).reversed());
        final Collection<QuotaRowIdEntry> paginatedRecordEntries = quotaEntries.stream().skip(Math.max(0, start - 1))
                .limit(limit).toList();

        for (final QuotaRowIdEntry quotaRowIdEntry : paginatedRecordEntries) {
            final Long id = quotaRowIdEntry.id();

            if (Objects.nonNull(id)) {
                switch (quotaRowIdEntry.quotaScope()) {
                    case TEAM_QUOTA -> teamQuotaIds.add(id);
                    case USER_QUOTA -> userQuotaIds.add(id);
                    case TEAM_USER_QUOTA -> teamUserQuotaIds.add(id);
                }
            }
        }

        LOGGER.debug("Quota team view list action - [{}]: Fetched {} records (paginated from {}) in {}ms, Org ID: {}, User ID: {}",
                provider, paginatedRecordEntries.size(), quotaEntries.size(), System.currentTimeMillis() - totalRecordFetchTime, orgId, userId);

        return new QuotaIdsHolder(teamQuotaIds, userQuotaIds, teamUserQuotaIds);
    }

    /**
     * <p>
     * Applies ORDER BY clause to the given select query based on the current filter context.
     * When both hasUserCriteria and hasTeamCriteria are true, applies ORDER BY on all relevant tables.
     * </p>
     *
     * @param selectQuery The {@link SelectQuery} to which ORDER BY clauses will be added.
     */
    private void applyOrderByClause(final SelectQuery selectQuery) {
        if (hasUserCriteria && hasTeamCriteria) {
            selectQuery.addOrderByClause(new OrderByClause(getTeamQuotaTable().getColumn(OMQFTEAMQUOTA.WON), OrderByClause.OrderType.DESCENDING));
            selectQuery.addOrderByClause(new OrderByClause(getTeamUserQuotaTable().getColumn(OMQFTEAMUSERQUOTA.WON), OrderByClause.OrderType.DESCENDING));
            selectQuery.addOrderByClause(new OrderByClause(getUserQuotaTable().getColumn(OMQFUSERQUOTA.WON), OrderByClause.OrderType.DESCENDING));
        } else if (hasTeamCriteria) {
            selectQuery.addOrderByClause(new OrderByClause(getTeamQuotaTable().getColumn(OMQFTEAMQUOTA.WON), OrderByClause.OrderType.DESCENDING));
        } else if (hasUserCriteria) {
            selectQuery.addOrderByClause(new OrderByClause(getUserQuotaTable().getColumn(OMQFUSERQUOTA.WON), OrderByClause.OrderType.DESCENDING));
        }
    }

    /**
     * <p>
     * Adds SELECT clauses required for team-level quota queries
     * </p>
     *
     * @param selectQuery the {@link SelectQuery} to which team-related select
     *                    clauses will be added
     */
    private void addTeamUserAndTeamSelect(final SelectQuery selectQuery) {
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamTable = Table.get(OMCRMTEAM.TABLE);

        selectQuery.addSelectClause(List.of(teamTable.getColumn(OMCRMTEAM.NAME, getAliasName(QuotaListFields.USER_AND_TEAM,
                        QuotaListFields.TEAM_QUOTA)),
                teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID, QuotaListFields.TEAM_QUOTA_TEAM_ID),
                teamTable.getColumn(OMCRMTEAM.NAME, QuotaListFields.TEAM_QUOTA_TEAM_NAME)));
    }

    /**
     * <p>
     * Adds all team quota records to the allRecords list without filtering.
     * Used when only team criteria is present (team-only view).
     * </p>
     *
     * @param dataContainer     The {@link DataContainer} containing the query results
     * @param teamQuotaTable    The team quota {@link Table}
     * @param quotaRowIdEntries The list to add records  {@link QuotaRowIdEntry}
     * @param selectedTeamIds   The selected team IDs, or null when the team filter is not applied
     */
    private void addTeamQuotaRecords(final DataContainer dataContainer, final Table teamQuotaTable,
                                     final Collection<QuotaRowIdEntry> quotaRowIdEntries, final Collection<Long> selectedTeamIds) {
        dataContainer.getRows(teamQuotaTable).forEach(row -> {
            final Long teamId = row.get(OMQFTEAMQUOTA.TEAM_ID);
            final boolean isTeamSelected = Objects.isNull(selectedTeamIds) ? Objects.isNull(teamId) : selectedTeamIds.contains(teamId);

            if (isTeamSelected) {
                final Long quotaId = row.get(OMQFTEAMQUOTA.QUOTA_ID);
                final Long teamQuotaId = row.getPKValue();

                quotaRowIdEntries.add(new QuotaRowIdEntry(quotaId, teamQuotaId, QuotaScope.TEAM_QUOTA));
            }
        });
    }

    /**
     * <p>
     * Adds user quota records to the allRecords list.
     * </p>
     *
     * @param dataContainer  The {@link DataContainer} containing the query results
     * @param userQuotaTable The user quota {@link Table}
     * @param allRecords     The list to add records to
     */
    private void addUserQuotaRecords(final DataContainer dataContainer, final Table userQuotaTable,
                                     final Collection<QuotaRowIdEntry> allRecords) {
        dataContainer.getRows(userQuotaTable).forEach(row -> {
            final Long userQuotaId = row.getPKValue();
            final Long quotaId = row.get(OMQFUSERQUOTA.QUOTA_ID);

            allRecords.add(new QuotaRowIdEntry(quotaId, userQuotaId, QuotaScope.USER_QUOTA));
        });
    }

    /**
     * <p>
     * Adds team user quota records to the allRecords list, filtering by the provided user IDs.
     * </p>
     *
     * @param dataContainer      The {@link DataContainer} containing the query results
     * @param teamUserQuotaTable The team user quota {@link Table}
     * @param teamQuotaTable     The team quota {@link Table}
     * @param allRecords         The collection to add records to
     * @param userIds            The collection of user IDs to filter by, or null to include all
     */
    private void addTeamUserQuotaRecords(final DataContainer dataContainer, final Table teamUserQuotaTable,
                                         final Table teamQuotaTable, final Collection<QuotaRowIdEntry> allRecords,
                                         final Collection<Long> userIds) {
        final boolean includeAll = Objects.isNull(userIds);

        dataContainer.getRows(teamUserQuotaTable).forEach(row -> {
            final Long userId = row.get(OMQFTEAMUSERQUOTA.USER_ID);

            if (includeAll || userIds.contains(userId)){
                final Long teamUserQuotaId = row.getPKValue();
                final Long teamQuotaId = row.get(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID);
                final Row teamQuotaRow = dataContainer.getRow(teamQuotaTable, teamQuotaId);
                final Long quotaId = Objects.nonNull(teamQuotaRow) ? teamQuotaRow.get(OMQFTEAMQUOTA.QUOTA_ID) : null;

                allRecords.add(new QuotaRowIdEntry(quotaId, teamUserQuotaId, QuotaScope.TEAM_USER_QUOTA));
            }
        });
    }

    /**
     * Represents the scope of a quota record, indicating whether it applies to
     * a team, a user, or a user within a team context.
     *
     * @author Kishore J
     * @version 1.0
     */
    private enum QuotaScope {
        TEAM_QUOTA,
        USER_QUOTA,
        TEAM_USER_QUOTA
    }

    /**
     * Holds identifiers for a single quota record, including the base quota ID,
     * the specific record ID, and its scope type.
     *
     * @param quotaId    The base quota identifier
     * @param id         The specific record identifier (team quota, user quota, or team user quota ID)
     * @param quotaScope The scope type of the quota record
     * @author Kishore J
     * @version 1.0
     */
    private record QuotaRowIdEntry(Long quotaId, Long id, QuotaScope quotaScope) {
    }

    /**
     * Holder for paginated quota IDs from different quota tables.
     * Used to hold the IDs within a requested page for efficient data retrieval.
     *
     * @param teamQuotaIds     Collection of team quota IDs for the current page
     * @param userQuotaIds     Collection of user quota IDs for the current page
     * @param teamUserQuotaIds Collection of team user quota IDs for the current page
     * @author Kishore J
     * @version 1.0
     */
    private record QuotaIdsHolder(Collection<Long> teamQuotaIds, Collection<Long> userQuotaIds,
                                  Collection<Long> teamUserQuotaIds) {

        private boolean isEmpty() {
            return teamQuotaIds.isEmpty() && userQuotaIds.isEmpty() && teamUserQuotaIds.isEmpty();
        }
    }

    /**
     * <p>
     * Computes the won percentage ((won / target) * 100) for a quota row entry,
     * used to sort quota entries by achievement before pagination.
     * </p>
     *
     * @param entry         Represents the {@link QuotaRowIdEntry} whose won percentage is computed
     * @param dataContainer The {@link DataContainer} holding the fetched quota rows
     * @return The won percentage, or 0 if the quota/scope row is missing or the target is not positive
     */
    private double getWonPercentage(final QuotaRowIdEntry entry, final DataContainer dataContainer) {
        final Row quotaRow = dataContainer.getRow(getTable(), entry.quotaId());

        if (Objects.isNull(quotaRow)) {
            return NumericalConstant.ZERO;
        }

        final double target = quotaRow.get(OMQFQUOTA.TARGET);

        if (target <= NumericalConstant.ZERO) {
            return NumericalConstant.ZERO;
        }

        final double won = switch (entry.quotaScope()) {
            case TEAM_QUOTA -> {
                final Row row = dataContainer.getRow(getTeamQuotaTable(), entry.id());

                yield Objects.isNull(row) ? NumericalConstant.ZERO : row.get(OMQFTEAMQUOTA.WON);
            }
            case USER_QUOTA -> {
                final Row row = dataContainer.getRow(getUserQuotaTable(), entry.id());

                yield Objects.isNull(row) ? NumericalConstant.ZERO : row.get(OMQFUSERQUOTA.WON);
            }
            case TEAM_USER_QUOTA -> {
                final Row row = dataContainer.getRow(getTeamUserQuotaTable(), entry.id());

                yield Objects.isNull(row) ? NumericalConstant.ZERO : row.get(OMQFTEAMUSERQUOTA.WON);
            }
        };

        return (won * 100) / target;
    }
}

