package com.tlc.crm.core.features.api.quota.manager;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.manager.internal.TeamQuotaManager;
import com.tlc.crm.core.features.api.quota.manager.internal.UserQuotaManager;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.Quota.RelatedEntityType;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.QuotaPeriod;
import com.tlc.crm.core.features.api.quota.model.QuotaType;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.features.api.quota.model.UserQuota;
import com.tlc.crm.core.sql.resource.OMQFPIPELINEQUOTA;
import com.tlc.crm.core.sql.resource.OMQFPRODUCTQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTATERRITORY;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manager contract for handling {@link QuotaHolder} related DB actions
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaHolderManagerImpl extends AbstractConfigManager<QuotaHolder> implements QuotaHolderManager {

    private final TeamQuotaManager teamQuotaManager;
    private final UserQuotaManager userQuotaManager;

    private QuotaHolderManagerImpl() {
        teamQuotaManager = TeamQuotaManager.getInstance();
        userQuotaManager = UserQuotaManager.getInstance();
    }

    private static class Instance {
        private static final QuotaHolderManagerImpl INSTANCE = new QuotaHolderManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link QuotaHolderManagerImpl}
     * </p>
     *
     * @return The {@link QuotaHolderManagerImpl}
     */
    public static QuotaHolderManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The quota table
     */
    @Override
    public Table getTable() {
        return Table.get(OMQFQUOTA.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation id.
     * @param quotaHolders Holds the details of the entities.
     * @return The {@link ProcessOutcome}
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<QuotaHolder> quotaHolders) {
        final Iterator<QuotaHolder> iterator = quotaHolders.iterator();
        final QuotaHolder quotaHolder = iterator.next();

        if (quotaHolder.hasTeamQuota()) {
            final Collection<TeamQuota> teamQuotas = new ArrayList<>(quotaHolders.size());

            teamQuotas.add(quotaHolder.getTeamQuota());

            while (iterator.hasNext()) {
                teamQuotas.add(iterator.next().getTeamQuota());
            }
            return teamQuotaManager.update(orgId, teamQuotas);
        } else {
            final Collection<UserQuota> userQuotas = new ArrayList<>(quotaHolders.size());

            userQuotas.add(quotaHolder.getUserQuota());

            while (iterator.hasNext()) {
                userQuotas.add(iterator.next().getUserQuota());
            }
            return userQuotaManager.update(orgId, userQuotas);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains collection of quota ids
     * @return The collection of {@link QuotaHolder}
     */
    @Override
    public Collection<QuotaHolder> get(final Long orgId, final Collection<Long> ids) {
        final QuotaType quotaType = getQuotaType(orgId, ids.iterator().next());
        final Collection<QuotaHolder> quotaHolders = new ArrayList<>(ids.size());

        if (quotaType.isTeamQuota()) {
            final Collection<TeamQuota> teamQuotas = teamQuotaManager.get(orgId, ids);

            for (final TeamQuota teamQuota : teamQuotas) {
                final QuotaHolder quotaHolder = new QuotaHolder();

                quotaHolder.setOrgId(orgId);
                quotaHolder.setId(teamQuota.id());
                quotaHolder.setTeamQuota(teamQuota);
                quotaHolders.add(quotaHolder);
            }
        } else {
            final Collection<UserQuota> userQuotas = userQuotaManager.get(orgId, ids);

            for (final UserQuota userQuota : userQuotas) {
                final QuotaHolder quotaHolder = new QuotaHolder();

                quotaHolder.setOrgId(orgId);
                quotaHolder.setId(userQuota.id());
                quotaHolder.setUserQuota(userQuota);
                quotaHolders.add(quotaHolder);
            }
        }
        return quotaHolders;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of ids
     * @return The collection of {@link QuotaHolder}
     */
    @Override
    public Collection<QuotaHolder> partialGet(final Long orgId, final Collection<Long> ids) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param quotaHolder Represents the {@link QuotaHolder}
     * @return The {@link QuotaHolder}
     */
    @Override
    public QuotaHolder create(final QuotaHolder quotaHolder) {
        final Long orgId;

        if (quotaHolder.hasTeamQuota()) {
            final TeamQuota teamQuota = quotaHolder.getTeamQuota();

            teamQuotaManager.create(teamQuota);
            quotaHolder.setId(teamQuota.id());
            orgId = teamQuota.orgId();
        } else {
            final UserQuota userQuota = quotaHolder.getUserQuota();

            userQuotaManager.create(userQuota);
            quotaHolder.setId(userQuota.id());
            orgId = userQuota.orgId();
        }
        quotaHolder.setOrgId(orgId);
        return quotaHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id
     * @param quotaId Represents the quota id
     * @return The {@link QuotaMetric}
     */
    @Override
    public QuotaMetric getQuotaMetric(final long orgId, final long quotaId) {
        final Table quotaTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(quotaTable);

        selectQuery.addSelectClause(quotaTable.getColumn(OMQFQUOTA.METRIC));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(quotaTable.getPKColumn(), quotaId)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(quotaTable);

        return QuotaMetric.getById(row.get(OMQFQUOTA.METRIC));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id
     * @param quotaId Represents the quota id
     * @return The {@link QuotaPeriod}, or null when no quota exists for the given id
     */
    @Override
    public QuotaPeriod getQuotaPeriod(final long orgId, final long quotaId) {
        final Table quotaTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(quotaTable);

        selectQuery.addSelectClause(List.of(quotaTable.getColumn(OMQFQUOTA.START_TIME), quotaTable.getColumn(OMQFQUOTA.END_TIME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(quotaTable.getPKColumn(), quotaId)));

        final Row row = getOrgDataStore(orgId).get(selectQuery).getRow(quotaTable);

        if (Objects.isNull(row)) {
            return null;
        }
        final DateTime startTime = row.get(OMQFQUOTA.START_TIME);
        final DateTime endTime = row.get(OMQFQUOTA.END_TIME);

        if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
            return null;
        }
        final Long startTimeStamp = startTime.getTimeStamp();
        final Long endTimeStamp = endTime.getTimeStamp();

        return Objects.isNull(startTimeStamp) || Objects.isNull(endTimeStamp) ? null : new QuotaPeriod(startTimeStamp, endTimeStamp);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param quotaId Represents the quota id
     * @param relatedEntityType Represents the {@link RelatedEntityType}
     * @return The collection of related entity ids, empty when there is no restriction
     */
    @Override
    public Collection<Long> fetchRelatedEntityIds(final long orgId, final long quotaId, final RelatedEntityType relatedEntityType) {
        final Table table;
        final Column quotaColumn;
        final Column valueColumn;

        switch (relatedEntityType) {
            case PIPELINE -> {
                table = Table.get(OMQFPIPELINEQUOTA.TABLE);
                quotaColumn = table.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID);
                valueColumn = table.getColumn(OMQFPIPELINEQUOTA.PIPELINE_ID);
            }
            case TERRITORY -> {
                table = Table.get(OMQFQUOTATERRITORY.TABLE);
                quotaColumn = table.getColumn(OMQFQUOTATERRITORY.QUOTA_ID);
                valueColumn = table.getColumn(OMQFQUOTATERRITORY.TERRITORY_ID);
            }
            case PRODUCT -> {
                table = Table.get(OMQFPRODUCTQUOTA.TABLE);
                quotaColumn = table.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID);
                valueColumn = table.getColumn(OMQFPRODUCTQUOTA.PRODUCT_ID);
            }
            default -> throw new IllegalArgumentException("Unsupported related entity type: " + relatedEntityType);
        }
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(valueColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(quotaColumn, quotaId)));

        return getOrgDataStore(orgId).get(selectQuery).getRows(table)
                .map(row -> (Long) row.get(valueColumn))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param userIds Which contains the collection of user ids
     * @return A map where the key is the user ID and the value is the total count of assigned quotas (both direct and team) for that user
     */
    @Override
    public Map<Long, Long> getUserAssignedQuotaCount(final Long orgId, final Collection<Long> userIds) {
        final Table userQuotaTable = Table.get(OMQFUSERQUOTA.TABLE);
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = Table.get(OMQFTEAMUSERQUOTA.TABLE);
        final Column userQuotaUserIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID);
        final Column teamQuotaUserIdColumn = teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID);
        final Table table = getTable();
        final Column quotaPkColumn = table.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final SelectQuery teamUserSelectQuery = SelectQuery.get(table);
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);

        selectQuery.addSelectClause(List.of(userQuotaUserIdColumn, userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID, "userCount").count()));
        selectQuery.addJoinClause(new JoinClause(quotaPkColumn, userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(userQuotaUserIdColumn, userIds)));
        selectQuery.addGroupByClause(new GroupByClause(userQuotaUserIdColumn));
        final List<Map<String, Object>> userQuotaUserCount = orgDataStore.getData(selectQuery);

        teamUserSelectQuery.addSelectClause(List.of(teamQuotaUserIdColumn, teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID, "userCount").count()));
        teamUserSelectQuery.addJoinClause(List.of(new JoinClause(quotaPkColumn, teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID)),
                new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID))));
        teamUserSelectQuery.addGroupByClause(new GroupByClause(teamQuotaUserIdColumn));
        teamUserSelectQuery.setWhereClause(new WhereClause(Criteria.in(teamQuotaUserIdColumn, userIds))
                .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.IS_ENABLED), true)));
        final List<Map<String, Object>> teamQuotaUserCount = orgDataStore.getData(teamUserSelectQuery);
        final Map<Long, Long> resultMap = new HashMap<>();

        for (final Map<String, Object> row : userQuotaUserCount) {
            resultMap.merge((Long) row.get(OMQFUSERQUOTA.USER_ID), (Long) row.get(QuotaFields.USER_COUNT), Long::sum);
        }

        for (final Map<String, Object> row : teamQuotaUserCount) {
            resultMap.merge((Long) row.get(OMQFTEAMUSERQUOTA.USER_ID), (Long) row.get(QuotaFields.USER_COUNT), Long::sum);
        }
        userIds.forEach(userId -> resultMap.putIfAbsent(userId, 0L));

        return resultMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param quotaId Represents the quota id for which attainment needs to be recalculated
     * @param userId  Represents the user id for whom attainment needs to be recalculated; if null, recalculates for all users under the quota
     */
    @Override
    public void recalculateQuotaAttainment(final long orgId, final long quotaId, final Long userId) {
        final QuotaType quotaType = getQuotaType(orgId, quotaId);

        if (quotaType.isTeamQuota()) {
            teamQuotaManager.recalculateTeamQuotaAttainment(orgId, quotaId, userId);
        } else {
            userQuotaManager.recalculateAttainment(orgId, quotaId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   the organization ID
     * @param quotaId the quota ID
     * @param userId  the user ID (applicable for team quota checks)
     * @param time    the threshold time in milliseconds (e.g., current time minus 3 hours)
     * @return true if the quota was recalculated after the given time; otherwise false
     */
    @Override
    public boolean isQuotaRecentlyRecalculated(final long orgId, final long quotaId, final Long userId, final Long time) {
        final QuotaType quotaType = getQuotaType(orgId, quotaId);

        if (quotaType.isTeamQuota()) {
            return teamQuotaManager.isQuotaRecentlyRecalculated(orgId, quotaId, userId, time);
        } else {
            return userQuotaManager.isQuotaRecentlyRecalculated(orgId, quotaId, time);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the quota id
     * @return The {@link QuotaType}
     */
    @Override
    public QuotaType getQuotaType(final long orgId, final Long id) {
        final Table table = Table.get(OMQFQUOTA.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMQFQUOTA.TYPE));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
        final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);

        return QuotaType.getById(dataContainer.getRow(table).get(OMQFQUOTA.TYPE));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization Id
     * @param userId  Represents the user Id
     * @param quotaId Represents the quota Id
     * @return True if the current user created the quota
     */
    @Override
    public boolean isQuotaOwner(final long orgId, final long userId, final Long quotaId) {
        final Table quotaTable = getTable();
        final CountQuery countQuery = CountQuery.get(quotaTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(quotaTable.getColumn(OMQFQUOTA.ID), quotaId))
                .and(Criteria.eq(quotaTable.getColumn(OMQFQUOTA.CREATED_BY), userId)));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization Id
     * @param userId  Represents the user Id
     * @param quotaId Represents the quota Id
     * @return True if the quota belongs to the user
     */
    @Override
    public boolean isUserQuota(final long orgId, final long userId, final Long quotaId) {
        final Table userQuotaTable = userQuotaManager.getTable();
        final CountQuery countQuery = CountQuery.get(userQuotaTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID), userId)));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization Id
     * @param userTeamIds Represents the collection of user's team Ids
     * @param quotaId     Represents the quota Id
     * @return True if the quota belongs to any of the user's teams
     */
    @Override
    public boolean isTeamQuota(final long orgId, final Collection<Long> userTeamIds, final Long quotaId) {

        if (userTeamIds.isEmpty()) {
            return false;
        }
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final CountQuery countQuery = CountQuery.get(teamQuotaTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.in(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), userTeamIds)));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param quotaHolders Which contains the collection of {@link QuotaHolder}
     * @param excludeCurrentHolder  True to exclude each quota holder's own id from the duplicate search
     * @return true if a matching duplicate quota exists, otherwise false
     */
    @Override
    public boolean isQuotaHolderExists(final long orgId, final Collection<QuotaHolder> quotaHolders, final boolean excludeCurrentHolder) {
        return quotaHolders.stream().anyMatch(quotaHolder -> isDuplicateQuotaHolder(orgId, quotaHolder, excludeCurrentHolder));
    }

    /**
     * <p>
     * Determines if a given {@link QuotaHolder} is a duplicate of any existing quota based on directional containment and configuration rules.
     * It checks if there is any existing quota that fully contains the new quota's time period and has the same territory, product, and pipeline configurations.
     * If such a quota exists, it returns true indicating a duplicate; otherwise, it returns false.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param quotaHolder The new {@link QuotaHolder} being evaluated for duplication
     * @param excludeCurrentHolder True to exclude the quota's own id from the duplicate search
     * @return true if a duplicate quota exists, otherwise false
     */
    private boolean isDuplicateQuotaHolder(final long orgId, final QuotaHolder quotaHolder, final boolean excludeCurrentHolder) {
        final Quota quota;
        final Collection<Long> userIds;

        if (quotaHolder.hasTeamQuota()) {
            final TeamQuota teamQuota = quotaHolder.getTeamQuota();

            userIds = teamQuota.getTeamUserQuotas().stream()
                    .map(tq -> tq.getTeamUser().getCrmUser().id())
                    .collect(Collectors.toSet());
            quota = teamQuota;
        } else {
            final UserQuota userQuota = quotaHolder.getUserQuota();

            userIds = userQuota.getCrmUsers().stream()
                    .map(CrmUser::id)
                    .collect(Collectors.toSet());
            quota = userQuota;
        }
        final Collection<Map<String, Object>> candidates = fetchContainingQuotaCandidates(orgId, quota, userIds, excludeCurrentHolder);

        return candidates.stream().anyMatch(existing -> hasSameConfiguration(existing, quota));
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} for the specified organization ID.
     * </p>
     *
     * @param orgId Represents the organization ID for which to retrieve the data store
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Fetches existing quotas that fully contain the new quota's time period and are associated with any of the same users.
     * This pre-filtering narrows down candidates for duplicate checking based on directional containment and user association.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param quota       The new quota being evaluated for duplication
     * @param userIds     The set of user IDs associated with the new quota (from team or user quotas)
     * @param excludeCurrentHolder True to exclude each quota holder's own id from the duplicate search
     * @return A collection of maps representing existing quotas that contain the new quota's time period and share user associations, including their configurations for further comparison
     */
    private Collection<Map<String, Object>> fetchContainingQuotaCandidates(final long orgId, final Quota quota, final Collection<Long> userIds, final boolean excludeCurrentHolder) {
        final Table quotaTable = getTable();
        final Table teamQuotaTable = Table.get(OMQFTEAMQUOTA.TABLE);
        final Table teamUserQuotaTable = Table.get(OMQFTEAMUSERQUOTA.TABLE);
        final Table userQuotaTable = Table.get(OMQFUSERQUOTA.TABLE);
        final Table quotaTerritoryTable = Table.get(OMQFQUOTATERRITORY.TABLE);
        final Table quotaProductTable = Table.get(OMQFPRODUCTQUOTA.TABLE);
        final Table quotaPipelineTable = Table.get(OMQFPIPELINEQUOTA.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(quotaTable);
        final WhereClause containmentCriteria = new WhereClause(
                Criteria.le(quotaTable.getColumn(OMQFQUOTA.START_TIME), quota.getStartTime().getTimeStamp()))
                .and(Criteria.ge(quotaTable.getColumn(OMQFQUOTA.END_TIME), quota.getEndTime().getTimeStamp()));
        final WhereClause metricCriteria = WhereClauseUtil.connect(containmentCriteria,
                new WhereClause(Criteria.eq(quotaTable.getColumn(OMQFQUOTA.METRIC), quota.getQuotaMetric().getId())), Connector.AND);
        final Column teamUserQuotaUserIdColumn = teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID);
        final Column userQuotaUserIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID);
        final WhereClause userCriteria = WhereClauseUtil.connect(metricCriteria,
                new WhereClause(Criteria.in(teamUserQuotaUserIdColumn, userIds))
                        .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.IS_ENABLED), true))
                        .or(Criteria.eq(teamUserQuotaUserIdColumn, null))
                        .and(new WhereClause(Criteria.in(userQuotaUserIdColumn, userIds))
                                .or(Criteria.eq(userQuotaUserIdColumn, null))), Connector.AND);
        final Long id = excludeCurrentHolder ? quota.id() : null;
        final WhereClause finalCriteria = Objects.nonNull(id)
                ? WhereClauseUtil.connect(userCriteria, new WhereClause(Criteria.notIn(quotaTable.getPKColumn(), Collections.singleton(id))), Connector.AND) : userCriteria;

        selectQuery.addSelectClause(quotaTable, quotaTerritoryTable, quotaProductTable, quotaPipelineTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(quotaTable.getPKColumn(), teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaTable.getPKColumn(), userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaTable.getPKColumn(), quotaTerritoryTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaTable.getPKColumn(), quotaProductTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaTable.getPKColumn(), quotaPipelineTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT)));
        selectQuery.setWhereClause(finalCriteria);
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(quotaTable)
                .map(row -> buildCandidateMap(row, dataContainer, quotaTerritoryTable, quotaProductTable, quotaPipelineTable))
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * Builds a candidate map representing an existing quota's configuration for comparison against a new quota.
     * The map includes the quota ID and sets of associated territory, product, and pipeline IDs.
     * This structured representation allows for easy comparison of configurations when determining duplicates.
     * </p>
     *
     * @param quotaRow            The row representing the existing quota from the main quota table
     * @param dataContainer       The data container holding all joined data for the query
     * @param quotaTerritoryTable The table representing quota-territory associations
     * @param quotaProductTable   The table representing quota-product associations
     * @param quotaPipelineTable  The table representing quota-pipeline associations
     * @return A map containing the existing quota's ID and its associated territory, product, and pipeline ID sets
     */
    private Map<String, Object> buildCandidateMap(final Row quotaRow, final DataContainer dataContainer, final Table quotaTerritoryTable,
                                                  final Table quotaProductTable, final Table quotaPipelineTable) {
        final Long quotaId = quotaRow.getPKValue();
        final Map<String, Object> quotaConfigMap = new HashMap<>();

        quotaConfigMap.put(OMQFQUOTA.ID, quotaId);
        quotaConfigMap.put(QuotaFields.TERRITORIES, dataContainer.getRows(quotaTerritoryTable)
                .filter(row -> quotaId.equals(row.get(OMQFQUOTATERRITORY.QUOTA_ID)))
                .map(row -> (Long) row.get(OMQFQUOTATERRITORY.TERRITORY_ID))
                .collect(Collectors.toSet()));
        final WhereClause quotaProductRowCriteria = new WhereClause(Criteria.eq(quotaProductTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.eq(quotaProductTable.getColumn(OMQFPRODUCTQUOTA.VARIATION_ID), null));

        quotaConfigMap.put(QuotaFields.PRODUCTS, dataContainer.getRows(quotaProductTable, quotaProductRowCriteria)
                .map(row -> (Long) row.get(OMQFPRODUCTQUOTA.PRODUCT_ID))
                .collect(Collectors.toSet()));

        final WhereClause quotaVariationRowCriteria = new WhereClause(Criteria.eq(quotaProductTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.notEq(quotaProductTable.getColumn(OMQFPRODUCTQUOTA.VARIATION_ID), null));

        quotaConfigMap.put(QuotaFields.VARIATION_IDS, dataContainer.getRows(quotaProductTable, quotaVariationRowCriteria)
                .map(row -> (Long) row.get(OMQFPRODUCTQUOTA.VARIATION_ID))
                .collect(Collectors.toSet()));
        quotaConfigMap.put(QuotaFields.PIPELINES, dataContainer.getRows(quotaPipelineTable)
                .filter(row -> quotaId.equals(row.get(OMQFPIPELINEQUOTA.QUOTA_ID)))
                .map(row -> (Long) row.get(OMQFPIPELINEQUOTA.PIPELINE_ID))
                .collect(Collectors.toSet()));

        return quotaConfigMap;
    }

    /**
     * <p>
     * Compares the configuration of an existing quota candidate with the new quota.
     * Returns true if territory, product, and pipeline configurations are all the same (indicating a duplicate),
     * or false if any configuration differs (indicating it's not a duplicate).
     * </p>
     */
    private boolean hasSameConfiguration(final Map<String, Object> existingQuotaRow, final Quota quota) {
        final Set<Long> existingTerritoryIds = getConfigIds(existingQuotaRow, QuotaFields.TERRITORIES);
        final Set<Long> existingProductIds = getConfigIds(existingQuotaRow, QuotaFields.PRODUCTS);
        final Set<Long> existingVariationIds = getConfigIds(existingQuotaRow, QuotaFields.VARIATION_IDS);
        final Set<Long> existingPipelineIds = getConfigIds(existingQuotaRow, QuotaFields.PIPELINES);

        final Set<Long> territoryIds = extractIds(quota.getTerritories());
        final Set<Long> productsIds = extractIds(quota.getProducts());
        final Set<Long> variationIds = extractIds(quota.getVariations());
        final Set<Long> pipelineIds = extractIds(quota.getPipelines());

        return existingTerritoryIds.equals(territoryIds) && existingProductIds.equals(productsIds)
                && existingVariationIds.equals(variationIds) && existingPipelineIds.equals(pipelineIds);
    }

    /**
     * <p>
     * Utility method to safely extract a set of IDs from the candidate map for a given key.
     * If the key is missing or the value is null, it returns an empty set, which represents "all" in our logic.
     * </p>
     *
     * @param row The candidate map containing configuration sets
     * @param key The key for which to extract the set of IDs (e.g., "territories", "products", "pipelines")
     * @return A set of IDs associated with the key, or an empty set if not present
     */
    @SuppressWarnings("unchecked")
    private Set<Long> getConfigIds(final Map<String, Object> row, final String key) {
        return Optional.ofNullable((Set<Long>) row.get(key)).orElse(Collections.emptySet());
    }

    /**
     * <p>
     * Extracts a set of IDs from a collection of TlcModel objects. If the input collection is null, it returns an empty set.
     * </p>
     *
     * @param tlcModels A collection of TlcModel objects from which to extract IDs
     * @param <T>       A type that extends TlcModel
     * @return A set of IDs extracted from the TlcModel objects
     */
    private <T extends TlcModel> Set<Long> extractIds(final Collection<T> tlcModels) {
        return Optional.ofNullable(tlcModels).orElse(Collections.emptyList()).stream().map(TlcModel::id).collect(Collectors.toSet());
    }
}