package com.tlc.crm.common.iam.api.manager.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.internal.manager.AbstractIamConfigManager;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * A {@link TeamManagerImpl} that provides the various methods for the {@link TeamManager}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TeamManagerImpl extends AbstractIamConfigManager<Team> implements TeamManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TeamManagerImpl.class);

    private final CrmUserManagerImpl crmUserManager;
    private final UserService userService;

    private TeamManagerImpl() {
        crmUserManager = CrmUserManagerImpl.getInstance();
        userService = UserService.getInstance();
    }

    private static class Instance {
        private static final TeamManagerImpl INSTANCE = new TeamManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link TeamManagerImpl} instance
     * </p>
     *
     * @return The instance of {@link TeamManagerImpl}
     */
    public static TeamManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The team {@link Table}
     */
    @Override
    public Table getTable() {
        return Table.get(OMCRMTEAM.TABLE);
    }

    /**
     * <p>
     * Gets the user team table
     * </p>
     *
     * @return The user team {@link Table}
     */
    public Table getUserTeamTable() {
        return Table.get(MCRMUSERTEAM.TABLE);
    }

    /**
     * <p>
     * Gets the user table
     * </p>
     *
     * @return The user {@link Table}
     */
    private Table getUserTable() {
        return crmUserManager.getTable();
    }

    /**
     * {@inheritDoc}
     *
     * @param team Represents the {@link Team} details
     * @return The {@link Team} details
     */
    @Override
    public Team create(final Team team) {
        final DataContainer targetContainer = DataContainer.create();

        loadTeamIntoContainer(true, team, null, targetContainer);
        secureOrgDataStore(team.orgId()).commitChanges(targetContainer);

        return team;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     * @param teams Contains the collection of models
     * @return The updated response sent
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Team> teams) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final Column teamPkColumn = teamTable.getPKColumn();
        final Collection<Long> teamIds = teams.stream().map(Team::id).toList();

        selectQuery.addSelectClause(teamTable, userTeamTable);
        selectQuery.addJoinClause(new JoinClause(teamPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(teamPkColumn, teamIds)));
        final DataContainer sourceContainer = secureOrgDataStore(orgId).get(selectQuery);

        for (final Team team : teams) {
            final Long id = team.id();

            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTeamIntoContainer(false, team, sourceContainer, targetContainer);
                secureOrgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(id);
                final Collection<TeamUser> teamUsers = team.getUsers();
                final Collection<Long> ids = teamUsers.stream().map(TeamUser::getCrmUser).map(CrmUser::id).collect(Collectors.toUnmodifiableSet());
                final Collection<Long> userIds = sourceContainer.getRows(userTeamTable, new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), id))
                        .and(Criteria.in(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), ids))).map(row -> (Long) row.get(MCRMUSERTEAM.USER_ID)).toList();

                if (!userIds.isEmpty()) {
                    team.getUsers().removeIf(teamUser -> userIds.contains(teamUser.getCrmUser().id()));
                }
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Team update failed Org ID : {} ID : {} reason : {}", orgId, id, errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TEAM_UPDATE_FAILED,
                        "i18n.team.update.action.failed"), ErrorKeys.UPDATE_ACTION_FAILED, team.id().toString()));
            }
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TEAM_UPDATE_FAILED,
                    "i18n.team.update.action.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param teams Contains the collection of teams
     * @return The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome partialUpdate(final Long orgId, final Collection<Team> teams) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Table teamTable = getTable();

        for (final Team team : teams) {
            try {
                final UpdateQuery updateQuery = UpdateQuery.get(teamTable);
                final Long teamId = team.id();

                updateQuery.addUpdateColumn(teamTable.getColumn(OMCRMTEAM.NAME), team.getName());
                updateQuery.addUpdateColumn(teamTable.getColumn(OMCRMTEAM.UPDATED_TIME), Instant.now().toEpochMilli());
                updateQuery.addUpdateColumn(teamTable.getColumn(OMCRMTEAM.UPDATED_BY), team.getUpdatedBy().id());
                updateQuery.setWhereClause(new WhereClause(Criteria.eq(teamTable.getPKColumn(), teamId)));

                secureOrgDataStore(orgId).update(updateQuery);
                processOutcome.addProcessedRecord(teamId);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Team update failed Org ID : {} ID : {} reason : {}", orgId, team.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TEAM_UPDATE_FAILED,
                        "i18n.team.update.action.failed"), ErrorKeys.UPDATE_ACTION_FAILED, team.id().toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TEAM_UPDATE_FAILED,
                    "i18n.team.update.action.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the team id
     * @return The {@link Team} details
     */
    @Override
    public Team get(final Long orgId, final Long id) {
        final Row row = orgDataStore(orgId).get(getTable(), id);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamErrorCodes.TEAM_NOT_FOUND, "i18n.team.not.found");
        }

        return convertRowToModel(row, null, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param teamIds Contains the collection of team ids
     * @param names   Contains the collection of team names
     * @return If any team is exists it will return true, otherwise it will return false
     */
    @Override
    public boolean checkTeamsAreExists(final Long orgId, final Collection<Long> teamIds, final Collection<String> names) {
        final Table teamTable = getTable();
        final CountQuery countQuery = CountQuery.get(teamTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(teamTable.getColumn(OMCRMTEAM.NAME), names)).
                and(Criteria.notIn(teamTable.getPKColumn(), teamIds)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param teamId         Represents the team id
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The {@link Team} details
     */
    @Override
    public Team get(final Long orgId, final Long teamId, final int userStartValue, final int userLimit) {
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final Table userTable = getUserTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final Column teamPkColumn = teamTable.getPKColumn();
        final Column userPkColumn = userTable.getPKColumn();

        selectQuery.addJoinClause(List.of(
                new JoinClause(teamPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID)),
                new JoinClause(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userPkColumn)));
        selectQuery.addSelectClause(List.of(teamTable.getColumn(OMCRMTEAM.NAME), teamTable.getColumn(OMCRMTEAM.ORG_ID)));
        selectQuery.addSelectClause(userTable, userTeamTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamPkColumn, teamId)));
        selectQuery.setLimitClause(new LimitClause(userStartValue, userLimit));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(getTable());

        return Objects.isNull(row) ? null : convertRowToModel(row, dataContainer, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param teamId Represents the team id
     * @return The total number of users
     */
    @Override
    public long getUsersCount(final Long orgId, final Long teamId) {
        final Table userTeamTable = getUserTeamTable();
        final CountQuery countQuery = CountQuery.get(userTeamTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param teamId Represents the team id.
     * @return The total number of managers of a team.
     */
    @Override
    public long getTeamManagersCount(final long orgId, final long teamId) {
        final Table userTeamTable = getUserTeamTable();
        final CountQuery countQuery = CountQuery.get(userTeamTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamId))
                .and(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.IS_MANAGER), true)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param teamId Represents the team id.
     * @return The total number of manager ids of a team.
     */
    @Override
    public Collection<Long> getTeamManagerIds(final long orgId, final long teamId) {
        final Table userTeamTable = getUserTeamTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamId)).and(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.IS_MANAGER), true));
        final DataContainer dataContainer = orgDataStore(orgId).get(userTeamTable, whereClause, List.of(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID)));

        return dataContainer.getRows(userTeamTable).map(row -> (Long) row.get(MCRMUSERTEAM.USER_ID)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index of team to be fetched
     * @param limit Represents the maximum number of team to be fetched
     * @return The collection of {@link Team} details
     */
    @Override
    public Collection<Team> get(final Long orgId, final int start, final int limit) {
        final Collection<Team> teams = new ArrayList<>();
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final SelectQuery innerSelectQuery = SelectQuery.get(teamTable);
        final Table userTable = getUserTable();
        final Column teamPkColumn = teamTable.getPKColumn();
        final Column userTeamIdColumn = userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID);
        final Column teamUserIdColumn = userTeamTable.getColumn(MCRMUSERTEAM.USER_ID);
        final Column isManagerColumn = userTeamTable.getColumn(MCRMUSERTEAM.IS_MANAGER);

        innerSelectQuery.addSelectClause(teamPkColumn);
        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(teamTable.getColumn(OMCRMTEAM.ORG_ID), orgId)));
        final SelectQuery selectQuery = SelectQuery.get(teamTable);

        selectQuery.addJoinClause(List.of(new JoinClause(teamPkColumn, userTeamIdColumn),
                new JoinClause(teamUserIdColumn, userTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(userTable.getColumn(OMCRMUSER.IAM_USER_ID), teamUserIdColumn, userTeamIdColumn, isManagerColumn));
        selectQuery.addSelectClause(teamTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(userTeamIdColumn, innerSelectQuery)));
        selectQuery.addOrderByClause(new OrderByClause(teamTable.getColumn(OMCRMTEAM.CREATED_TIME), OrderByClause.OrderType.ASCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(teamTable).forEach(row -> {
            final Long createdByUserId = row.get(OMCRMTEAM.CREATED_BY);
            final Long updatedByUserId = row.get(OMCRMTEAM.UPDATED_BY);
            final Long teamId = row.getPKValue();
            final Criteria teamIdCriteria = Criteria.eq(userTeamIdColumn, teamId);
            final long usersCount = dataContainer.getRows(userTeamTable, new WhereClause(teamIdCriteria)).count();
            final long managersCount = dataContainer.getRows(userTeamTable,
                    new WhereClause(Criteria.eq(isManagerColumn, true)).and(teamIdCriteria)).count();
            final Team team = new Team();

            team.setId(teamId);
            team.setName(row.get(OMCRMTEAM.NAME));
            team.setCreatedTime(row.get(OMCRMTEAM.CREATED_TIME));
            team.setUpdatedTime(row.get(OMCRMTEAM.UPDATED_TIME));
            team.setUsersCount(usersCount);
            team.setManagersCount(managersCount);

            if (Objects.nonNull(createdByUserId) && Objects.nonNull(updatedByUserId)) {
                loadCreatedByAndUpdatedByIntoTeam(orgId, createdByUserId, updatedByUserId, team);
            }
            teams.add(team);
        });

        return teams;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param teamId         Represents the team id
     * @param fetchManagers  A flag indicates whether there is a need to retrieve users assigned as managers or not.
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The Collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsers(final Long orgId, final Long teamId, final boolean fetchManagers, final int userStartValue, final int userLimit) {
        final Table userTable = getUserTable();
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final Column teamPkColumn = teamTable.getPKColumn();
        final Column teamUserIdColumn = userTeamTable.getColumn(MCRMUSERTEAM.USER_ID);
        final Column userTeamIdColumn = userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID);
        final WhereClause whereClause = new WhereClause(Criteria.eq(teamPkColumn, teamId));

        selectQuery.addJoinClause(List.of(new JoinClause(teamPkColumn, userTeamIdColumn),
                new JoinClause(teamUserIdColumn, userTable.getPKColumn())));
        selectQuery.setLimitClause(new LimitClause(userStartValue, userLimit));
        selectQuery.addSelectClause(userTable);
        selectQuery.addSelectClause(List.of(teamUserIdColumn, userTeamIdColumn, teamPkColumn));
        selectQuery.setWhereClause(fetchManagers ? whereClause.and(Criteria
                .eq(userTeamTable.getColumn(MCRMUSERTEAM.IS_MANAGER), true)) : whereClause);

        return orgDataStore(orgId).get(selectQuery).getRows(userTable).map(crmUserManager::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Refers the organization id.
     * @param teamId          Refers the team id.
     * @param userId          Refers the user id.
     * @param assignAsManager A flag indicates whether the user linkUsers as manager or not.
     * @return The updated status.
     */
    @Override
    public boolean designateUserAsManager(final long orgId, final long teamId, final long userId, final boolean assignAsManager) {
        final Table userTeamTable = getUserTeamTable();
        final UpdateQuery updateQuery = UpdateQuery.get(userTeamTable);

        updateQuery.addUpdateColumn(userTeamTable.getColumn(MCRMUSERTEAM.IS_MANAGER), assignAsManager);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userId))
                .and(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamId)));

        try {
            secureOrgDataStore(orgId).update(updateQuery);

            return true;
        } catch (ErrorCode errorCode) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id.
     * @param teamId Refers the team id.
     * @param userId Refers the user id.
     * @return true, if the user is a member of the team, otherwise false.
     */
    @Override
    public boolean isUserPartOfTeam(final long orgId, final long teamId, final long userId) {
        final Table userTeamTable = getUserTeamTable();
        final CountQuery countQuery = CountQuery.get(userTeamTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userId))
                .and(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamId)));

        return secureOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Refers the organization id.
     * @param teamId  Refers the team id.
     * @param userIds Refers the set of user ids.
     * @return True, if the user is a member of the team, otherwise false.
     */
    @Override
    public boolean areUsersPartOfTeam(final long orgId, final long teamId, final Set<Long> userIds) {
        final Table userTeamTable = getUserTeamTable();
        final CountQuery countQuery = CountQuery.get(userTeamTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userIds))
                .and(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamId)));

        return secureOrgDataStore(orgId).get(countQuery) == userIds.size();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param limitClause Represents the start and limit the team to be fetched
     * @param criteria    Represents the criteria for team filtering
     * @return The collection of {@link Team} details
     */
    @Override
    public Collection<Team> getTeamDropDownList(final Long orgId, final LimitClause limitClause, final Criteria criteria) {
        final Collection<Team> teams = new ArrayList<>();
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMCRMTEAM.NAME));
        selectQuery.setWhereClause(new WhereClause(criteria));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn()));
        selectQuery.setLimitClause(limitClause);
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(table);

        rows.forEach(row -> {
            final Team team = new Team();

            team.setId(row.getPKValue());
            team.setName(row.get(OMCRMTEAM.NAME));
            teams.add(team);
        });

        return teams;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param teamIds Represents the collection of team ids
     * @return {@link Collection} of user ids associated with team
     */
    @Override
    public Collection<Long> getTeamUserIds(final Long orgId, final Collection<Long> teamIds) {
        return getTeamUserIds(orgId, new WhereClause(Criteria.in(getUserTeamTable().getColumn(MCRMUSERTEAM.TEAM_ID), teamIds)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param teamIds       Represents the collection of team ids
     * @param excludedUsers Represents the collection of user ids to be filtered
     * @return {@link Collection} of user ids associated with team
     */
    @Override
    public Collection<Long> getTeamUserIds(final Long orgId, final Collection<Long> teamIds, final Collection<Long> excludedUsers) {
        final Table userTeamTable = getUserTeamTable();

        return getTeamUserIds(orgId, new WhereClause(Criteria.in(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamIds))
                .and(Criteria.notIn(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), excludedUsers)));
    }

    /**
     * <p>
     * Fetches the team user ids by applying the provided where clause condition
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the where clause to filter while fetching users
     * @return {@link Collection} of user ids associated with team
     */
    private Collection<Long> getTeamUserIds(final Long orgId, final WhereClause whereClause) {
        final Table userTeamTable = getUserTeamTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(userTeamTable, whereClause,
                List.of(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID)));

        return dataContainer.getRows(userTeamTable).map(row -> (Long) row.get(MCRMUSERTEAM.USER_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return Collection of user ids.
     */
    @Override
    public Collection<Long> getTeamMateIds(final Long orgId, final Long userId) {
        final Table userTeamTable = getUserTeamTable();
        final SelectQuery innerSelectQuery = SelectQuery.get(userTeamTable);
        final SelectQuery selectQuery = SelectQuery.get(userTeamTable);
        final Column teamIdColumn = userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID);
        final Column userIdColumn = userTeamTable.getColumn(MCRMUSERTEAM.USER_ID);

        innerSelectQuery.addSelectClause(teamIdColumn);
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(userIdColumn, userId)));
        selectQuery.addSelectClause(userIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(teamIdColumn, innerSelectQuery)).and(Criteria.notEq(userIdColumn, userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(userTeamTable).map(row -> (Long) row.get(MCRMUSERTEAM.USER_ID)).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id
     * @param teamIds Represents the team ids
     * @return True if team ids exist, otherwise false
     */
    @Override
    public boolean exists(final long orgId, final Collection<Long> teamIds) {
        final Table crmTeamTable = getTable();
        final CountQuery countQuery = CountQuery.get(crmTeamTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(crmTeamTable.getPKColumn(), teamIds)));

        return orgDataStore(orgId).get(countQuery) == teamIds.size();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param teamIds Represents the {@link Collection} of {@link Team} ids
     * @param isPartial Represents whether partial data required or not
     * @return The {@link Collection} of {@link Team}
     */
    @Override
    public Collection<Team> getTeams(final long orgId, final Collection<Long> teamIds, final boolean isPartial) {
        final Table teamTable = getTable();
        final SelectQuery selectQuery = getSelectQuery(teamIds, isPartial);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptySet() : dataContainer.getRows(teamTable)
                .map(row -> convertRowToModel(row, dataContainer, isPartial)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the team select query
     * </p>
     *
     * @param ids       Contains the collection of team ids
     * @param isPartial Represents whether partial data required or not
     * @return The {@link SelectQuery}
     */
    private SelectQuery getSelectQuery(final Collection<Long> ids, final boolean isPartial) {
        final Table teamTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final Column teamPkColumn = teamTable.getPKColumn();

        if (isPartial) {
            selectQuery.addSelectClause(teamTable.getColumn(OMCRMTEAM.NAME));
        } else {
            final Table userTeamTable = getUserTeamTable();
            final Table userTable = getUserTable();

            selectQuery.addJoinClause(List.of(
                    new JoinClause(teamPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID)),
                    new JoinClause(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userTable.getPKColumn()))
            );
            selectQuery.addSelectClause(teamTable, userTable, userTeamTable);
        }
        selectQuery.setWhereClause(new WhereClause(Criteria.in(teamPkColumn, ids)));

        return selectQuery;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param names Contains the collection of team names
     * @return If any team is exists it will return true, otherwise it will return false
     */
    public boolean checkTeamsAreExists(final Long orgId, final Collection<String> names) {
        final Table teamTable = getTable();
        final CountQuery countQuery = CountQuery.get(teamTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(teamTable.getColumn(OMCRMTEAM.NAME), names)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id.
     * @return the team ids
     */
    @Override
    public Collection<Long> getUserTeamIds(final long orgId, final long userId) {
        final Table userTeamTable = getUserTeamTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(userTeamTable,
                new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userId)));

        return dataContainer.getRows(userTeamTable).map(row -> (Long) row.get(MCRMUSERTEAM.TEAM_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param teamId Represents the {@link Team}
     * @return the {@link Team}
     */
    @Override
    public Team getTeam(final long orgId, final Long teamId) {
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final Table userTable = getUserTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final Column teamPkColumn = teamTable.getPKColumn();

        selectQuery.addJoinClause(List.of(new JoinClause(teamPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID)),
                new JoinClause(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userTable.getPKColumn())));
        selectQuery.addSelectClause(teamTable, userTable, userTeamTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamPkColumn, teamId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return convertRowToModel(dataContainer.getRow(teamTable), dataContainer, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param searchFilter Represents the {@link SearchFilter}
     * @return The collection of {@link Team} details
     */
    @Override
    public Collection<Team> getTeams(final long orgId, final SearchFilter searchFilter) {
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final Table userTable = getUserTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final Column teamNameColumn = teamTable.getColumn(OMCRMTEAM.NAME);
        final Object value = searchFilter.getValue();

        selectQuery.addSelectClause(teamNameColumn);
        selectQuery.addSelectClause(userTeamTable, userTable);
        selectQuery.addJoinClause(List.of(new JoinClause(teamTable.getPKColumn(), userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID)),
                new JoinClause(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userTable.getPKColumn())));

        if (Objects.nonNull(value)) {
            selectQuery.setWhereClause(new WhereClause(new Criteria(teamNameColumn, value, Operator.getOperator(
                    searchFilter.getType()))));
        }
        selectQuery.setLimitClause(new LimitClause(searchFilter.getStart(), searchFilter.getLimit()));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptySet() : dataContainer.getRows(teamTable)
                .map(row -> convertRowToModel(row, dataContainer, false)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param searchFilter Represents the {@link SearchFilter}
     * @return The total team count
     */
    public long getTeamsCount(final long orgId, final SearchFilter searchFilter) {
        final Table teamTable = getTable();
        final CountQuery countQuery = CountQuery.get(teamTable);
        final Column teamNameColumn = teamTable.getColumn(OMCRMTEAM.NAME);
        final Object value = searchFilter.getValue();

        if (Objects.nonNull(value)) {
            countQuery.setWhereClause(new WhereClause(new Criteria(teamNameColumn, value, Operator.getOperator(
                    searchFilter.getType()))));
        }
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Loads the team details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param team            Represents the team details which have to been saved in database
     * @param targetContainer Holds the field values of existing team
     * @param sourceContainer Holds the user defined and system field values of existing team
     */
    private void loadTeamIntoContainer(final boolean isCreateEvent, final Team team, final DataContainer sourceContainer,
                                       final DataContainer targetContainer) {
        final Table teamTable = getTable();
        final Collection<TeamUser> teamUsers = team.getUsers();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row teamRow = isCreateEvent ? new Row(teamTable) : sourceContainer.getRow(teamTable,
                new WhereClause(Criteria.eq(teamTable.getPKColumn(), team.id())));
        final Long teamId = teamRow.getPKValue();

        if (teamRow.isNewRow()) {
            team.setId(teamId);
            teamRow.setOrgId(team.orgId());
            teamRow.set(OMCRMTEAM.CREATED_BY, team.getCreatedBy().id());
            teamRow.set(OMCRMTEAM.CREATED_TIME, currentTime);
        }
        teamRow.set(OMCRMTEAM.NAME, team.getName());
        teamRow.set(OMCRMTEAM.UPDATED_TIME, currentTime);
        teamRow.set(OMCRMTEAM.UPDATED_BY, team.getUpdatedBy().id());

        if (Objects.nonNull(teamUsers)) {
            loadUserTeamIntoContainer(isCreateEvent, teamId, teamUsers, sourceContainer, targetContainer);
        }

        if (!isCreateEvent) {
            deleteUnmappedTeamUser(team, sourceContainer, targetContainer);
        }
        targetContainer.updateRow(teamRow);
    }

    /**
     * <p>
     * Deletes unmapped team users of the team in the container
     * </p>
     *
     * @param team            Represents the {@link Team} details
     * @param sourceContainer Holds the user defined and system field values of existing team
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedTeamUser(final Team team, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<TeamUser> teamUsers = team.getUsers();

        if (Objects.nonNull(teamUsers) && !teamUsers.isEmpty()) {
            final Collection<Long> userIds = teamUsers.stream().map(TeamUser::getCrmUser).map(CrmUser::id).toList();
            final Table userTeamTable = getUserTeamTable();
            final WhereClause whereClause = new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), team.id()))
                    .and(Criteria.notIn(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userIds));
            final Stream<Row> userRows = sourceContainer.getRows(userTeamTable, whereClause);

            userRows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the team users details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param teamId          Represents the team id
     * @param teamUsers       Which contains the collection of {@link TeamUser}
     * @param sourceContainer Holds the user defined and system field values of existing team
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadUserTeamIntoContainer(final boolean isCreateEvent, final Long teamId, final Collection<TeamUser> teamUsers,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table userTeamTable = getUserTeamTable();

        for (final TeamUser teamUser : teamUsers) {
            final Long userId = teamUser.getCrmUser().id();
            final boolean isManager = teamUser.isManager();
            final Row row;

            if (isCreateEvent) {
                row = new Row(userTeamTable);
            } else {
                final Row existingRow = sourceContainer.getRow(userTeamTable, new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userId))
                        .and(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamId)));

                row = Objects.isNull(existingRow) ? new Row(userTeamTable) : existingRow;
            }

            if (row.isNewRow()) {
                row.set(MCRMUSERTEAM.USER_ID, userId);
                row.set(MCRMUSERTEAM.TEAM_ID, teamId);
            }
            row.set(MCRMUSERTEAM.IS_MANAGER, isManager);
            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link Team}
     * </p>
     *
     * @param row           Represents the team row
     * @param dataContainer A container that holds the user defined field values and system values
     * @param isPartial     Represents whether partial data required or not
     * @return The {@link Team} details
     */
    private Team convertRowToModel(final Row row, final DataContainer dataContainer, final boolean isPartial) {
        final Team team = new Team();
        final Long teamId = row.getPKValue();

        team.setId(teamId);
        team.setName(row.get(OMCRMTEAM.NAME));

        if (isPartial) {
            return team;
        }
        final Long orgId = row.getOrgId();
        final Long createdByUserId = row.get(OMCRMTEAM.CREATED_BY);
        final Long updatedByUserId = row.get(OMCRMTEAM.UPDATED_BY);

        if (Objects.nonNull(dataContainer)) {
            final Table userTeamTable = getUserTeamTable();
            final Table userTable = getUserTable();
            final Stream<Row> rows = dataContainer.getRows(userTeamTable, new WhereClause(Criteria.eq(userTeamTable
                    .getColumn(MCRMUSERTEAM.TEAM_ID), teamId)));
            final Collection<Long> userIds = rows.map(userTeamRow -> (Long) userTeamRow.get(MCRMUSERTEAM.USER_ID)).collect(Collectors.toUnmodifiableSet());
            final Stream<Row> userRows = dataContainer.getRows(userTable, new WhereClause(Criteria.in(userTable.getPKColumn(), userIds)));

            userRows.forEach(userRow -> {
                final TeamUser teamUser = new TeamUser();
                final CrmUser crmUser = crmUserManager.convertRowToModel(userRow);
                final Long crmUserId = crmUser.id();
                final Row teamUserRow = dataContainer.getRow(userTeamTable, new WhereClause(Criteria.eq(userTeamTable.
                        getColumn(MCRMUSERTEAM.USER_ID), crmUserId)));

                if (Objects.nonNull(teamUserRow)) {
                    teamUser.setManager(teamUserRow.get(MCRMUSERTEAM.IS_MANAGER));
                }
                teamUser.setCrmUser(crmUser);
                team.addUser(teamUser);
            });
        }
        team.setOrgId(orgId);
        team.setCreatedTime(row.get(OMCRMTEAM.CREATED_TIME));
        team.setUpdatedTime(row.get(OMCRMTEAM.UPDATED_TIME));

        if (Objects.nonNull(createdByUserId) && Objects.nonNull(updatedByUserId)) {
            loadCreatedByAndUpdatedByIntoTeam(orgId, createdByUserId, updatedByUserId, team);
        }

        return team;
    }

    /**
     * <p>
     * Loads the created by and updated by users into team
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param createdByUserId Represents the created by user id
     * @param updatedByUserId Represents the updated by used id
     * @param team            Represents the team details
     */
    private void loadCreatedByAndUpdatedByIntoTeam(final Long orgId, final Long createdByUserId, final Long updatedByUserId, final Team team) {
        final Collection<CrmUser> crmUsers = userService.getUsers(orgId, List.of(createdByUserId, updatedByUserId));

        for (final CrmUser crmUser : crmUsers) {
            final Long userId = crmUser.id();

            if (Objects.equals(userId, createdByUserId)) {
                team.setCreatedBy(crmUser);
            }

            if (Objects.equals(userId, updatedByUserId)) {
                team.setUpdatedBy(crmUser);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param team Holds the details of the team.
     */
    @Override
    public void linkUsers(final Team team) {
        final Long teamId = team.id();
        final Collection<Long> userIds = team.getUserIds();
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final DataContainer targetContainer = DataContainer.create();
        final OrgDataStore orgDataStore = secureOrgDataStore(team.orgId());
        final Column teamPkColumn = teamTable.getPKColumn();

        selectQuery.addSelectClause(teamTable);
        selectQuery.addSelectClause(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID));
        selectQuery.addJoinClause(new JoinClause(teamPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamPkColumn, teamId)));
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final Collection<Long> existingIds = dataContainer.getRows(userTeamTable).map(row -> (Long) row.get(MCRMUSERTEAM.USER_ID))
                .collect(Collectors.toSet());
        Row teamRow = null;

        for (final Long userId : userIds) {

            if (!existingIds.contains(userId)) {
                final Row row = new Row(userTeamTable);

                row.set(MCRMUSERTEAM.TEAM_ID, teamId);
                row.set(MCRMUSERTEAM.USER_ID, userId);

                if (Objects.isNull(teamRow)) {
                    teamRow = dataContainer.getRow(teamTable);
                }
                targetContainer.updateRow(row);
            }
        }

        if (Objects.nonNull(teamRow)) {
            teamRow.set(OMCRMTEAM.UPDATED_TIME, Instant.now().toEpochMilli());
            teamRow.set(OMCRMTEAM.UPDATED_BY, team.getUpdatedBy().id());
            targetContainer.updateRow(teamRow);
        }
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param team Holds the details of the team.
     */
    @Override
    public void unlinkUsers(final Team team) {
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final OrgDataStore orgDataStore = secureOrgDataStore(team.orgId());
        final Column teamPkColumn = teamTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);

        selectQuery.addJoinClause(new JoinClause(teamPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamPkColumn, team.id()))
                .and(Criteria.in(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), team.getUserIds())));
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();
        final Row teamRow = dataContainer.getRow(teamTable);

        if (Objects.nonNull(teamRow)) {
            teamRow.set(OMCRMTEAM.UPDATED_TIME, Instant.now().toEpochMilli());
            teamRow.set(OMCRMTEAM.UPDATED_BY, team.getUpdatedBy().id());
            targetContainer.updateRow(teamRow);
        }
        dataContainer.getRows(userTeamTable).forEach(targetContainer::deleteRow);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents an organization id
     * @param requiredUserCount A flag indicating whether the territory should include the user count
     * @param limitClause       Represents the start and limit the team to be fetched
     * @param criteria          Represents the criteria for team filtering
     * @return The collection of {@link Team} details
     */
    @Override
    public Collection<Team> getTeamDropDownList( final Long orgId,
                                                 final boolean requiredUserCount,
                                                 final LimitClause limitClause,
                                                 final Criteria criteria )
    {
        final Collection<Team> teams = new ArrayList<>();
        final Table teamTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final SelectQuery selectQuery = SelectQuery.get( teamTable );
        final Column teamPkColumn = teamTable.getPKColumn();
        final Column teamNameColumn = teamTable.getColumn( OMCRMTEAM.NAME );
        final Column teamOrgColumn = teamTable.getColumn( OMCRMTEAM.ORG_ID );
        final Column userCountColumn = userTeamTable.getColumn( MCRMUSERTEAM.USER_ID )
                                                    .count();

        selectQuery.addJoinClause( new JoinClause( teamPkColumn, userTeamTable.getColumn( MCRMUSERTEAM.TEAM_ID ) ) );
        selectQuery.addSelectClause( List.of( userCountColumn, teamPkColumn, teamNameColumn, teamOrgColumn ) );
        selectQuery.addOrderByClause( new OrderByClause( teamPkColumn ) );
        selectQuery.setWhereClause( new WhereClause( criteria ) );
        selectQuery.setLimitClause( limitClause );
        selectQuery.addGroupByClause( List.of( new GroupByClause( teamPkColumn ), new GroupByClause( teamNameColumn ), new GroupByClause( teamOrgColumn ) ) );
        final List<Map<String, Object>> teamDetails = orgDataStore( orgId ).getData( selectQuery );

        if( Objects.nonNull( teamDetails ) && !teamDetails.isEmpty() )
        {
            teamDetails.forEach( teamDetail ->
                       {
                           final Team team = new Team();

                           if( requiredUserCount )
                           {
                               team.setUsersCount( ( long ) teamDetail.get( MCRMUSERTEAM.USER_ID ) );
                           }

                           team.setId( ( long ) teamDetail.get( OMCRMTEAM.ID ) );
                           team.setName( ( String ) teamDetail.get( OMCRMTEAM.NAME ) );
                           teams.add( team );
                       } );
        }

        return teams;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        The unique identifier of the organization whose teams are to be retrieved.
     * @param searchFilter An optional {@link SearchFilter} containing filtering and pagination criteria;
     *                     may be {@code null} to fetch all teams.
     * @return A collection of {@link Team} objects matching the specified organization and filter criteria.
     */
    @Override
    public Collection<Team> getTeamList(final long orgId, final SearchFilter searchFilter) {
        final Table teamTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(teamTable);
        final Column teamNameColumn = teamTable.getColumn(OMCRMTEAM.NAME);
        final Object value = searchFilter.getValue();

        if (Objects.nonNull(value)) {
            selectQuery.setWhereClause(new WhereClause(new Criteria(teamNameColumn, value, Operator.getOperator(searchFilter.getType()))));
        }

        selectQuery.addSelectClause(List.of(teamNameColumn));
        selectQuery.addOrderByClause(new OrderByClause(teamTable.getPKColumn()));
        selectQuery.setLimitClause(new LimitClause(searchFilter.getStart(), searchFilter.getLimit()));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(teamTable).map(row -> {
            final Team team = new Team();

            team.setId(row.getPKValue());
            team.setName(row.get(OMCRMTEAM.NAME));
            return team;
        }).toList();
    }
}
