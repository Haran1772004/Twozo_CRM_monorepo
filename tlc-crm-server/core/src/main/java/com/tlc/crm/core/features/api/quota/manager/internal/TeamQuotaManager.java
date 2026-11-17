package com.tlc.crm.core.features.api.quota.manager.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;

import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.features.api.quota.model.TeamUserQuota;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAWONDEAL;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manager contract for handling {@link TeamQuota}
 *
 * @author kishore J
 * @version 1.0
 */
public class TeamQuotaManager extends AbstractQuotaManager<TeamQuota> {

    private final TeamManager teamManager;
    private final DealManager dealManager;

    private TeamQuotaManager() {
        teamManager = TeamManager.getInstance();
        dealManager = DealManager.getInstance();
    }

    private static class Instance {
        private static final TeamQuotaManager INSTANCE = new TeamQuotaManager();
    }

    /**
     * <p>
     * Gets the {@link TeamQuotaManager}
     * </p>
     *
     * @return The {@link TeamQuotaManager}
     */
    public static TeamQuotaManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The team quota table
     */
    @Override
    public Table getTable() {
        return Table.get(OMQFTEAMQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the team user quota table
     * </p>
     *
     * @return The team user quota table
     */
    private Table getTeamUserQuotaTable() {
        return Table.get(OMQFTEAMUSERQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the team user quota deal table
     * </p>
     *
     * @return the team user quota deal table
     */
    private Table getTeamUserQuotaDealTable() {
        return Table.get(OMQFTEAMUSERQUOTAWONDEAL.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param teamQuotas Which contains the collection {@link TeamQuota}
     * @return The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<TeamQuota> teamQuotas) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Set<Long> ids = teamQuotas.stream().map(Quota::id).collect(Collectors.toSet());
        final DataContainer sourceContainer = getDataContainer(orgId, ids);
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);

        for (final TeamQuota teamQuota : teamQuotas) {
            final Long id = teamQuota.id();
            final DataContainer targetContainer = DataContainer.create();

            loadQuotaIntoContainer(false, teamQuota, targetContainer, sourceContainer);
            loadTeamQuotaIntoContainer(false, teamQuota, targetContainer, sourceContainer);

            try {
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(id);
            } catch (final ErrorCode errorCode) {
                failedEntities.add(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_FAILED, id.toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains collection of quota ids
     * @return The collection of {@link TeamQuota}
     */
    @Override
    public Collection<TeamQuota> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = getDataContainer(orgId, ids);
        final Table quotaTable = getQuotaTable();
        final Table teamQuotaTable = getTable();
        final Column quotaIdColumn = teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID);

        return dataContainer.getRows(quotaTable).map(row -> {
            final TeamQuota teamQuota = new TeamQuota();

            loadQuotaFromRow(row, dataContainer, teamQuota);
            final Row teamQuotaRow = dataContainer.getRow(teamQuotaTable,
                    new WhereClause(Criteria.eq(quotaIdColumn, row.getPKValue())));

            if (Objects.nonNull(teamQuotaRow)) {
                loadRowToTeamUserQuota(teamQuotaRow, dataContainer, teamQuota);
            }
            return teamQuota;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains collection of quota ids
     * @return The collection of {@link TeamQuota}
     */
    @Override
    public Collection<TeamQuota> partialGet(final Long orgId, final Collection<Long> ids) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param teamQuota Represents the {@link TeamQuota}
     * @return The {@link Quota}
     */
    @Override
    public TeamQuota create(final TeamQuota teamQuota) {
        final DataContainer targetContainer = DataContainer.create();

        loadQuotaIntoContainer(true, teamQuota, targetContainer, null);
        loadTeamQuotaIntoContainer(true, teamQuota, targetContainer, null);

        getOrgDataStore(teamQuota.orgId()).commitChanges(targetContainer);
        return teamQuota;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of quota ids
     * @return The {@link Quota} data container
     */
    @Override
    protected DataContainer getDataContainer(final long orgId, final Collection<Long> ids) {
        final Table table = getQuotaTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Table teamQuotaTable = getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Column quotaPkColumn = table.getPKColumn();

        selectQuery.addSelectClause(table, teamQuotaTable, teamUserQuotaTable);
        selectQuery.addJoinClause(List.of(new JoinClause(quotaPkColumn, teamQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID)),
                new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(quotaPkColumn, ids)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        loadDataContainer(orgId, ids, dataContainer);
        return dataContainer;
    }

    /**
     * <p>
     * Loads the teams quota of the given {@link TeamUserQuota} into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether this is a create or update operation
     * @param teamQuota       Represents the {@link TeamUserQuota}
     * @param targetContainer The container to load data into
     * @param sourceContainer The container for existing data
     */
    private void loadTeamQuotaIntoContainer(final boolean isCreateEvent, final TeamQuota teamQuota,
                                            final DataContainer targetContainer, final DataContainer sourceContainer) {
        final Table teamQuotaTable = getTable();
        final Long quotaId = teamQuota.id();
        final Long teamId = teamQuota.getTeam().id();
        final Row teamQuotaRow;

        if (isCreateEvent) {
            teamQuotaRow = new Row(teamQuotaTable);

            teamQuota.setTeamQuotaId(teamQuotaRow.getPKValue());
            teamQuotaRow.setOrgId(teamQuota.orgId());
            teamQuotaRow.set(OMQFTEAMQUOTA.QUOTA_ID, quotaId);
            teamQuotaRow.set(OMQFTEAMQUOTA.TEAM_ID, teamId);
            loadTeamUserQuotaIntoContainer(true, teamQuota, targetContainer, sourceContainer);
        } else {
            teamQuotaRow = sourceContainer.getRow(teamQuotaTable,
                    new WhereClause(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID),
                            quotaId)).and(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), teamId)));

            teamQuota.setTeamQuotaId(teamQuotaRow.getPKValue());
            loadTeamUserQuotaIntoContainer(false, teamQuota, targetContainer, sourceContainer);
        }
        targetContainer.updateRow(teamQuotaRow);
    }

    /**
     * <p>
     * Loads the teams user quota of the given {@link TeamUserQuota} into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether this is a create or update operation
     * @param teamQuota       Represents the {@link TeamUserQuota}
     * @param targetContainer The container to load data into
     * @param sourceContainer The container for existing data
     */
    private void loadTeamUserQuotaIntoContainer(final boolean isCreateEvent, final TeamQuota teamQuota, final DataContainer targetContainer,
                                                final DataContainer sourceContainer) {
        final Collection<TeamUserQuota> teamUserQuotas = teamQuota.getTeamUserQuotas();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Long orgId = teamQuota.orgId();
        final Long teamQuotaId = teamQuota.getTeamQuotaId();

        for (final TeamUserQuota teamUserQuota : teamUserQuotas) {
            if (teamUserQuota.isNotPartOfTeamGoal() && !teamUserQuota.isEnabled()) {
                continue;
            }
            teamUserQuota.setTeamQuotaId(teamQuotaId);

            if (isCreateEvent) {
                loadTeamUserRowToContainer(orgId, new Row(teamUserQuotaTable), teamUserQuota, targetContainer);
            } else {
                final Row existingTeamUserRow = sourceContainer.getRow(teamUserQuotaTable, new WhereClause(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), teamQuotaId))
                        .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), teamUserQuota.getTeamUser().getCrmUser().id())));
                final Row teamUserRow = Objects.isNull(existingTeamUserRow) ? new Row(teamUserQuotaTable) : existingTeamUserRow;

                loadTeamUserRowToContainer(orgId, teamUserRow, teamUserQuota, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the teams user row of the given {@link TeamUserQuota} into the target container.
     * </p>
     *
     * @param orgId           Represents an org id
     * @param teamUserRow     Represents the team user quota row
     * @param teamUserQuota   Represents the {@link TeamUserQuota}
     * @param targetContainer The container to load data into
     */
    private void loadTeamUserRowToContainer(final long orgId, final Row teamUserRow, final TeamUserQuota teamUserQuota,
                                            final DataContainer targetContainer) {
        if (teamUserRow.isNewRow()) {
            teamUserRow.setOrgId(orgId);
        }
        teamUserQuota.setId(teamUserRow.getPKValue());
        teamUserRow.set(OMQFTEAMUSERQUOTA.TARGET, teamUserQuota.getTarget());
        teamUserRow.set(OMQFTEAMUSERQUOTA.USER_ID, teamUserQuota.getTeamUser().getCrmUser().id());
        teamUserRow.set(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID, teamUserQuota.getTeamQuotaId());
        teamUserRow.set(OMQFTEAMUSERQUOTA.IS_ENABLED, teamUserQuota.isEnabled());

        targetContainer.updateRow(teamUserRow);
    }

    /**
     * <p>
     * Loads the row to {@link TeamQuota} for the given row
     * </p>
     *
     * @param row           Represents the quota row
     * @param dataContainer A container that holds the quota related data
     * @param teamQuota     Represents the {@link TeamQuota}
     */
    private void loadRowToTeamUserQuota(final Row row, final DataContainer dataContainer, final TeamQuota teamQuota) {
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Long orgId = row.getOrgId();
        final Collection<Long> userIds = new HashSet<>();
        final Long teamId = row.get(OMQFTEAMQUOTA.TEAM_ID);
        final Stream<Row> teamUserQuotaRows = dataContainer.getRows(teamUserQuotaTable,
                new WhereClause(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), row.getPKValue())));
        final Map<Long, TeamUser> teamUserMap;

        if (Objects.nonNull(teamId)) {
            final Team team = teamManager.getTeam(orgId, teamId);
            final Collection<TeamUser> teamUsers = team.getUsers();

            teamUserMap = teamUsers.stream().collect(Collectors.toMap(teamUser -> teamUser.getCrmUser().id(), Function.identity()));
            teamQuota.setTeam(team);
            teamQuota.setActive(true);
        } else {
            teamUserMap = Collections.emptyMap();
            teamQuota.setActive(false);
        }

        teamUserQuotaRows.forEach(teamUserQuotaRow -> {
            final Long userId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.USER_ID);
            final TeamUserQuota teamUserQuota = new TeamUserQuota();

            teamUserQuota.setId(teamUserQuotaRow.getPKValue());
            teamUserQuota.setTeamQuotaId(teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID));
            teamUserQuota.setOrgId(orgId);
            teamUserQuota.setTarget(teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.TARGET));
            teamUserQuota.setEnabled(teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.IS_ENABLED));

            if (teamUserMap.containsKey(userId)) {
                teamUserQuota.setTeamUser(teamUserMap.get(userId));
            } else {
                final TeamUser teamUser = new TeamUser();

                teamUser.setCrmUser(userService.getUser(orgId, userId));
                teamUserQuota.setTeamUser(teamUser);
                teamUserQuota.setExcludedFromTeam(true);
            }
            teamQuota.addTeamQuotaUsers(teamUserQuota);
            userIds.add(userId);
        });
        teamQuota.setTeamQuotaId(row.getPKValue());
        addNewlyAddedTeamUsers(orgId, teamQuota, teamUserMap, userIds);
        teamQuota.setUserIds(userIds);
    }

    /**
     * Adds newly joined team members without existing quota rows as disabled entries.
     *
     * @param orgId the organization ID
     * @param teamQuota the team quota
     * @param teamUserMap the current team members
     * @param userIds the already loaded user IDs
     */
    private void addNewlyAddedTeamUsers(final Long orgId, final TeamQuota teamQuota, final Map<Long, TeamUser> teamUserMap,
                                        final Collection<Long> userIds) {
        teamUserMap.forEach((userId, teamUser) -> {
            if (userIds.contains(userId)) {
                return;
            }
            final TeamUserQuota teamUserQuota = new TeamUserQuota();

            teamUserQuota.setOrgId(orgId);
            teamUserQuota.setTeamQuotaId(teamQuota.getTeamQuotaId());
            teamUserQuota.setTeamUser(teamUser);
            teamUserQuota.setTarget(0.0);
            teamUserQuota.setEnabled(false);
            teamUserQuota.setNotPartOfTeamGoal(true);
            teamQuota.addTeamQuotaUsers(teamUserQuota);
            userIds.add(userId);
        });
    }

    /**
     * <p>
     * Recalculates the attainment for the given team quota and user. If userId is null, it recalculates for all users under the team quota.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param quotaId Represents a team quota id
     * @param userId  Represents a user id, can be null to indicate all users under the team quota
     */
    public void recalculateTeamQuotaAttainment(final long orgId, final long quotaId, final Long userId) {
        final TeamQuota teamQuota = get(orgId, quotaId);
        final Table teamQuotaTable = getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table teamUserQuotaDealTable = getTeamUserQuotaDealTable();
        final WhereClause quotaIdCriteria = new WhereClause(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), quotaId));
        final WhereClause userIdCriteria = Objects.isNull(userId)
                ? quotaIdCriteria : quotaIdCriteria.and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), userId));
        final DataContainer teamQuotaAttainmentDataContainer = getTeamQuotaAttainmentDataContainer(orgId, userIdCriteria);

        if (teamQuotaAttainmentDataContainer.isEmpty()) {
            return;
        }
        final Row teamQuotaRow = teamQuotaAttainmentDataContainer.getRow(teamQuotaTable);

        if (Objects.isNull(teamQuotaRow)) {
            return;
        }

        if (Objects.nonNull(userId)) {
            teamQuotaRow.set(OMQFTEAMQUOTA.ACTION_TIME, Instant.now().toEpochMilli());
        }

        final Map<Long, Boolean> userIdsWithExclusionStatus = Optional.ofNullable(teamQuota.getTeamUserQuotas())
                .map(teamUserQuotas -> teamUserQuotas.stream().collect(Collectors.toMap(teamUserQuota -> teamUserQuota.getTeamUser().getCrmUser().id(), TeamUserQuota::isExcludedFromTeam))).orElseGet(Collections::emptyMap);
        final Collection<Long> pipelineIds = Optional.ofNullable(teamQuota.getPipelines())
                .map(pipelines -> pipelines.stream().map(Pipeline::id).collect(Collectors.toSet())).orElseGet(Collections::emptySet);
        final Collection<Long> territoryIds = Optional.ofNullable(teamQuota.getTerritories())
                .map(territories -> territories.stream().map(Territory::id).collect(Collectors.toSet())).orElseGet(Collections::emptySet);
        final Collection<Long> productIds = Optional.ofNullable(teamQuota.getProducts())
                .map(products -> products.stream().map(Product::id).collect(Collectors.toSet())).orElseGet(Collections::emptySet);
        final QuotaMetric quotaMetric = teamQuota.getQuotaMetric();
        final boolean isDealValue = quotaMetric.isDealValue();
        final Map<Long, Collection<Long>> dealIdsByUserQuotaId = new LinkedHashMap<>();
        final Collection<Long> teamUserQuotaDealIds = new HashSet<>();

        teamQuotaAttainmentDataContainer.getRows(teamUserQuotaTable).forEach(teamUserQuotaRow -> {
            final Long teamUserQuotaId = teamUserQuotaRow.getPKValue();
            final Long teamUserId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.USER_ID);

            if (userIdsWithExclusionStatus.getOrDefault(teamUserId, true)
                    || !Boolean.TRUE.equals(teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.IS_ENABLED))) {
                dealIdsByUserQuotaId.put(teamUserQuotaId, Collections.emptyList());
                return;
            }
            final Collection<Long> existingDealIds = getDealIdsForTeamUserQuota(orgId, teamUserQuotaId);
            final Collection<Long> dealIds = dealManager.getDealIds(orgId, Set.of(teamUserId),
                    teamQuota.getStartTime().getTimeStamp(), teamQuota.getEndTime().getTimeStamp(),
                    pipelineIds, territoryIds, productIds, existingDealIds);

            teamUserQuotaDealIds.addAll(dealIds);
            dealIdsByUserQuotaId.put(teamUserQuotaId, dealIds);
        });
        final Map<Long, Monetary> dealValueMap = isDealValue ? dealManager.getDealValuesInBaseCurrency(orgId, teamUserQuotaDealIds)
                                                               .stream()
                                                               .collect(Collectors.toMap(TlcModel::id, Deal::getDealValueInBaseCurrency)) : Collections.emptyMap();
        final DataContainer targetContainer = DataContainer.create();
        final AtomicReference<Double> teamWonValue = new AtomicReference<>(0.0);
        final AtomicLong teamWonCount = new AtomicLong(0L);

        teamQuotaAttainmentDataContainer.getRows(teamUserQuotaTable).forEach(teamUserQuotaRow -> {
            final Long teamUserQuotaId = teamUserQuotaRow.getPKValue();
            final Long teamUserId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.USER_ID);
            final boolean isActiveTeamMember = !userIdsWithExclusionStatus.getOrDefault(teamUserId, true);
            final Collection<Long> dealIds = dealIdsByUserQuotaId.getOrDefault(teamUserQuotaId, Collections.emptyList());

            if (isDealValue) {
                double userWonTotal = Optional
                        .ofNullable((Double) teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.WON))
                        .orElse(0.0);

                for (final Long dealId : dealIds) {
                    final Monetary dealValue = dealValueMap.get(dealId);

                    if (Objects.nonNull(dealValue)) {
                        final double value = dealValue.getValue().doubleValue();

                        userWonTotal += value;

                        if (isActiveTeamMember) {
                            teamWonValue.set(teamWonValue.get() + value);
                        }
                        final Row dealRow = new Row(teamUserQuotaDealTable);

                        dealRow.setOrgId(orgId);
                        dealRow.set(OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID, teamUserQuotaId);
                        dealRow.set(OMQFTEAMUSERQUOTAWONDEAL.DEAL_ID, dealId);
                        targetContainer.addNewRow(dealRow);
                    }
                }
                teamUserQuotaRow.set(OMQFTEAMUSERQUOTA.WON, userWonTotal);
            } else {
                final long existingCount = Optional
                        .ofNullable((Long) teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.WON))
                        .orElse(0L);
                final int dealCount = dealIds.size();

                teamUserQuotaRow.set(OMQFTEAMUSERQUOTA.WON, existingCount + dealCount);
                if (isActiveTeamMember) {
                    teamWonCount.addAndGet(dealCount);
                }

                for (final Long dealId : dealIds) {
                    final Row dealRow = new Row(teamUserQuotaDealTable);

                    dealRow.setOrgId(orgId);
                    dealRow.set(OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID, teamUserQuotaId);
                    dealRow.set(OMQFTEAMUSERQUOTAWONDEAL.DEAL_ID, dealId);
                    targetContainer.addNewRow(dealRow);
                }
            }

            if (Objects.equals(userId, teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.USER_ID))) {
                teamUserQuotaRow.set(OMQFTEAMUSERQUOTA.ACTION_TIME, Instant.now().toEpochMilli());
            }
            targetContainer.updateRow(teamUserQuotaRow);
        });

        if (isDealValue) {
            final double currentTeamWon = Optional.ofNullable((Double) teamQuotaRow.get(OMQFTEAMQUOTA.WON)).orElse(0.0);

            teamQuotaRow.set(OMQFTEAMQUOTA.WON, currentTeamWon + teamWonValue.get());
        } else {
            final long currentTeamWon = Optional.ofNullable((Long) teamQuotaRow.get(OMQFTEAMQUOTA.WON)).orElse(0L);

            teamQuotaRow.set(OMQFTEAMQUOTA.WON, currentTeamWon + teamWonCount.get());
        }
        targetContainer.updateRow(teamQuotaRow);
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Checks if the quota was recently recalculated for the team or a specific user.
     *
     * @param orgId   Represents an organization id
     * @param quotaId Represents a team quota id
     * @param userId  Represents a user id (optional)
     * @param time    The threshold timestamp
     * @return true if recalculated after the threshold, otherwise false
     */
    public boolean isQuotaRecentlyRecalculated(final long orgId, final long quotaId, final Long userId, final Long time) {
        final Table teamQuotaTable = getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final CountQuery countQuery = CountQuery.get(teamQuotaTable);

        if (Objects.isNull(userId)) {
            countQuery.setWhereClause(new WhereClause(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), quotaId))
                    .and(Criteria.ge(teamQuotaTable.getColumn(OMQFTEAMQUOTA.ACTION_TIME), time)));
        } else {
            countQuery.addJoinClause(new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)));
            countQuery.setWhereClause(new WhereClause(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), userId))
                    .and(Criteria.ge(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.ACTION_TIME), time)));
        }
        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * Retrieves deal IDs already associated with a specific TeamUserQuota.
     * Scoped per teamUserQuotaId so that the same deal can legitimately
     * exist under a different TeamUserQuota within the same TeamQuota.
     *
     * @param orgId           the organization identifier
     * @param teamUserQuotaId the team user quota identifier
     * @return a collection of deal IDs already linked to this TeamUserQuota
     */
    private Collection<Long> getDealIdsForTeamUserQuota(final long orgId, final Long teamUserQuotaId) {
        final Table teamUserQuotaDealTable = getTeamUserQuotaDealTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(teamUserQuotaTable.getPKColumn(), teamUserQuotaId));
        final DataContainer dataContainer = getTeamQuotaAttainmentDataContainer(orgId, whereClause);

        return dataContainer.getRows(teamUserQuotaDealTable)
                .map(row -> (Long) row.get(OMQFTEAMUSERQUOTAWONDEAL.DEAL_ID))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves the attainment data for a team quota based on the given criteria.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the criteria to filter team user quotas for attainment calculation
     * @return The data container containing the attainment data for the team quota
     */
    private DataContainer getTeamQuotaAttainmentDataContainer(final long orgId, final WhereClause whereClause) {
        final Table teamQuotaTable = getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table teamUserQuotaDealTable = getTeamUserQuotaDealTable();
        final SelectQuery selectQuery = SelectQuery.get(teamQuotaTable);

        selectQuery.addSelectClause(teamUserQuotaTable, teamQuotaTable, teamUserQuotaDealTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)),
                new JoinClause(teamUserQuotaTable.getPKColumn(), teamUserQuotaDealTable.getColumn(OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID))));
        selectQuery.setWhereClause(whereClause);

        return getOrgDataStore(orgId).get(selectQuery);
    }
}
