package com.tlc.crm.core.features.action.quota.list;

import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.list.transformer.model.ListResponseStructureType;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.entity.list.filter.loader.impl.DefaultJoinLoader;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Process the hierarchy view list actions in the quota and Provides the implementation for the {@link QuotaSQLListInterpreter}.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class QuotaHierarchyViewSQLListInterpreter extends QuotaSQLListInterpreter {

    private final boolean hasUserList;
    private Collection<Long> userIds;

    protected QuotaHierarchyViewSQLListInterpreter(final ListRequestData listRequestData, final Provider provider) {
        super(listRequestData, provider);
        this.hasUserList = Objects.nonNull(criteriaJson) && criteriaJson.containsKey(QuotaListFields.VIEW_TYPE) && Objects.equals(ListResponseStructureType.get(criteriaJson.getString(QuotaListFields.VIEW_TYPE)),
                ListResponseStructureType.NESTED);
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamTable = Table.get(OMCRMTEAM.TABLE);
        final DefaultJoinLoader defaultJoinLoader = new DefaultJoinLoader(selectQuery, JoinClause.JoinType.LEFT);

        loadBaseSelectClause();
        selectQuery.addSelectClause(List.of(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.ID, QuotaListFields.TEAM_USER_QUOTA_ID),
                userQuotaTable.getColumn(OMQFUSERQUOTA.ID, QuotaListFields.USER_QUOTA_ID)));

        loadBaseJoins(selectQuery);
        defaultJoinLoader.load(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), teamTable.getPKColumn());
        loadUserTeamSelectAndJoin(teamUserQuotaTable, teamQuotaTable);

        if (criteriaJson.containsKey(QuotaListFields.USER_IDS)) {
            userIds = collectIdsFromJson(QuotaListFields.USER_IDS);
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
        final SelectQuery selectQuery = buildBaseSelectQuery();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table userQuotaTable = getUserQuotaTable();

        if (hasUserList) {
            final Collection<Long> userQuotaIds = new HashSet<>();
            final Collection<Long> teamUserQuotaIds = new HashSet<>();

            selectQuery.addSelectClause(List.of(userQuotaTable.getPKColumn(), teamUserQuotaTable.getPKColumn()));
            final DataContainer dataContainer = readableDataStore.get(selectQuery);

            dataContainer.getRows(userQuotaTable).forEach(row -> userQuotaIds.add(row.getPKValue()));
            dataContainer.getRows(teamUserQuotaTable).forEach(row -> teamUserQuotaIds.add(row.getPKValue()));

            return userQuotaIds.size() + teamUserQuotaIds.size();
        } else {
            final Collection<Long> userIds = new HashSet<>();

            selectQuery.addSelectClause(List.of(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID)));
            final DataContainer dataContainer = readableDataStore.get(selectQuery);

            dataContainer.getRows(userQuotaTable).forEach(row -> userIds.add(row.get(OMQFUSERQUOTA.USER_ID)));
            dataContainer.getRows(teamUserQuotaTable).forEach(row -> userIds.add(row.get(OMQFTEAMUSERQUOTA.USER_ID)));

            return userIds.size();
        }
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
                    final DefaultJoinLoader defaultJoinLoader = new DefaultJoinLoader(selectQuery, JoinClause.JoinType.LEFT);

                    loadUserQuota(fieldConfig, defaultJoinLoader);
                } else {
                    switch (fieldName) {
                        case QuotaListFields.USER -> addUserSelect(selectQuery, fieldName);
                        case QuotaListFields.QUOTA ->
                                selectQuery.addSelectClause(List.of(primaryTable.getColumn(OMQFQUOTA.TARGET, getAliasName(fieldName, QuotaListFields.USER_QUOTA)),
                                        teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TARGET, getAliasName(fieldName, QuotaListFields.TEAM_USER_QUOTA))));
                        case QuotaListFields.WON -> {
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

    /**
     * {@inheritDoc}
     *
     * @return The collection of maps containing the mapped data
     */
    @Override
    public Collection<Map<String, Object>> getResult() {
        final long listDataFetchStartTime = System.currentTimeMillis();
        final LimitClause limitClause = derivedQuery.getLimitClause();
        final WhereClause whereClause = derivedQuery.getWhereClause();
        final QuotaIdsHolder quotaIdsHolder;

        if (hasUserList) {
            quotaIdsHolder = getQuotaIdsHolder(whereClause, limitClause);
        } else {

            if (Objects.isNull(userIds) || userIds.isEmpty()) {
                return Collections.emptyList();
            }
            final int start = limitClause.getStart();
            final int limit = limitClause.getNumberOfResults();
            final List<Long> paginatedUserIds = userIds.stream().skip(Math.max(0, start - 1)).limit(limit).toList();

            quotaIdsHolder = getQuotaIdsHolderByUserIds(whereClause, paginatedUserIds);
        }

        if (quotaIdsHolder.isEmpty()) {
            return Collections.emptyList();
        }
        final WhereClause idsCriteria = buildIdsCriteria(quotaIdsHolder);

        if (Objects.isNull(idsCriteria)) {
            return Collections.emptyList();
        }
        selectQuery.setWhereClause(idsCriteria);
        LOGGER.debug("Quota hierarchy view list action - [{}]: Going to fetch the data from the database, Org ID: {}, User ID: {}", provider, orgId, userId);
        final List<Map<String, Object>> data = getReadableDataStore(orgId).getData(selectQuery);

        LOGGER.debug("Quota hierarchy view list action - [{}]: Data are fetched from the database in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - listDataFetchStartTime, orgId, userId);
        return data;
    }

    /**
     * <p>
     * Fetches the user quota and team user quota IDs by applying the given WHERE clause
     * and LIMIT clause to narrow down the results. Collects IDs from both user quota
     * and team user quota tables.
     * </p>
     *
     * @param whereClause The {@link WhereClause} to filter records
     * @param limitClause The {@link LimitClause} to apply pagination
     * @return A {@link QuotaIdsHolder} containing the user quota and team user quota IDs
     */
    private QuotaIdsHolder getQuotaIdsHolder(final WhereClause whereClause, final LimitClause limitClause) {
        final ReadableDataStore readableDataStore = getReadableDataStore(orgId);
        final Table userQuotaTable = getUserQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final SelectQuery quotaIdQuery = SelectQuery.get(primaryTable);

        loadBaseJoins(quotaIdQuery);
        quotaIdQuery.setWhereClause(whereClause);
        quotaIdQuery.setLimitClause(limitClause);
        quotaIdQuery.addSelectClause(List.of(userQuotaTable.getPKColumn(), teamUserQuotaTable.getPKColumn()));

        final DataContainer dataContainer = readableDataStore.get(quotaIdQuery);
        final Collection<Long> userQuotaIds = new HashSet<>();
        final Collection<Long> teamUserQuotaIds = new HashSet<>();

        dataContainer.getRows(userQuotaTable).forEach(row -> userQuotaIds.add(row.getPKValue()));
        dataContainer.getRows(teamUserQuotaTable).forEach(row -> teamUserQuotaIds.add(row.getPKValue()));

        return new QuotaIdsHolder(userQuotaIds, teamUserQuotaIds);
    }

    /**
     * <p>
     * Fetches the user quota and team user quota IDs for the given paginated user IDs.
     * Applies the WHERE clause along with user ID filtering to collect relevant quota IDs.
     * </p>
     *
     * @param whereClause      The {@link WhereClause} to filter records
     * @param paginatedUserIds The paginated collection of user IDs to filter by
     * @return A {@link QuotaIdsHolder} containing the user quota and team user quota IDs
     */
    private QuotaIdsHolder getQuotaIdsHolderByUserIds(final WhereClause whereClause, final Collection<Long> paginatedUserIds) {
        final ReadableDataStore readableDataStore = getReadableDataStore(orgId);
        final Table userQuotaTable = getUserQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final SelectQuery quotaIdQuery = SelectQuery.get(primaryTable);

        loadBaseJoins(quotaIdQuery);

        final WhereClause userQuotaCriteria = new WhereClause(Criteria.in(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID), paginatedUserIds));
        final WhereClause teamUserQuotaCriteria = new WhereClause(Criteria.in(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), paginatedUserIds));
        final WhereClause userFilterCriteria = WhereClauseUtil.connect(userQuotaCriteria, teamUserQuotaCriteria, Connector.OR);

        quotaIdQuery.setWhereClause(WhereClauseUtil.connect(whereClause, userFilterCriteria, Connector.AND));
        quotaIdQuery.addSelectClause(List.of(userQuotaTable.getPKColumn(), teamUserQuotaTable.getPKColumn()));

        final DataContainer dataContainer = readableDataStore.get(quotaIdQuery);
        final Collection<Long> userQuotaIds = new HashSet<>();
        final Collection<Long> teamUserQuotaIds = new HashSet<>();

        dataContainer.getRows(userQuotaTable).forEach(row -> userQuotaIds.add(row.getPKValue()));
        dataContainer.getRows(teamUserQuotaTable).forEach(row -> teamUserQuotaIds.add(row.getPKValue()));

        return new QuotaIdsHolder(userQuotaIds, teamUserQuotaIds);
    }

    /**
     * <p>
     * Builds combined WHERE clause criteria from the quota IDs holder.
     * Filters by exact user quota and team user quota IDs.
     * </p>
     *
     * @param quotaIdsHolder Represents the {@link QuotaIdsHolder} containing the IDs
     * @return The combined {@link WhereClause} or null if all ID collections are empty
     */
    private WhereClause buildIdsCriteria(final QuotaIdsHolder quotaIdsHolder) {
        final Collection<Long> userQuotaIds = quotaIdsHolder.userQuotaIds();
        final Collection<Long> teamUserQuotaIds = quotaIdsHolder.teamUserQuotaIds();
        WhereClause whereClause = null;

        if (!userQuotaIds.isEmpty()) {
            whereClause = new WhereClause(Criteria.in(getUserQuotaTable().getPKColumn(), userQuotaIds));
        }

        if (!teamUserQuotaIds.isEmpty()) {
            final WhereClause teamUserQuotaCriteria = new WhereClause(Criteria.in(getTeamUserQuotaTable().getPKColumn(), teamUserQuotaIds));
            whereClause = WhereClauseUtil.connect(whereClause, teamUserQuotaCriteria, Connector.OR);
        }
        return whereClause;
    }

    /**
     * Holder for paginated quota IDs from user quota and team user quota tables.
     *
     * @param userQuotaIds     Collection of user quota IDs
     * @param teamUserQuotaIds Collection of team user quota IDs
     */
    private record QuotaIdsHolder(Collection<Long> userQuotaIds, Collection<Long> teamUserQuotaIds) {

        private boolean isEmpty() {
            return userQuotaIds.isEmpty() && teamUserQuotaIds.isEmpty();
        }
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

        defaultJoinLoader.load(quotaPKColumn, userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID));
        defaultJoinLoader.load(quotaPKColumn, teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID));
        defaultJoinLoader.load(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID));
    }
}
