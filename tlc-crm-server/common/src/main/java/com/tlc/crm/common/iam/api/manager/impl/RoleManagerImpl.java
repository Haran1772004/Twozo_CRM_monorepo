package com.tlc.crm.common.iam.api.manager.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.internal.manager.AbstractIamConfigManager;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.CrmRole;

import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;

import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.crm.security.permission.manager.FieldPermissionManager;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.crm.security.sql.resource.OIAMPFIELDPERMISSIONS;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * A {@link RoleManagerImpl} that provides the various methods for the {@link RoleManager}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class RoleManagerImpl extends AbstractIamConfigManager<CrmRole> implements RoleManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RoleManagerImpl.class);

    private final CrmUserManagerImpl crmUserManager;
    private final UserService userService;
    private final DateTimeUtil dateTimeUtil;
    private final FieldPermissionManager fieldPermissionManager;

    private RoleManagerImpl() {
        crmUserManager = CrmUserManagerImpl.getInstance();
        userService = UserService.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        fieldPermissionManager = FieldPermissionManager.getInstance();
    }

    private static class Instance {
        private static final RoleManagerImpl INSTANCE = new RoleManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of the {@link RoleManagerImpl}
     * </p>
     *
     * @return The instance of {@link RoleManagerImpl}
     */
    public static RoleManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The role {@link Table}
     */
    @Override
    public Table getTable() {
        return Table.get(OMCRMROLE.TABLE);
    }

    /**
     * <p>
     * Gets the user table
     * </p>
     *
     * @return The user {@link Table}
     */
    private Table getUserTable() {
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
     * {@inheritDoc}
     *
     * @param role Represents the role details
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole create(final CrmRole role) {
        final DataContainer targetContainer = DataContainer.create();
        final Collection<CrmUser> crmUsers = role.getCrmUsers();
        final Table userRoleTable = getUserRoleTable();
        final Long orgId = role.orgId();
        final DataContainer sourceContainer = Optional.ofNullable(crmUsers).filter(users -> !users.isEmpty()).map(users -> {
            final WhereClause whereClause = new WhereClause(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.USER_ID),
                    users.stream().map(TlcModel::id).collect(Collectors.toSet())));

            return orgDataStore(orgId).get(userRoleTable, whereClause);
        }).orElseGet(DataContainer::create);

        loadRoleIntoContainer(true, role, sourceContainer, targetContainer);
        secureOrgDataStore(orgId).commitChanges(targetContainer);

        return role;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param roles Contains the collection of {@link CrmRole}
     * @return The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome partialUpdate(final Long orgId, final Collection<CrmRole> roles) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Table roleTable = getTable();
        final Column rolePkColumn = roleTable.getPKColumn();

        for (final CrmRole role : roles) {

            try {
                final UpdateQuery updateQuery = UpdateQuery.get(roleTable);
                final Long roleId = role.id();

                updateQuery.addUpdateColumn(roleTable.getColumn(OMCRMROLE.NAME), role.getName());
                updateQuery.addUpdateColumn(roleTable.getColumn(OMCRMROLE.UPDATED_BY), role.getUpdatedBy().id());
                updateQuery.addUpdateColumn(roleTable.getColumn(OMCRMROLE.UPDATED_TIME), dateTimeUtil.getTimeStamp(role.getUpdatedTime()));
                updateQuery.setWhereClause(new WhereClause(Criteria.eq(rolePkColumn, roleId)));
                secureOrgDataStore(orgId).update(updateQuery);
                processOutcome.addProcessedRecord(roleId);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Role update failed Org ID : {} ID : {} reason : {}", orgId, role.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.ROLE_UPDATE_FAILED,
                        "i18n.role.update.action.failed"), ErrorKeys.UPDATE_ACTION_FAILED, role.id().toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.ROLE_UPDATE_FAILED,
                    "i18n.role.update.action.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     * @param roles Contains the collection of roles
     * @return The updated response sent
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<CrmRole> roles) {
        final Table userRoleTable = getUserRoleTable();
        final Table roleTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<ErrorDescriptor> failedEntities = new ArrayList<>();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> userIds = new HashSet<>();
        final Collection<Long> ids = new HashSet<>();
        final Column rolePkColumn = roleTable.getPKColumn();

        for (final CrmRole crmRole : roles) {
            final Collection<CrmUser> crmUsers = crmRole.getCrmUsers();

            if (Objects.nonNull(crmUsers)) {
                userIds.addAll(crmUsers.stream().map(TlcModel::id).collect(Collectors.toSet()));
            }
            ids.add(crmRole.id());
        }
        selectQuery.addJoinClause(new JoinClause(rolePkColumn, fieldPermissionManager.getTable().getColumn(OIAMPFIELDPERMISSIONS.ROLE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(rolePkColumn, ids)));
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        if (!userIds.isEmpty()) {
            dataContainer.append(orgDataStore.get(userRoleTable, new WhereClause(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.USER_ID), userIds))));
        }

        roles.forEach(role -> {
            try {
                loadRoleIntoContainer(false, role, dataContainer, targetContainer);

                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(role.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Role update failed Org ID : {} ID : {} reason : {}", orgId, role.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.ROLE_UPDATE_FAILED,
                        "i18n.role.update.action.failed"), ErrorKeys.UPDATE_ACTION_FAILED, role.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.ROLE_UPDATE_FAILED,
                    "i18n.role.update.action.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the role id
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole get(final Long orgId, final Long id) {
        final Row row = orgDataStore(orgId).get(getTable(), id);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }

        return convertRowToModel(row, null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the collection of role ids
     * @param names Contains the collection of role names
     * @return If any role is exists it will return true, otherwise it will return false
     */
    @Override
    public boolean checkRolesAreExists(final Long orgId, final Collection<Long> ids, final Collection<String> names) {
        final Table roleTable = getTable();
        final CountQuery countQuery = CountQuery.get(roleTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(roleTable.getColumn(OMCRMROLE.NAME), names)).
                and(Criteria.notIn(roleTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index of role to be fetched
     * @param limit Represents the maximum number of role to be fetched
     * @return The collection of {@link CrmRole} details
     */
    @Override
    public Collection<CrmRole> get(final Long orgId, final int start, final int limit) {
        final Collection<CrmRole> roles = new ArrayList<>();
        final Table roleTable = getTable();
        final Table userTable = getUserTable();
        final SelectQuery innerSelectQuery = SelectQuery.get(roleTable);
        final Table userRoleTable = getUserRoleTable();
        final Column rolePkColumn = roleTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column userRoleIdColumn = userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID);
        final Column roleUserIdColumn = userRoleTable.getColumn(MCRMUSERROLE.USER_ID);

        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        innerSelectQuery.addSelectClause(rolePkColumn);
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(roleTable.getColumn(OMCRMROLE.ORG_ID), orgId)));
        final SelectQuery selectQuery = SelectQuery.get(roleTable);

        selectQuery.addJoinClause(List.of(new JoinClause(rolePkColumn, userRoleIdColumn, leftJoin),
                new JoinClause(roleUserIdColumn, userTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(roleUserIdColumn, userRoleIdColumn, userTable.getColumn(OMCRMUSER.IAM_USER_ID)));
        selectQuery.addSelectClause(roleTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(rolePkColumn, innerSelectQuery)));
        selectQuery.addOrderByClause(new OrderByClause(roleTable.getColumn(OMCRMROLE.CREATED_TIME), OrderByClause.OrderType.ASCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(roleTable).forEach(row -> {
            final Long roleId = row.getPKValue();
            final String roleConfig = row.get(OMCRMROLE.CONFIG);
            final CrmRole.Config config = new CrmRole.Config(roleConfig);
            final CrmRole role = new CrmRole();

            loadCreatedByAndUpdatedByIntoRole(orgId, row, role);
            role.setId(roleId);
            role.setName(row.get(OMCRMTEAM.NAME));
            role.setCreatedTime(row.get(OMCRMROLE.CREATED_TIME));
            role.setUpdatedTime(row.get(OMCRMROLE.UPDATED_TIME));
            role.setUsersCount(dataContainer.getRows(userRoleTable,
                    new WhereClause(Criteria.eq(userRoleIdColumn, roleId))).count());
            role.setConfig(config);

            roles.add(role);
        });

        return roles;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param roleId Represents the role id
     * @return The total number of users
     */
    @Override
    public long getUsersCount(final Long orgId, final Long roleId) {
        final Table userRoleTable = getUserRoleTable();
        final CountQuery countQuery = CountQuery.get(userRoleTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param roleId         Represents the role id
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole get(final Long orgId, final Long roleId, final int userStartValue, final int userLimit) {
        final Table roleTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final Table userRoleTable = getUserRoleTable();
        final Table userTable = getUserTable();
        final Column rolePkColumn = roleTable.getPKColumn();
        final Column userPkColumn = userTable.getPKColumn();
        final Column roleIdColumn = userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID);
        final Column userIdColumn = userRoleTable.getColumn(MCRMUSERROLE.USER_ID);

        selectQuery.setLimitClause(new LimitClause(userStartValue, userLimit));
        selectQuery.addJoinClause(List.of(new JoinClause(rolePkColumn, roleIdColumn), new JoinClause(userIdColumn, userPkColumn)));
        selectQuery.addSelectClause(List.of(roleIdColumn, userIdColumn, roleTable.getColumn(OMCRMROLE.NAME), userTable.getColumn(OMCRMUSER.ORG_ID),
                userTable.getColumn(OMCRMUSER.IAM_USER_ID), roleTable.getColumn(OMCRMROLE.PERMISSIONS), roleTable.getColumn(OMCRMROLE.CONFIG), roleTable.getColumn(OMCRMROLE.ORG_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(rolePkColumn, roleId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(roleTable);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }

        return convertRowToModel(row, dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param limitClause Represents the start and limit the role to be fetched
     * @param criteria    Represents the criteria for role filtering
     * @return The collection of {@link CrmRole} details
     */
    @Override
    public Collection<CrmRole> getRoleDropDownList(final Long orgId, final LimitClause limitClause, final Criteria criteria) {
        final Collection<CrmRole> roles = new ArrayList<>();
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMCRMROLE.NAME));
        selectQuery.setWhereClause(new WhereClause(criteria));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn()));
        selectQuery.setLimitClause(limitClause);
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(table);

        rows.forEach(row -> {
            final CrmRole crmUserRole = new CrmRole();

            crmUserRole.setId(row.getPKValue());
            crmUserRole.setName(row.get(OMCRMROLE.NAME));
            roles.add(crmUserRole);
        });

        return roles;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param names Contains the collection of role names
     * @return If any role is exists it will return true, otherwise it will return false
     */
    public boolean checkRolesAreExists(final Long orgId, final Collection<String> names) {
        final Table roleTable = getTable();
        final CountQuery countQuery = CountQuery.get(roleTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(roleTable.getColumn(OMCRMROLE.NAME), names)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param roleId         Represents the role id
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsers(final Long orgId, final Long roleId, final int userStartValue, final int userLimit) {
        final Table roleTable = getTable();
        final Table userTable = getUserTable();
        final Table userRoleTable = getUserRoleTable();
        final Column rolePkColumn = roleTable.getPKColumn();
        final Column roleUserIdColumn = userRoleTable.getColumn(MCRMUSERROLE.USER_ID);
        final SelectQuery selectQuery = SelectQuery.get(roleTable);

        selectQuery.addJoinClause(List.of(new JoinClause(rolePkColumn, userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID)),
                new JoinClause(roleUserIdColumn, userTable.getPKColumn())));
        selectQuery.addSelectClause(userTable);
        selectQuery.addSelectClause(List.of(rolePkColumn, roleUserIdColumn));
        selectQuery.setLimitClause(new LimitClause(userStartValue, userLimit));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(rolePkColumn, roleId)));

        return orgDataStore(orgId).get(selectQuery).getRows(userTable).map(crmUserManager::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the role ids
     * @return The collection of {@link CrmRole}
     */
    @Override
    public Collection<CrmRole> get(final long orgId, final Collection<Long> ids) {
        final Table roleTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final Collection<CrmRole> crmRoles = new ArrayList<>();

        selectQuery.addSelectClause(List.of(roleTable.getColumn(OMCRMROLE.CONFIG), roleTable.getColumn(OMCRMROLE.NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(roleTable.getPKColumn(), ids)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(roleTable);

        rows.forEach(row -> {
            final CrmRole crmRole = new CrmRole();

            crmRole.setConfig(new CrmRole.Config(row.get(OMCRMROLE.CONFIG)));
            crmRole.setName(roleTable.getName());
            crmRole.setId(row.getPKValue());
            crmRoles.add(crmRole);
        });

        return crmRoles;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id.
     * @param userIds Represents the user id.
     * @return The map of user id and {@link CrmRole}.
     */
    @Override
    public Map<Long, CrmRole> getRolesByUsers(final Long orgId, final Collection<Long> userIds) {
        final Table roleTable = getTable();
        final Table userRoleTable = getUserRoleTable();
        final SelectQuery selectQuery = SelectQuery.get(userRoleTable);

        selectQuery.addJoinClause(new JoinClause(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(roleTable.getColumn(OMCRMROLE.NAME), roleTable.getColumn(OMCRMROLE.ORG_ID),
                roleTable.getColumn(OMCRMROLE.PERMISSIONS), roleTable.getColumn(OMCRMROLE.CONFIG)));
        selectQuery.addSelectClause(userRoleTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.USER_ID), userIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> roleRows = dataContainer.getRows(roleTable);
        final Map<Long, CrmRole> roles = roleRows.collect(Collectors.toMap(Row::getPKValue, roleRow -> convertRowToModel(roleRow, null)));
        final Stream<Row> userRoleRows = dataContainer.getRows(userRoleTable);

        return userRoleRows.collect(Collectors.toMap(userRoleRow -> userRoleRow.get(MCRMUSERROLE.USER_ID),
                userRoleRow -> roles.get((Long) userRoleRow.get(MCRMUSERROLE.ROLE_ID))));
    }

    /**
     * <p>
     * Loads the role details into the target container
     * </p>
     *
     * @param role            Represents the role details which have to be saved into database
     * @param targetContainer Holds the updated information of field values provided by role
     */
    public void loadRoleIntoContainer(final boolean isCreateEvent, final CrmRole role, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Long currentTime = Instant.now().toEpochMilli();
        final CrmUser createdBy = role.getCreatedBy();
        final CrmUser updatedBy = role.getUpdatedBy();
        final FieldPermissions fieldPermissions = role.getFieldPermissions();
        final JsonObject permissionJson = role.getPermissionJson();
        final Collection<CrmUser> crmUsers = role.getCrmUsers();
        final Long orgId = role.orgId();
        final int defaultScopeId = role.getDefaultScope().getType();
        final Table roleTable = getTable();
        final Row row = isCreateEvent ? new Row(roleTable) : dataContainer.getRow(roleTable, role.id());

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }
        final Long id = row.getPKValue();
        final boolean isNewRow = row.isNewRow();

        row.set(OMCRMROLE.NAME, role.getName());
        row.set(OMCRMROLE.UPDATED_TIME, currentTime);
        row.set(OMCRMROLE.DEFAULT_SCOPE, defaultScopeId);

        if (Objects.nonNull(updatedBy)) {
            row.set(OMCRMROLE.UPDATED_BY, updatedBy.id());
        }

        if (isNewRow) {
            role.setId(id);
            row.setOrgId(orgId);
            row.set(OMCRMROLE.CREATED_TIME, currentTime);
            row.set(OMCRMROLE.CONFIG, role.getConfigJsonString());

            if (Objects.nonNull(createdBy)) {
                row.set(OMCRMROLE.CREATED_BY, createdBy.id());
            }
        }

        if (Objects.nonNull(fieldPermissions)) {
            fieldPermissions.setRoleId(id);
            fieldPermissionManager.loadFieldPermissionsIntoContainer(isCreateEvent, fieldPermissions, dataContainer, targetContainer);
        }

        if (Objects.nonNull(permissionJson)) {
            row.set(OMCRMROLE.PERMISSIONS, permissionJson.toString());
        }
        targetContainer.updateRow(row);

        if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
            loadUserRoleIntoContainer(role, dataContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the user role into container
     * </p>
     *
     * @param role            Represents the role details which have to be saved into database
     * @param targetContainer Holds the field values of existing role
     */
    public void loadUserRoleIntoContainer(final CrmRole role, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<Long> userIds = role.getCrmUsers().stream().map(TlcModel::id).collect(Collectors.toSet());
        final Table userRoleTable = getUserRoleTable();

        userIds.forEach(id -> {
            final Row row = sourceContainer.getRow(userRoleTable, new WhereClause(Criteria.eq(userRoleTable.getColumn(MCRMUSERROLE.USER_ID), id)));
            final Row userRoleRow = Objects.isNull(row) ? new Row(userRoleTable) : row;

            userRoleRow.set(MCRMUSERROLE.ROLE_ID, role.id());

            if (userRoleRow.isNewRow()) {
                userRoleRow.set(MCRMUSERROLE.USER_ID, id);
            }
            targetContainer.updateRow(userRoleRow);
        });
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link CrmRole}
     * </p>
     *
     * @param row           Represents the role row
     * @param dataContainer A container that holds the user defined field values and system values
     * @return The {@link CrmRole} details
     */
    public CrmRole convertRowToModel(final Row row, final DataContainer dataContainer) {
        final CrmRole role = new CrmRole();
        final Table userRoleTable = getUserRoleTable();
        final Table userTable = getUserTable();
        final Long roleId = row.getPKValue();
        final Long orgId = row.getOrgId();

        if (Objects.nonNull(dataContainer)) {
            final Stream<Row> userRoleRows = dataContainer.getRows(userRoleTable, new WhereClause(Criteria
                    .eq(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleId)));
            final Collection<Long> userIds = userRoleRows.map(userRoleRow -> (Long) userRoleRow.get(MCRMUSERROLE.USER_ID))
                    .collect(Collectors.toUnmodifiableSet());
            final Stream<Row> userRows = dataContainer.getRows(userTable, new WhereClause(Criteria.in(userTable.getPKColumn(), userIds)));

            userRows.forEach(userRow -> role.addUser(crmUserManager.convertRowToModel(userRow)));
        }
        role.setId(roleId);
        role.setName(row.get(OMCRMROLE.NAME));
        role.setOrgId(orgId);
        role.setCreatedTime(row.get(OMCRMROLE.CREATED_TIME));
        role.setUpdatedTime(row.get(OMCRMROLE.UPDATED_TIME));
        final String permissions = row.get(OMCRMROLE.PERMISSIONS);
        final String roleConfig = row.get(OMCRMROLE.CONFIG);

        if (Objects.nonNull(permissions)) {
            role.setPermissionJson(Json.object(permissions));
        }

        if (Objects.nonNull(roleConfig)) {
            final CrmRole.Config config = new CrmRole.Config(roleConfig);

            role.setConfig(config);
        }
        loadCreatedByAndUpdatedByIntoRole(orgId, row, role);

        return role;
    }

    /**
     * <p>
     * Loads the created by and updated by users into role
     * </p>
     *
     * @param orgId Represents an organization id
     * @param row   Represents the role row
     * @param role  Represents the role details
     */
    private void loadCreatedByAndUpdatedByIntoRole(final Long orgId, final Row row, final CrmRole role) {
        final Long createdById = row.get(OMCRMROLE.CREATED_BY);
        final Long updatedById = row.get(OMCRMROLE.UPDATED_BY);

        if (Objects.nonNull(createdById)) {
            final CrmUser crmUser = userService.getUser(orgId, createdById);

            role.setCreatedBy(crmUser);
        }

        if (Objects.nonNull(updatedById)) {
            final CrmUser crmUser = userService.getUser(orgId, updatedById);

            role.setUpdatedBy(crmUser);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id
     * @param roleIds Represents the role id
     * @return True if role ids exist, otherwise false
     */
    @Override
    public boolean exists(final long orgId, final Collection<Long> roleIds) {
        final Table crmRoleTable = getTable();
        final CountQuery countQuery = CountQuery.get(crmRoleTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(crmRoleTable.getPKColumn(), roleIds)));

        return orgDataStore(orgId).get(countQuery) == roleIds.size();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The {@link CrmRole} details
     */
    public CrmRole getRoleByUser(final Long orgId, final Long userId) {
        final Table roleTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final Table userRoleTable = getUserRoleTable();

        selectQuery.addJoinClause(new JoinClause(roleTable.getPKColumn(), userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID)));
        selectQuery.addSelectClause(List.of(roleTable.getColumn(OMCRMROLE.NAME), roleTable.getColumn(OMCRMROLE.ORG_ID),
                roleTable.getColumn(OMCRMROLE.PERMISSIONS), roleTable.getColumn(OMCRMROLE.CONFIG)));
        selectQuery.addSelectClause(userRoleTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userRoleTable.getColumn(MCRMUSERROLE.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(roleTable);

        return convertRowToModel(row, null);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRole Holds the details of the role.
     */
    @Override
    public void linkUsers(final CrmRole crmRole) {
        final Long roleId = crmRole.id();
        final Collection<Long> userIds = crmRole.getUserIds();
        final Table roleTable = getTable();
        final Table userRoleTable = getUserRoleTable();
        final DataContainer targetContainer = DataContainer.create();
        final Column userIdColumn = userRoleTable.getColumn(MCRMUSERROLE.USER_ID);
        final OrgDataStore orgDataStore = secureOrgDataStore(crmRole.orgId());
        final DataContainer dataContainer = orgDataStore.get(userRoleTable, new WhereClause(Criteria
                .in(userIdColumn, userIds)).and((Criteria.notEq(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleId))));
        Row roleRow = null;

        for (final Long userId : userIds) {
            final Row exitingRow = dataContainer.getRow(userRoleTable, new WhereClause(Criteria.eq(userIdColumn, userId)));

            if (Objects.nonNull(exitingRow)) {
                if (Objects.isNull(roleRow)) {
                    roleRow = orgDataStore.get(roleTable, roleId);
                }
                exitingRow.set(MCRMUSERROLE.ROLE_ID, roleId);
                targetContainer.updateRow(exitingRow);
            }
        }

        if (Objects.nonNull(roleRow)) {
            roleRow.set(OMCRMROLE.UPDATED_TIME, Instant.now().toEpochMilli());
            roleRow.set(OMCRMROLE.UPDATED_BY, crmRole.getUpdatedBy().id());
            targetContainer.updateRow(roleRow);
        }
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRole Holds the details of the role.
     */
    @Override
    public void unlinkUsers(final CrmRole crmRole) {
        throw ErrorCode.get(IamErrorCodes.USER_CANNOT_BE_UNLINKED_FROM_THE_ROLE, "i18n.user.role.unlink.restricted");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represent an organization id
     * @param userIds Represents the user ids
     * @return the collection of {@link CrmRole}
     */
    @Override
    public Collection<CrmRole> fetchRolesForUsers(final long orgId, final Collection<Long> userIds) {
        final Table roleTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final Table userRoleTable = getUserRoleTable();
        final Collection<CrmRole> roles = new ArrayList<>();
        final Column roleIdColumn = userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID);

        selectQuery.addSelectClause(roleTable.getColumn(OMCRMROLE.NAME));
        selectQuery.addSelectClause(userRoleTable);
        selectQuery.addJoinClause(new JoinClause(roleTable.getPKColumn(), roleIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.USER_ID), userIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(roleTable);

        rows.forEach(row -> {
            final Stream<Row> userRows = dataContainer.getRows(userRoleTable,
                    new WhereClause(Criteria.eq(roleIdColumn, row.getPKValue())));
            final CrmRole crmRole = new CrmRole();

            crmRole.setUserIds(userRows.map(userRoleRow -> (Long) userRoleRow.get(MCRMUSERROLE.USER_ID)).toList());
            crmRole.setOrgId(orgId);
            crmRole.setName(row.get(OMCRMROLE.NAME));
            roles.add(crmRole);
        });

        return roles;
    }

    /**
     * <p>
     * Fetches a CRM role along with its associated permissions, assigned users and iam id.
     * </p>
     *
     * @param orgId Represents an organization ID.
     * @param id    Represents the role ID.
     * @return the {@link CrmRole} object populated with its details, permissions, and associated users.
     */
    @Override
    public CrmRole getRole(final long orgId, final long id) {
        final Table roleTable = getTable();
        final Table userRoleTable = getUserRoleTable();
        final Table userTable = getUserTable();
        final Column pkColumn = roleTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(roleTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(pkColumn, id));

        selectQuery.addSelectClause(roleTable, userRoleTable, userTable);
        selectQuery.addJoinClause(new JoinClause(pkColumn, userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), leftJoin));
        selectQuery.addJoinClause(new JoinClause(userRoleTable.getColumn(MCRMUSERROLE.USER_ID), userTable.getPKColumn(), leftJoin));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(roleTable, whereClause);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }

        return convertRoleRowToModel(row, dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return the collection of {@link CrmRole}
     */
    @Override
    public Set<Long> getRoleIds(final long orgId) {
        final Table roleTable = getTable();

        return orgDataStore(orgId).get(roleTable, (WhereClause) null, List.of(roleTable.getPKColumn()))
                .getRows(roleTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organisation id.
     * @return Admin user ids.
     */
    @Override
    public Collection<Long> fetchAdminUserIds(final Long orgId) {
        final Table roleTable = getTable();
        final Table userRoleTable = getUserRoleTable();
        final SelectQuery selectQuery = SelectQuery.get(roleTable);

        selectQuery.addSelectClause(userRoleTable.getColumn(MCRMUSERROLE.USER_ID));
        selectQuery.addJoinClause(new JoinClause(roleTable.getPKColumn(), userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(roleTable.getColumn(OMCRMROLE.NAME), DefaultRole.ACCOUNT_ADMIN.getName())));
        final Stream<Row> userRoleRows = secureOrgDataStore(orgId).get(selectQuery).getRows(userRoleTable);

        return userRoleRows.map(userRoleRow -> (Long) userRoleRow.get(MCRMUSERROLE.USER_ID)).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param roleId Represents the role id
     * @return true, if admin role is present
     */
    @Override
    public boolean isAdminRole(final long orgId, final long roleId) {
        final Table roleTable = getTable();
        final CountQuery countQuery = CountQuery.get(roleTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(roleTable.getPKColumn(), roleId))
                .and(Criteria.eq(roleTable.getColumn(OMCRMROLE.NAME), DefaultRole.ACCOUNT_ADMIN.getName())));

        return secureOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization ID.
     * @param roleIds Represents the role IDs
     * @return Map of role and user IDs.
     */
    @Override
    public Map<Long, Collection<Long>> getUserIds(final Long orgId, final Collection<Long> roleIds) {
        final Table userRoleTable = getUserRoleTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleIds));
        final DataContainer dataContainer = orgDataStore(orgId).get(userRoleTable, whereClause);

        return dataContainer.getRows(userRoleTable).collect(Collectors.collectingAndThen(Collectors.groupingBy(row -> (Long) row.get(MCRMUSERROLE.ROLE_ID),
                Collectors.mapping(row -> (Long) row.get(MCRMUSERROLE.USER_ID), Collectors.toCollection(HashSet::new))), Collections::unmodifiableMap));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID.
     * @return Admin role.
     */
    @Override
    public CrmRole getAdminRole(final long orgId) {
        final Table roleTable = getTable();
        final Column roleNameColumn = roleTable.getColumn(OMCRMROLE.NAME);
        final SelectQuery selectQuery = SelectQuery.get(roleTable);

        selectQuery.addSelectClause(roleNameColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(roleNameColumn, DefaultRole.ACCOUNT_ADMIN.getName())));
        final Row roleRow = orgDataStore(orgId).get(selectQuery).getRow(roleTable);

        if (Objects.isNull(roleRow)) {
            throw ErrorCode.get(IamErrorCodes.ADMIN_ROLE_NOT_FOUND);
        }
        return convertRowToModel(roleRow, null);
    }

    /**
     * <p>
     * Converts a data container row to a {@link CrmRole} model.
     * </p>
     *
     * @param row           Refers to the role row.
     * @param dataContainer The data container with rows.
     * @return the populated {@link CrmRole}.
     */
    private CrmRole convertRoleRowToModel(final Row row, final DataContainer dataContainer) {
        final CrmRole crmRole = new CrmRole();
        final Table userTable = getUserTable();
        final Table userRoleTable = getUserRoleTable();
        final Column pkColumn = userTable.getPKColumn();
        final Long id = row.getPKValue();
        final Long orgId = row.getOrgId();

        crmRole.setId(id);
        crmRole.setOrgId(orgId);
        crmRole.setName(row.get(OMCRMROLE.NAME));
        crmRole.setCreatedTime(row.get(OMCRMROLE.CREATED_TIME));
        crmRole.setUpdatedTime(row.get(OMCRMROLE.UPDATED_TIME));
        crmRole.setPermissionJson(Json.object((String) row.get(OMCRMROLE.PERMISSIONS)));
        loadCreatedByAndUpdatedByIntoRole(orgId, row, crmRole);
        final Stream<Row> userRoleRows = dataContainer.getRows(userRoleTable, new WhereClause(Criteria.eq(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), id)));

        userRoleRows.forEach(userRoleRow -> {
            final CrmUser crmUser = new CrmUserEntity();
            final Long userId = userRoleRow.get(MCRMUSERROLE.USER_ID);
            final Row userRow = dataContainer.getRow(userTable, new WhereClause(Criteria.eq(pkColumn, userId)));

            crmUser.setIamId(userRow.get(OMCRMUSER.IAM_USER_ID));
            crmUser.setId(userId);

            crmRole.addUser(crmUser);
        });

        return crmRole;
    }
}
