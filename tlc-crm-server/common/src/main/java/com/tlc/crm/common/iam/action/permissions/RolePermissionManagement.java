package com.tlc.crm.common.iam.action.permissions;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.permissions.model.converter.config.ConverterConfig;
import com.tlc.crm.common.iam.api.permissions.model.converter.impl.RolePermissionConverter;
import com.tlc.crm.common.iam.field.permissions.PermissionAttributes;
import com.tlc.crm.common.iam.internal.IamErrorCodes;

import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.manager.PermissionManager;
import com.tlc.crm.security.permission.manager.RolePermissionManager;
import com.tlc.crm.security.permission.model.Permissions;
import com.tlc.crm.security.permission.service.RolePermissionService;

import com.tlc.i18n.I18nResolutionAdapter;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link RolePermissions} related web actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/role/permission/mgmt")
public class RolePermissionManagement extends PermissionConfigAction<RolePermission, RolePermissions> {

    private static final String DATA = "data";
    private static final String TYPE = "type";
    private static final String GET_ROLE_PERMISSIONS = "getRolePermissions";

    private final RoleManager roleManager;
    private final RolePermissionConverter permissionConverter;
    private final RolePermissionService rolePermissionService;
    private final RolePermissionManager rolePermissionManager;

    public RolePermissionManagement() {
        roleManager = RoleManager.getInstance();
        permissionConverter = RolePermissionConverter.getInstance();
        rolePermissionService = RolePermissionService.getInstance();
        rolePermissionManager = RolePermissionManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    protected final void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(TYPE);
        final JsonObject data = requestJson.getJsonObject(DATA);
        final Long orgId = request.orgId();

        switch (type) {
            case GET_ROLE_PERMISSIONS -> {
                final long roleId = data.getLong(PermissionAttributes.ROLE_ID.getName());

                if (roleManager.exists(orgId, Set.of(roleId))) {
                    final RolePermissions rolePermissions = rolePermissionService.getRolePermissions(orgId, roleId);
                    final JsonObject permissionJson = permissionConverter.convert(rolePermissions, new ConverterConfig(false));

                    loadPermissionJsonIntoResponse(permissionJson, response);
                } else {
                    throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id.
     * @param permissions Collection of {@link RolePermissions}.
     * @return true if the given details are valid.
     */
    @Override
    protected final boolean validatePermissionDetails(final Long orgId, final Collection<RolePermissions> permissions) {
        final Collection<Long> roleIds = permissions.stream().map(RolePermissions::getRoleId).collect(Collectors.toSet());

        if (roleManager.exists(orgId, roleIds)) {
            return true;
        }
        throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organisation id.
     * @param jsonObject Holds the role permission values as json.
     * @return {@link RolePermissions} Holds the details of the permissions of the role.
     */
    @Override
    protected final RolePermissions convert(final long orgId, final JsonObject jsonObject) {
        return permissionConverter.convert(orgId, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param permissions           Which holds the values of role permission.
     * @param i18nResolutionAdapter {@link I18nResolutionAdapter} to resolve the text language.
     * @return {@link JsonObject} holds the role permission values as json.
     */
    @Override
    protected final JsonObject convert(final RolePermissions permissions, final I18nResolutionAdapter i18nResolutionAdapter) {
        return permissionConverter.convert(permissions, new ConverterConfig(true));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link PermissionManager} instance.
     */
    @Override
    protected final PermissionManager<RolePermission, RolePermissions> getPermissionManager() {
        return rolePermissionManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID.
     * @param data  Represents the request data.
     * @return The {@link Permissions}.
     */
    @Override
    protected final RolePermissions get(final Long orgId, final JsonObject data) {
        final long roleId = data.getLong(PermissionAttributes.ID.getName());

        if (roleManager.exists(orgId, Set.of(roleId))) {
            return rolePermissionService.getRolePermissions(orgId, roleId);
        }
        throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
    }

    /**
     * {@inheritDoc}
     *
     * @param permissionJson Holds the details of the permissions.
     * @param response       Represents the {@link CrmResponse}.
     */
    @Override
    protected final void loadPermissionJsonIntoResponse(final JsonObject permissionJson, final CrmResponse response) {
        for (final String category : permissionJson.keySet()) {
            response.put(category, permissionJson.get(category));
        }
    }
}
