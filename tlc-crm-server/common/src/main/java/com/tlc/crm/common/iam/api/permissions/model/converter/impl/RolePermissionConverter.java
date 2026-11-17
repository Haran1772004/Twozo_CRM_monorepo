package com.tlc.crm.common.iam.api.permissions.model.converter.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.permissions.model.converter.PermissionConverter;
import com.tlc.crm.common.iam.api.permissions.model.converter.config.ConverterConfig;
import com.tlc.crm.common.iam.field.permissions.PermissionAttributes;

import com.tlc.crm.security.internal.permission.builder.RolePermissionBuilder;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link RolePermissions}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RolePermissionConverter implements PermissionConverter<RolePermission, RolePermissions> {

    private final RolePermissionBuilder rolePermissionBuilder;

    private static final class Instance {
        private static final RolePermissionConverter INSTANCE = new RolePermissionConverter();
    }

    private RolePermissionConverter() {
        rolePermissionBuilder = RolePermissionBuilder.getInstance();
    }

    /**
     * <p>
     * Gets the {@link RolePermissionConverter} Instance
     * </p>
     *
     * @return {@link RolePermissionConverter}
     */
    public static RolePermissionConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     * @param data  Which contains the request data
     * @return {@link RolePermissions} filled with the data
     */
    @Override
    public RolePermissions convert(final Long orgId, final JsonObject data) {
        final RolePermissions rolePermissions = new RolePermissions();
        final JsonObject permissionJson = data.getJsonObject(PermissionAttributes.PERMISSIONS.getName());
        final String roleIdKey = PermissionAttributes.ROLE_ID.getName();

        rolePermissions.setOrgId(orgId);
        rolePermissions.setPermissions(rolePermissionBuilder.build(orgId, null, permissionJson));

        if (data.containsKey(roleIdKey)) {
            rolePermissions.setRoleId(data.getLong(roleIdKey));
        }
        return rolePermissions;
    }

    /**
     * {@inheritDoc}
     *
     * @param rolePermissions Represents the role permissions attributes.
     * @param converterConfig
     * @return Converted {@link JsonObject}.
     */
    @Override
    public JsonObject convert(final RolePermissions rolePermissions, final ConverterConfig converterConfig) {
        if (converterConfig.isGroupingNeeded()) {
            return rolePermissionBuilder.build(rolePermissions.getPermissions());
        }
        final Map<String, Map<String, Map<String, Object>>> rolePermissionMap = new HashMap<>();

        for (final RolePermission rolePermission : rolePermissions.getPermissions()) {
            rolePermissionBuilder.loadPermissionIntoMap(rolePermission, rolePermissionMap);
        }
        return Json.object(rolePermissionMap);
    }
}
