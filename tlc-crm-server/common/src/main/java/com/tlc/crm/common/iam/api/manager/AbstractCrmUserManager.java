package com.tlc.crm.common.iam.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.impl.RoleManagerImpl;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.preferences.manager.PreferenceManager;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.manager.impl.UserPreferenceManagerImpl;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.builder.CrmRoleBuilder;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.sql.resource.MCRMUSERREPORTINGMANAGER;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;
import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.common.sql.resource.OMCRMUSERPREFERENCE;

import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.iam.common.models.User;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.WhereClauseBuilder;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * A {@link AbstractCrmUserManager} that provides the various methods for the {@link CrmUserManager}
 * </p>
 *
 * @author PandiarajkumarG
 * @author Sathish
 * @version 1.0
 * @see CrmUserManager
 */
public abstract class AbstractCrmUserManager implements CrmUserManager {

    private static final short MIN_USER_COUNT = 1;
    private final CrmRoleBuilder roleBuilder;
    private final PreferenceManager<UserPreferenceHolder> preferenceManager;

    protected AbstractCrmUserManager() {
        roleBuilder = CrmRoleBuilder.getInstance();
        preferenceManager = UserPreferenceManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link AdminDataStore}
     * </p>
     *
     * @return the {@link AdminDataStore}
     */
    private AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Gets the crm user table
     * </p>>
     *
     * @return The user {@link Table}
     */
    public Table getTable() {
        return Table.get(OMCRMUSER.TABLE);
    }

    /**
     * <p>
     * Gets the user role table
     * </p>
     *
     * @return The user role {@link Table}
     */
    private Table getUserRoleTable() {
        return Table.get(MCRMUSERROLE.TABLE);
    }

    /**
     * <p>
     * Gets the user territory table
     * </p>
     *
     * @return The user territory {@link Table}
     */
    private Table getUserTerritoryTable() {
        return Table.get(MCRMUSERTERRITORY.TABLE);
    }

    /**
     * <p>
     * Gets the user team table
     * </p>
     *
     * @return The user team {@link Table}
     */
    private Table getUserTeamTable() {
        return Table.get(MCRMUSERTEAM.TABLE);
    }

    /**
     * <p>
     * Gets the user reporting manager table
     * </p>
     *
     * @return The user reporting manager {@link Table}
     */
    private Table getUserReportingManagerTable() {
        return Table.get(MCRMUSERREPORTINGMANAGER.TABLE);
    }

    /**
     * <p>
     * Gets the territory table
     * </p>
     *
     * @return The territory table
     */
    private Table getTerritoryTable() {
        return Table.get(OMCRMTERRITORY.TABLE);
    }

    /**
     * <p>
     * Gets the team table
     * </p>
     *
     * @return The team table
     */
    private Table getTeamTable() {
        return Table.get(OMCRMTEAM.TABLE);
    }

    /**
     * <p>
     * Gets the role table
     * </p>
     *
     * @return The role table
     */
    private Table getRoleTable() {
        return Table.get(OMCRMROLE.TABLE);
    }

    /**
     * <p>
     * Gets the user preference table
     * </p>
     *
     * @return The user preference table
     */
    private Table getUserPreferenceTable() {
        return Table.get(OMCRMUSERPREFERENCE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the collection of user ids
     * @return The collection of {@link CrmUser}
     */
    @Override
    public Collection<CrmUser> get(final Long orgId, final Collection<Long> ids) {
        return get(orgId, new WhereClause(Criteria.in(getTable().getPKColumn(), ids)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the user id
     * @return The {@link CrmUser} details
     */
    @Override
    public CrmUser get(final Long orgId, final Long id) {
        return get(orgId, Set.of(id)).stream().findAny().orElseGet(() -> null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the collection of user ids
     * @return if all the user ids are exists, It will return true otherwise it will return false
     */
    @Override
    public boolean exists(final Long orgId, final Collection<Long> ids) {
        final Table userTable = getTable();
        final CountQuery countQuery = CountQuery.get(userTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(userTable.getPKColumn(), ids)));

        return getDataStore(orgId).get(countQuery) == ids.size();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param roleIds Contains the collection of role ids
     * @return If all roles are mapped to at least a user, it will return true, otherwise it will return false
     */
    @Override
    public boolean rolesHaveUsers(final Long orgId, final Collection<Long> roleIds) {
        final Table userRoleTable = getUserRoleTable();
        final CountQuery countQuery = CountQuery.get(userRoleTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleIds)));

        return getDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param iamUserIds Represents the iam user id
     * @return The {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsersByIamId(final Long orgId, final Collection<Long> iamUserIds) {
        return get(orgId, new WhereClause(Criteria.in(getTable().getColumn(OMCRMUSER.IAM_USER_ID), iamUserIds)));
    }

    /**
     * Gets the users that matches the given {@link WhereClause} criteria
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the {@link WhereClause} criteria
     * @return The collection of {@link CrmUser} details
     */
    private Collection<CrmUser> getUsers(final Long orgId, final WhereClause whereClause) {
        final UserService userService = UserService.getInstance();
        final Collection<CrmUser> crmUsers = new ArrayList<>();
        final Table userTable = getTable();
        final Table userRoleTable = getUserRoleTable();
        final Table roleTable = Table.get(OMCRMROLE.TABLE);
        final Table reportingManagerTable = getUserReportingManagerTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Column rolePkColumn = roleTable.getPKColumn();
        final Column userIdColumn = userRoleTable.getColumn(MCRMUSERROLE.USER_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(userTable.getPKColumn(), userIdColumn),
                new JoinClause(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), rolePkColumn),
                new JoinClause(userTable.getPKColumn(), reportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.USER_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(userTable, userRoleTable, reportingManagerTable);
        selectQuery.addSelectClause(List.of(roleTable.getColumn(OMCRMROLE.NAME), roleTable.getColumn(OMCRMROLE.CONFIG)));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = getDataStore(orgId).get(selectQuery);

        dataContainer.getRows(userTable).forEach(row -> {
            final Long userId = row.getPKValue();
            final Long roleId = dataContainer.getRow(userRoleTable, new WhereClause(Criteria.eq(userIdColumn,
                    userId))).get(MCRMUSERROLE.ROLE_ID);
            final Row roleRow = dataContainer.getRow(roleTable, roleId);
            final CrmUserEntity crmUser = new CrmUserEntity();

            if (Objects.nonNull(roleRow)) {
                final CrmRole.Config config = new CrmRole.Config(roleRow.get(OMCRMROLE.CONFIG));
                final CrmRole role = new CrmRole();

                role.setName(roleRow.get(OMCRMROLE.NAME));
                role.setId(roleRow.getPKValue());
                role.setConfig(config);
                crmUser.setRole(role);
            }
            crmUser.setId(userId);
            crmUser.setIamId(row.get(OMCRMUSER.IAM_USER_ID));
            crmUser.setOrgId(row.getOrgId());
            final Row reportingManagerRow = dataContainer.getRow(reportingManagerTable, new WhereClause(Criteria
                    .eq(reportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.USER_ID), userId)));

            if (Objects.nonNull(reportingManagerRow)) {
                crmUser.setReportingManager(userService.getUser(orgId, reportingManagerRow
                        .get(MCRMUSERREPORTINGMANAGER.REPORTING_MANAGER_ID)));
            }
            crmUsers.add(crmUser);
        });

        return crmUsers;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param iamUserIds Represents the collection of iam user Ids
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUserListByIamId(final Long orgId, final Collection<Long> iamUserIds) {
        return getUsers(orgId, new WhereClause(Criteria.in(getTable().getColumn(OMCRMUSER.IAM_USER_ID), iamUserIds)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return The total number of users
     */
    @Override
    public Long totalUsers(final Long orgId) {
        final CountQuery countQuery = CountQuery.get(getTable());

        return getDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent A flag indicating whether the operation is triggered by a create event
     * @param crmUser       An entity that holds the {@link CrmUser} details
     */
    @Override
    public void upsert(final boolean isCreateEvent, final CrmUser crmUser) {
        final DataContainer targetContainer = DataContainer.create();

        loadUserDataIntoContainer(isCreateEvent, crmUser, targetContainer);
        getDataStore(crmUser.orgId()).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the user details into container for create or update operation
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether the operation is triggered by a create event
     * @param crmUser         An entity that holds the {@link CrmUser} details
     * @param targetContainer The {@link DataContainer} to load the user details
     */
    @Override
    public void loadUserDataIntoContainer(final boolean isCreateEvent, final CrmUser crmUser,
                                          final DataContainer targetContainer) {
        final Long orgId = crmUser.orgId();
        final Table crmUserTable = getTable();
        final Collection<Team> teams = crmUser.getTeams();
        final DataContainer sourceContainer = isCreateEvent ? null : fetchSourceContainer(crmUser);
        final Row row = isCreateEvent || Objects.isNull(crmUser.id()) ? new Row(crmUserTable) : sourceContainer.getRow(crmUserTable);
        final UserPreferenceHolder preferenceHolder = crmUser.getPreferenceHolder();
        final Long userId = row.getPKValue();
        final String userName = crmUser.getFullName();

        if (row.isNewRow()) {
            crmUser.setId(userId);
            row.setOrgId(orgId);
            row.set(OMCRMUSER.IAM_USER_ID, crmUser.getIamId());
            row.set(OMCRMUSER.PENDING_NOTIFICATION_COUNT, NumericalConstant.ZERO);
        }

        if (Objects.nonNull(userName)) {
            row.set(OMCRMUSER.NAME, userName);
        }

        final Long crmUserId = crmUser.id();
        final CrmUser reportingManager = crmUser.getReportingManager();

        loadUserRoleIntoContainer(isCreateEvent, crmUser, sourceContainer, targetContainer);

        if (Objects.nonNull(preferenceHolder)) {
            preferenceHolder.setUserId(userId);
            preferenceHolder.setUpdated(!isCreateEvent);
            preferenceManager.loadPreferencesIntoContainer(isCreateEvent, preferenceHolder, sourceContainer, targetContainer);
        }

        if (Objects.nonNull(reportingManager)) {
            final Long reportingManagerId = reportingManager.id();

            loadUserReportingManagerIntoContainer(isCreateEvent, crmUserId, reportingManagerId, sourceContainer, targetContainer);
        }

        if (Objects.nonNull(teams)) {
            loadUserTeamIntoContainer(orgId, isCreateEvent, crmUserId, teams.stream().map(Team::id).collect(Collectors.toSet()), sourceContainer, targetContainer);
        } else if (!isCreateEvent) {
            deleteUnmappedUserTeam(orgId, crmUserId, null, sourceContainer, targetContainer);
        }

        targetContainer.updateRow(row);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser} summary details
     */
    @Override
    public CrmUser getSummary(final Long orgId, final Long userId) {
        final Table userTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final Table userRoleTable = getUserRoleTable();
        final Table teamTable = getTeamTable();
        final Table territoryTable = getTerritoryTable();
        final Table roleTable = getRoleTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column userPkColumn = userTable.getPKColumn();

        selectQuery.addJoinClause(List.of(new JoinClause(userPkColumn, userRoleTable.getColumn(MCRMUSERROLE.USER_ID)),
                new JoinClause(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleTable.getPKColumn()),
                new JoinClause(userPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), leftJoin),
                new JoinClause(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamTable.getPKColumn(), leftJoin),
                new JoinClause(userPkColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), leftJoin),
                new JoinClause(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), territoryTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(teamTable.getColumn(OMCRMTEAM.NAME), territoryTable.getColumn(OMCRMTERRITORY.NAME),
                roleTable.getColumn(OMCRMROLE.NAME)));
        selectQuery.addSelectClause(userRoleTable, userTeamTable, userTerritoryTable, userTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userPkColumn, userId)));
        final DataContainer dataContainer = getDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(userTable);

        return convertRowToUserSummary(row, dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmUser Represents the {@link CrmUser} details
     */
    @Override
    public void partialUpdate(final CrmUser crmUser) {
        final long orgId = crmUser.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadPartialUserDataIntoContainer(orgId, crmUser, targetContainer);
        getDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the partial user data into container for update operation
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param crmUser         An entity that holds the {@link CrmUser} details to be updated
     * @param targetContainer The {@link DataContainer} to load the user details
     */
    @Override
    public void loadPartialUserDataIntoContainer(final long orgId, final CrmUser crmUser,
                                                 final DataContainer targetContainer) {
        final Collection<Team> teams = crmUser.getTeams();
        final DataContainer sourceContainer = fetchSourceContainer(crmUser);
        final Long crmUserId = crmUser.id();
        final CrmUser reportingManager = crmUser.reportingManager();
        final CrmRole crmRole = crmUser.getRole();
        final UserPreferenceHolder preferenceHolder = crmUser.getPreferenceHolder();
        final Row userRow = sourceContainer.getRow(getTable());

        if (Objects.nonNull(userRow)) {
            final String fullName = crmUser.getFullName();

            if (Objects.nonNull(fullName)) {
                userRow.set(OMCRMUSER.NAME, fullName);
            }
            targetContainer.updateRow(userRow);
        }

        if (Objects.nonNull(crmRole)) {
            loadUserRoleIntoContainer(false, crmUser, sourceContainer, targetContainer);
        }

        if (Objects.isNull(reportingManager)) {
            final Row reportingManagerRow = sourceContainer.getRow(getUserReportingManagerTable());

            if (Objects.nonNull(reportingManagerRow)) {
                targetContainer.deleteRow(reportingManagerRow);
            }
        } else {
            loadUserReportingManagerIntoContainer(false, crmUserId, reportingManager.id(), sourceContainer, targetContainer);
        }

        if (Objects.isNull(teams)) {
            deleteUnmappedUserTeam(orgId, crmUserId, null, sourceContainer, targetContainer);
        } else {
            loadUserTeamIntoContainer(orgId, false, crmUserId, teams.stream().map(Team::id).collect(Collectors.toSet()), sourceContainer, targetContainer);
        }

        if (Objects.nonNull(preferenceHolder)) {
            preferenceHolder.setOrgId(orgId);
            preferenceHolder.setUpdated(true);
            preferenceHolder.setUserId(crmUserId);
            preferenceManager.loadPreferencesIntoContainer(false, preferenceHolder, sourceContainer, targetContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return The collection of {@link CrmRole}
     */
    @Override
    public Collection<CrmRole> getRoles(final long orgId) {
        final Collection<CrmRole> crmRoles = new ArrayList<>();
        final Table roleTable = getRoleTable();
        final Table userRoleTale = getUserRoleTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final Column roleIdColumn = userRoleTale.getColumn(MCRMUSERROLE.ROLE_ID);
        final Column rolePkColumn = roleTable.getPKColumn();

        selectQuery.addSelectClause(List.of(roleTable.getColumn(OMCRMROLE.NAME), roleTable.getColumn(OMCRMROLE.CONFIG),
                userRoleTale.getColumn(MCRMUSERROLE.USER_ID), roleIdColumn));
        selectQuery.addJoinClause(new JoinClause(rolePkColumn, roleIdColumn));
        final DataContainer dataContainer = getDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(userRoleTale);

        rows.forEach(row -> {
            final Row roleRow = dataContainer.getRow(roleTable, new WhereClause(Criteria.eq(rolePkColumn,
                    row.get(MCRMUSERROLE.ROLE_ID))));

            if (Objects.nonNull(roleRow)) {
                final CrmRole crmRole = new CrmRole();
                final CrmRole.Config config = new CrmRole.Config(roleRow.get(OMCRMROLE.CONFIG));

                crmRole.setConfig(config);
                crmRole.setId(roleRow.getPKValue());
                crmRole.setName(roleRow.get(OMCRMROLE.NAME));
                crmRoles.add(crmRole);
            }
        });

        return crmRoles;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return the admin user ID
     */
    @Override
    public Long getAdminUserId(final Long orgId) {
        final Table roleTable = getRoleTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final Table userRoleTable = getUserRoleTable();

        selectQuery.addJoinClause(new JoinClause(roleTable.getPKColumn(), userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(roleTable.getColumn(OMCRMROLE.NAME), DefaultRole.ACCOUNT_ADMIN.getName())));
        selectQuery.addOrderByClause(new OrderByClause(userRoleTable.getPKColumn()));
        selectQuery.setLimitClause(new LimitClause(1));

        final Row userRoleRow = getDataStore(orgId).get(selectQuery).getRow(userRoleTable);

        return Objects.isNull(userRoleRow) ? null : userRoleRow.get(MCRMUSERROLE.USER_ID);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id.
     * @param userIds Represents an user ids.
     * @return True, if the given user ids contains reporting manager.
     */
    @Override
    public boolean isReportingManager(final Long orgId, final Collection<Long> userIds) {
        final Table reportingManagerTable = getUserReportingManagerTable();
        final CountQuery countQuery = CountQuery.get(reportingManagerTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(reportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.REPORTING_MANAGER_ID), userIds)));
        return 0 != getDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     */
    @Override
    public Long getOrgAccountCreatorId(final long orgId) {
        final Table crmUserTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(crmUserTable);
        final Column iamUserIdColumn = crmUserTable.getColumn(OMCRMUSER.IAM_USER_ID);

        selectQuery.addSelectClause(iamUserIdColumn);
        selectQuery.setLimitClause(new LimitClause(1));
        selectQuery.addOrderByClause(new OrderByClause(iamUserIdColumn));
        final Row row = getDataStore(orgId).get(selectQuery).getRow(crmUserTable);

        return Objects.isNull(row) ? null : row.get(OMCRMUSER.IAM_USER_ID);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgIds Represents the collection of org id.
     * @return the map of primary user id (crm user)
     */
    @Override
    public Map<Long, Long> getPrimaryUserMap(final Collection<Long> orgIds) {
        final Table crmUserTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(crmUserTable);
        final Column orgIdColumn = crmUserTable.getColumn(OMCRMUSER.ORG_ID);

        selectQuery.addSelectClause(List.of(crmUserTable.getPKColumn().min(), orgIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(orgIdColumn, orgIds)));
        selectQuery.addGroupByClause(new GroupByClause(orgIdColumn));

        return adminDataStore().get(selectQuery).getRows(crmUserTable).collect(Collectors.toMap(Row::getOrgId, Row::getPKValue));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the org id
     * @return the collection of user id
     */
    @Override
    public Collection<Long> getUserIds(final Long orgId) {
        final Table crmUserTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(crmUserTable);
        final Column orgIdColumn = crmUserTable.getColumn(OMCRMUSER.ORG_ID);

        selectQuery.addSelectClause(List.of(crmUserTable.getPKColumn(), orgIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(orgIdColumn, orgId)));

        return getDataStore(orgId).get(selectQuery).getRows(crmUserTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Saves the given role details into data base
     * </p>
     *
     * @param orgId         Represents an org id
     * @param crmUserEntity Represents the {@link CrmUser} details
     */
    @Override
    public void create(final long orgId, final CrmUserEntity crmUserEntity) {
        final DataContainer dataContainer = DataContainer.create();
        final RoleManagerImpl roleManager = RoleManagerImpl.getInstance();
        final Collection<CrmRole> roles = roleBuilder.build(DefaultRole.values());

        crmUserEntity.setOrgId(orgId);
        loadUserIntoContainer(crmUserEntity, dataContainer);

        for (final CrmRole crmRole : roles) {
            if (crmRole.getConfig().isAdmin()) {
                crmRole.addUser(crmUserEntity);
            }
            crmRole.setOrgId(orgId);
            roleManager.loadRoleIntoContainer(true, crmRole, DataContainer.create(), dataContainer);
        }
        getDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Converts {@link Row} into {@link CrmUser} details
     * </p>
     *
     * @param row A row represents the {@link CrmUser} details
     * @return The {@link CrmUser} details
     */
    @Override
    public CrmUser convertRowToModel(final Row row) {
        final CrmUserEntity crmUser = new CrmUserEntity();
        final User user = new User();

        crmUser.setIamId(row.get(OMCRMUSER.IAM_USER_ID));
        crmUser.setId(row.getPKValue());
        crmUser.setOrgId(row.getOrgId());
        user.setFirstName(row.get(OMCRMUSER.NAME));
        crmUser.setUser(user);

        return crmUser;
    }

    /**
     * <p>
     * Gets {@link CrmUser} reporting manager id.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the id of the user.
     * @return reporting manager id.
     */
    @Override
    public Long getReportingManagerId(final Long orgId, final Long id) {
        final Table userReportingManagerTable = getUserReportingManagerTable();
        final SelectQuery selectQuery = SelectQuery.get(userReportingManagerTable);

        selectQuery.addSelectClause(userReportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.REPORTING_MANAGER_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userReportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.USER_ID), id)));
        final Row row = getDataStore(orgId).get(selectQuery).getRow(userReportingManagerTable);

        return Objects.isNull(row) ? null : row.get(MCRMUSERREPORTINGMANAGER.REPORTING_MANAGER_ID);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser}
     */
    @Override
    public CrmUser getCrmUser(final Long orgId, final Long userId) {
        final Table crmUserTable = getTable();
        final Table crmUserRoleTable = getUserRoleTable();
        final Table crmUserTeamTable = getUserTeamTable();
        final Table userReportingManagerTable = getUserReportingManagerTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final Table userPreferenceTable = getUserPreferenceTable();
        final Table preferenceTable = preferenceManager.getTable();
        final Column crmUserPKColumn = crmUserTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(crmUserTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(crmUserPKColumn, crmUserRoleTable.getColumn(MCRMUSERROLE.USER_ID)),
                new JoinClause(crmUserPKColumn, userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.USER_ID)),
                new JoinClause(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.PREFERENCE_ID), preferenceTable.getPKColumn()),
                new JoinClause(crmUserPKColumn, crmUserTeamTable.getColumn(MCRMUSERTEAM.USER_ID), leftJoin),
                new JoinClause(crmUserPKColumn, userReportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.USER_ID), leftJoin),
                new JoinClause(crmUserPKColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(crmUserPKColumn, userId)));
        final DataContainer dataContainer = getDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            throw ErrorCode.get(IamErrorCodes.CRM_USER_NOT_FOUND, "i18n.common.user.not.found");
        }

        return convertRowToModel(dataContainer.getRow(crmUserTable), dataContainer);
    }

    /**
     * <p>
     * Converts {@link Row} into {@link CrmUser} summary details
     * </p>
     *
     * @param row           A row represents the {@link CrmUser} details
     * @param dataContainer The data container containing existing user information
     * @return The {@link CrmUser} details
     */
    private CrmUser convertRowToUserSummary(final Row row, final DataContainer dataContainer) {
        final CrmUserEntity crmUser = new CrmUserEntity();
        final Table territoryTable = getTerritoryTable();
        final Table teamTable = getTeamTable();
        final Collection<Territory> territories = new HashSet<>();
        final Collection<Team> teams = new HashSet<>();
        final Row roleRow = dataContainer.getRow(getRoleTable());
        final CrmRole crmRole = new CrmRole();

        dataContainer.getRows(teamTable).forEach(teamRow -> {
            final Team team = new Team();

            team.setId(teamRow.getPKValue());
            team.setName(teamRow.get(OMCRMTEAM.NAME));
            teams.add(team);
        });

        dataContainer.getRows(territoryTable).forEach(territoryRow -> {
            final Territory territory = new Territory();

            territory.setId(territoryRow.getPKValue());
            territory.setName(territoryRow.get(OMCRMTERRITORY.NAME));
            territories.add(territory);
        });
        crmRole.setId(roleRow.getPKValue());
        crmRole.setName(roleRow.get(OMCRMROLE.NAME));
        crmUser.setRole(crmRole);
        crmUser.setId(row.getPKValue());
        crmUser.setIamId(row.get(OMCRMUSER.IAM_USER_ID));
        crmUser.setTeams(teams);
        crmUser.setTerritories(territories);
        crmUser.setOrgId(row.getOrgId());

        return crmUser;
    }

    /**
     * <p>
     * Loads user reporting manager into the target container
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether the operation is triggered by a create event.
     * @param crmUserId       Represents the crm user id
     * @param sourceContainer The source data container containing existing user team information.
     * @param targetContainer The target data container where the user team information will be loaded
     */
    private void loadUserReportingManagerIntoContainer(final boolean isCreateEvent, final Long crmUserId, final Long reportingManagerId,
                                                       final DataContainer sourceContainer, DataContainer targetContainer) {
        final Table crmUserReportingManagerTable = getUserReportingManagerTable();
        final Row row;

        if (isCreateEvent) {
            row = new Row(crmUserReportingManagerTable);
        } else {
            final Row existingRow = sourceContainer.getRow(crmUserReportingManagerTable,
                    new WhereClause(Criteria.eq(crmUserReportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.USER_ID), crmUserId)));

            row = Objects.isNull(existingRow) ? new Row(crmUserReportingManagerTable) : existingRow;
        }

        if (row.isNewRow()) {
            row.set(MCRMUSERREPORTINGMANAGER.USER_ID, crmUserId);
        }
        row.set(MCRMUSERREPORTINGMANAGER.REPORTING_MANAGER_ID, reportingManagerId);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Fetches the source data container containing user-related information for the given {@link CrmUser}
     * </p>
     *
     * @param crmUser An entity that holds the {@link CrmUser} details
     * @return A {@link DataContainer} containing existing user-related data for the specified {@link CrmUser}
     */
    private DataContainer fetchSourceContainer(final CrmUser crmUser) {
        final Table crmUserTable = getTable();
        final Table crmUserRoleTable = getUserRoleTable();
        final Table crmUserTeamTable = getUserTeamTable();
        final Table userReportingManagerTable = getUserReportingManagerTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final Table userPreferenceTable = getUserPreferenceTable();
        final Table preferenceTable = preferenceManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(crmUserTable);
        final Column crmUserTablePKColumn = crmUserTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(crmUserTablePKColumn, crmUserRoleTable.getColumn(MCRMUSERROLE.USER_ID)),
                new JoinClause(crmUserTablePKColumn, crmUserTeamTable.getColumn(MCRMUSERTEAM.USER_ID), leftJoin),
                new JoinClause(crmUserTablePKColumn, userReportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.USER_ID), leftJoin),
                new JoinClause(crmUserTablePKColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), leftJoin),
                new JoinClause(crmUserTablePKColumn, userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.USER_ID), leftJoin),
                new JoinClause(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.PREFERENCE_ID), preferenceTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(crmUserTable, crmUserRoleTable, crmUserTeamTable, userReportingManagerTable, userTerritoryTable, userPreferenceTable, preferenceTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(crmUserTablePKColumn, crmUser.id())));

        return getDataStore(crmUser.orgId()).get(selectQuery);
    }

    /**
     * <p>
     * Loads user team information into the target container based on the provided crm user id and team ids
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param isCreateEvent   A flag indicating whether the operation is triggered by a create event.
     * @param crmUserId       Represents the crm user id
     * @param teamIds         Represents the collection of team ids
     * @param sourceContainer The source data container containing existing user team information.
     * @param targetContainer The target data container where the user team information will be loaded
     */
    private void loadUserTeamIntoContainer(final long orgId, final boolean isCreateEvent, final Long crmUserId, final Collection<Long> teamIds,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table crmUserTeamTable = getUserTeamTable();
        final Column crmUserTeamIdcolumn = crmUserTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID);

        for (final Long teamId : teamIds) {
            final Row row;

            if (isCreateEvent) {
                row = new Row(crmUserTeamTable);
            } else {
                final Row existingRow = sourceContainer.getRow(crmUserTeamTable,
                        new WhereClause(Criteria.eq(crmUserTeamTable.getColumn(MCRMUSERTEAM.USER_ID), crmUserId))
                                .and(Criteria.eq(crmUserTeamIdcolumn, teamId)));
                row = Objects.isNull(existingRow) ? new Row(crmUserTeamTable) : existingRow;
            }
            row.set(MCRMUSERTEAM.TEAM_ID, teamId);

            if (row.isNewRow()) {
                row.set(MCRMUSERTEAM.USER_ID, crmUserId);
            }

            targetContainer.updateRow(row);
        }

        if (!isCreateEvent) {
            deleteUnmappedUserTeam(orgId, crmUserId, Criteria.notIn(crmUserTeamIdcolumn, teamIds), sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Deletes unmapped user team entries from the target container based on the provided criteria and source container
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param crmUserId       Represents the crm user id.
     * @param criteria        The criteria for identifying unmapped user team entries.
     * @param sourceContainer The source data container containing existing user team entries.
     * @param targetContainer The target data container from which unmapped entries will be deleted.
     */
    private void deleteUnmappedUserTeam(final long orgId, final Long crmUserId, final Criteria criteria, final DataContainer sourceContainer,
                                        final DataContainer targetContainer) {
        final Table userTeamTable = getUserTeamTable();
        final Table teamTable = getTeamTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), crmUserId));
        final Stream<Row> userTeamRows = sourceContainer.getRows(userTeamTable, Objects.isNull(criteria) ?
                whereClause : whereClause.and(criteria));
        final Collection<Long> teamIdsToBeDeleted = new HashSet<>();
        final Column teamIdColumn = userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID);

        userTeamRows.forEach(userTeamRow -> {
            teamIdsToBeDeleted.add(userTeamRow.get(MCRMUSERTEAM.TEAM_ID));
            targetContainer.deleteRow(userTeamRow);
        });

        if (!teamIdsToBeDeleted.isEmpty()) {
            final SelectQuery selectQuery = SelectQuery.get(userTeamTable);

            selectQuery.addSelectClause(List.of(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID).count(), teamIdColumn));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(teamIdColumn, teamIdsToBeDeleted)));
            selectQuery.addGroupByClause(new GroupByClause(teamIdColumn));

            for (final Map<String, Object> teamUserMap : getDataStore(orgId).getData(selectQuery)) {
                final Long teamId = (Long) teamUserMap.get(MCRMUSERTEAM.TEAM_ID);
                final Long userCount = (Long) teamUserMap.get(MCRMUSERTEAM.USER_ID);

                if (Objects.equals(MIN_USER_COUNT, userCount.shortValue())) {
                    targetContainer.deleteRow(new Row(teamTable, teamId));
                }
            }
        }
    }


    /**
     * <p>
     * Loads user role information into the target container based on the provided CrmUser and source container
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether the operation is triggered by a create event.
     * @param crmUser         An entity that holds the {@link CrmUser} details
     * @param sourceContainer The source data container containing existing user role information.
     * @param targetContainer The target data container where the user role information will be loaded.
     */
    private void loadUserRoleIntoContainer(final boolean isCreateEvent, final CrmUser crmUser, final DataContainer sourceContainer,
                                           final DataContainer targetContainer) {
        final Table table = getUserRoleTable();
        final Long crmUserId = crmUser.id();
        final CrmRole role = crmUser.getRole();
        final Row row;

        if (isCreateEvent) {
            row = new Row(table);

            row.set(MCRMUSERROLE.USER_ID, crmUserId);
        } else {
            final long orgId = crmUser.orgId();
            row = sourceContainer.getRow(getUserRoleTable(),
                    new WhereClause(Criteria.eq(table.getColumn(MCRMUSERROLE.USER_ID), crmUserId)));

            role.setOrgId(orgId);
            role.setUserIds(List.of(crmUserId));
            role.setName(RoleManager.getInstance().getRoleByUser(orgId, crmUserId).getName());
        }
        row.set(MCRMUSERROLE.ROLE_ID, role.id());

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Loads {@link CrmUser} details into container
     * </p>
     *
     * @param crmUserEntity Represents the {@link CrmUser} details
     * @param dataContainer A container that holds the user defined field values and system values
     */
    private void loadUserIntoContainer(final CrmUserEntity crmUserEntity, final DataContainer dataContainer) {
        final Row row = new Row(getTable());
        final Long orgId = crmUserEntity.orgId();
        final Long userId = row.getPKValue();

        crmUserEntity.setId(userId);
        row.set(OMCRMUSER.IAM_USER_ID, crmUserEntity.getIamId());
        row.set(OMCRMUSER.NAME, crmUserEntity.getFullName());
        row.setOrgId(orgId);
        row.set(OMCRMUSER.PENDING_NOTIFICATION_COUNT, NumericalConstant.ZERO);
        dataContainer.addNewRow(row);
    }

    /**
     * <p>
     * Converts row into {@link CrmUser}
     * </p>
     *
     * @param row           Represents the {@link Row}
     * @param dataContainer Holds the user data
     * @return The {@link CrmUser}
     */
    private CrmUser convertRowToModel(final Row row, final DataContainer dataContainer) {
        final UserService userService = UserService.getInstance();
        final Table userRoleTable = getUserRoleTable();
        final Table userTeamTable = getUserTeamTable();
        final Table userReportingManagerTable = getUserReportingManagerTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final Table userPreferenceTable = getUserPreferenceTable();
        final Long userId = row.getPKValue();
        final Long orgId = row.getOrgId();
        final Table teamTable = getTeamTable();
        final Row reportingManagerRow = dataContainer.getRow(userReportingManagerTable,
                new WhereClause(Criteria.eq(userReportingManagerTable.getColumn(MCRMUSERREPORTINGMANAGER.USER_ID), userId)));
        final Stream<Row> userTeamRows = dataContainer.getRows(userTeamTable,
                new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userId)));
        final CrmUserEntity crmUser = new CrmUserEntity();
        final Collection<Team> teams = new ArrayList<>();
        final Collection<Territory> territories = new HashSet<>();
        final Row userRoleRow = dataContainer.getRow(userRoleTable,
                new WhereClause(Criteria.eq(userRoleTable.getColumn(MCRMUSERROLE.USER_ID), userId)));
        final Stream<Row> userTerritoryRows = dataContainer.getRows(userTerritoryTable,
                new WhereClause(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userId)));
        final Collection<Long> teamIds = userTeamRows.map(teamRow -> (Long) teamRow.get(MCRMUSERTEAM.TEAM_ID)).toList();
        final Optional<Row> optionalUserPreferenceRow = Optional.ofNullable(dataContainer.getRow(userPreferenceTable,
                new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.USER_ID), userId))));
        final Optional<Row> optionalPreferenceRow = optionalUserPreferenceRow.map(userPreferenceRow -> dataContainer.getRow(preferenceManager.getTable(),
                (Long) userPreferenceRow.get(OMCRMUSERPREFERENCE.PREFERENCE_ID)));
        final User user = new User();

        if (Objects.nonNull(userRoleRow)) {
            final CrmRole crmRole = new CrmRole();

            crmRole.setId(userRoleRow.get(MCRMUSERROLE.ROLE_ID));
            crmUser.setRole(crmRole);
        }
        crmUser.setIamId(row.get(OMCRMUSER.IAM_USER_ID));
        crmUser.setId(userId);
        crmUser.setOrgId(orgId);

        if (Objects.nonNull(reportingManagerRow)) {
            crmUser.setReportingManager(userService.getUser(orgId,
                    reportingManagerRow.get(MCRMUSERREPORTINGMANAGER.REPORTING_MANAGER_ID)));
        }

        if (dataContainer.size(teamTable) > 0) {
            final Stream<Row> teamRows = dataContainer.getRows(teamTable, new WhereClause(Criteria
                    .in(teamTable.getPKColumn(), teamIds)));

            teamRows.forEach(teamRow -> {
                final Team team = new Team();

                team.setId(teamRow.getPKValue());
                team.setName(teamRow.get(OMCRMTEAM.NAME));
                teams.add(team);
            });
        } else {
            for (final Long teamId : teamIds) {
                final Team team = new Team();

                team.setId(teamId);
                teams.add(team);
            }
        }

        userTerritoryRows.forEach(userTerritoryRow -> {
            final Territory territory = new Territory();

            territory.setId(userTerritoryRow.get(MCRMUSERTERRITORY.TERRITORY_ID));
            territories.add(territory);
        });
        crmUser.setTeams(teams);
        crmUser.setTerritories(territories);
        optionalPreferenceRow.ifPresent(preferenceRow -> crmUser.setPreferenceHolder(((UserPreferenceManagerImpl) preferenceManager)
                .convertRowToModel(userId, preferenceRow)));
        user.setFirstName(row.get(OMCRMUSER.NAME));
        user.setOrgId(orgId);
        crmUser.setUser(user);

        return crmUser;
    }

    /**
     * <p>
     * Fetches the collection of {@link CrmUser} for the given org id, criteria
     * </p>
     *
     * @param orgId       Represents an org id
     * @param whereClause Which contains the criteria to fetch the user data
     * @return The collection of {@link CrmUser}
     */
    private Collection<CrmUser> get(final long orgId, final WhereClause whereClause) {
        final Table userTable = getTable();
        final Table userTeamTable = getUserTeamTable();
        final Table teamTable = getTeamTable();
        final Table userPreferenceTable = getUserPreferenceTable();
        final Table preferenceTable = preferenceManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(userTable, userPreferenceTable, preferenceTable, userTeamTable, teamTable);
        selectQuery.addJoinClause(List.of(new JoinClause(userTable.getPKColumn(), userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.USER_ID), leftJoin),
                new JoinClause(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.PREFERENCE_ID), preferenceTable.getPKColumn(), leftJoin),
                new JoinClause(userTable.getPKColumn(), userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), leftJoin),
                new JoinClause(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), teamTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = getDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(userTable);

        return rows.map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Provides the {@link OrgDataStore} for the respective organisation.
     * </p>
     *
     * @param orgId Represents the organisation ID.
     * @return {@link OrgDataStore}
     */
    protected abstract OrgDataStore getDataStore(final long orgId);

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param userNames Represents the collection of user names
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsersByNames(final Long orgId, final Collection<String> userNames) {
        return getUsers(orgId, new WhereClause(Criteria.in(getTable().getColumn(OMCRMUSER.NAME), userNames)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Represents an organization id
     * @param criteriaDetailsHolder  Holds the filtering criteria for the count query
     * @param excludeCrmUserIds      CRM user ids to exclude from the result
     * @return The total number of matching users
     */
    @Override
    public long getUsersCount(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder,
                              final Collection<Long> excludeCrmUserIds) {
        final CountQuery countQuery = CountQuery.get(getTable());

        countQuery.setWhereClause(buildUserCountWhereClause(criteriaDetailsHolder, excludeCrmUserIds));

        return getDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Represents an organization id
     * @param criteriaDetailsHolder  Holds the filtering criteria
     * @param excludeCrmUserIds      CRM user ids to exclude from the result
     * @return IAM user ids in stable order
     */
    @Override
    public Collection<Long> getIamUserIds(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder,
                                          final Collection<Long> excludeCrmUserIds) {
        final Table userTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Column iamUserIdColumn = userTable.getColumn(OMCRMUSER.IAM_USER_ID);
        final Column userPkColumn = userTable.getPKColumn();

        selectQuery.addSelectClause(List.of(userPkColumn, iamUserIdColumn));
        selectQuery.setWhereClause(buildUserCountWhereClause(criteriaDetailsHolder, excludeCrmUserIds));
        selectQuery.addOrderByClause(new OrderByClause(userPkColumn));

        return getDataStore(orgId).get(selectQuery).getRows(userTable)
                .map(row -> (Long) row.get(OMCRMUSER.IAM_USER_ID))
                .collect(Collectors.toList());
    }

    /**
     * Builds a where clause for CRM user count and search requests.
     *
     * @param criteriaDetailsHolder Holds the filtering criteria
     * @param excludeCrmUserIds     CRM user ids to exclude
     * @return The {@link WhereClause}
     */
    private WhereClause buildUserCountWhereClause(final CriteriaDetailsHolder criteriaDetailsHolder,
                                                  final Collection<Long> excludeCrmUserIds) {
        final Table userTable = getTable();
        final Column userPkColumn = userTable.getPKColumn();
        final WhereClauseBuilder whereClauseBuilder = new WhereClauseBuilder(
                criteriaDetailsHolder.getCriteria(this::getUserSearchColumn).orElse(null));

        if (!excludeCrmUserIds.isEmpty()) {
            whereClauseBuilder.append(new WhereClause(Criteria.notIn(userPkColumn, excludeCrmUserIds)), Connector.AND);
        }

        return whereClauseBuilder.getWhereClause();
    }

    /**
     * Resolves a field name string into a corresponding {@link Column} on the CRM user table.
     *
     * @param field the API field name used in filter criteria
     * @return the corresponding column for filtering
     */
    private Column getUserSearchColumn(final String field) {
        final Table userTable = getTable();

        if (CrmUserFields.NAME.getName().equalsIgnoreCase(field)) {
            return userTable.getColumn(OMCRMUSER.NAME);
        }

        throw ErrorCode.get(IamErrorCodes.FIELD_IS_INVALID, "i18n.common.field.is.invalid");
    }
}
