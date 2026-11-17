package com.tlc.crm.security.internal.permission.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.security.internal.permission.builder.RolePermissionBuilder;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.manager.RolePermissionManager;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMROLE;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the {@link RolePermission} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see RolePermissionManager
 */
public class RolePermissionManagerImpl implements RolePermissionManager {

    private final RolePermissionBuilder permissionBuilder;

    private static class Instance {
        private static final RolePermissionManagerImpl INSTANCE = new RolePermissionManagerImpl();
    }

    private RolePermissionManagerImpl() {
        permissionBuilder = RolePermissionBuilder.getInstance();
    }

    public static RolePermissionManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the table of the role.
     * </p>
     *
     * @return OMCRMRole table.
     */
    private Table getRoleTable() {
        return Table.get(OMCRMROLE.TABLE);
    }

    /**
     * <p>
     * Provides the {@link OrgDataStore} instance to action organisation related DB actions.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @return {@link OrgDataStore} instance.
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param roleId Refers the id of the role.
     * @return If no rows are having the given id, it will return empty {@link Optional} otherwise it will return the row
     * that match the one of the given id.
     */
    @Override
    public RolePermissions get(final Long orgId, final Long roleId) {
        return get(orgId, Set.of(roleId)).stream().findAny().get();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organisation id.
     * @param roleIds Which contains the collection of role ids.
     * @return If no rows are having the given ids, it will return empty list otherwise it will return the rows
     * that match the one of the given ids.
     */
    public Collection<RolePermissions> get(final Long orgId, final Collection<Long> roleIds) {
        final Table roleTable = getRoleTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(roleTable, new WhereClause(Criteria.in(roleTable.getPKColumn(), roleIds)),
                Set.of(roleTable.getColumn(OMCRMROLE.PERMISSIONS)));

        return dataContainer.getRows(roleTable).map(roleRow -> {
            final RolePermissions rolePermissions = new RolePermissions();
            final JsonObject permissionJson = Json.object((String) roleRow.get(OMCRMROLE.PERMISSIONS));
            final Long roleId = roleRow.getPKValue();

            rolePermissions.setOrgId(orgId);
            rolePermissions.setRoleId(roleId);
            rolePermissions.setPermissionsAsJson(permissionJson);
            rolePermissions.setPermissions(permissionBuilder.build(orgId, roleId, permissionJson));

            return rolePermissions;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organisation id.
     * @param permissions Holds the collection of permissions of the role.
     * @return Collection of updated entities.
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<RolePermissions> permissions) {
        final Table roleTable = getRoleTable();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> roleIds = permissions.stream().map(RolePermissions::getRoleId).collect(Collectors.toUnmodifiableSet());
        final OrgDataStore dataStore = orgDataStore(orgId);
        final DataContainer dataContainer = dataStore.get(roleTable, roleIds);

        for (final RolePermissions rolePermissions : permissions) {
            final Row roleRow = dataContainer.getRow(roleTable, rolePermissions.getRoleId());

            roleRow.set(OMCRMROLE.PERMISSIONS, rolePermissions.toString());
            dataContainer.updateRow(roleRow);
        }

        try {
            dataStore.commitChanges(dataContainer);
            roleIds.forEach(processOutcome::addProcessedRecord);
        } catch (ErrorCode errorCode) {
            final Collection<ErrorDescriptor> failedEntities = new ArrayList<>();
            final ErrorCodeProvider errorCodeProvider = PermissionErrorCodes.PERMISSION_ROLE_UPDATE_FAILED;

            for (final Long roleId : roleIds) {
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(PermissionErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.role.permission.update.action.failed"), errorCodeProvider.name(), roleId.toString()));
            }
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(PermissionErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.role.permission.update.action.failed")), failedEntities));
        }
        return processOutcome;
    }
}