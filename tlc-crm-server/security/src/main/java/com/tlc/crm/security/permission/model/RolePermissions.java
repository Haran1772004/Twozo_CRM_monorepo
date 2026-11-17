package com.tlc.crm.security.permission.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.internal.permission.builder.RolePermissionBuilder;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents the permissions of the role.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RolePermissions extends Permissions<RolePermission> {

    @NotNull(groups = Group.Update.class, message = "i18n.role.permission.roleId.required")
    private Long roleId;

    private JsonObject permissionsAsJson;

    private final RolePermissionBuilder rolePermissionBuilder;

    public RolePermissions() {
        rolePermissionBuilder = RolePermissionBuilder.getInstance();
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(final Long roleId) {
        this.roleId = roleId;
    }

    public void setPermissionsAsJson(final JsonObject permissionsAsJson) {
        this.permissionsAsJson = permissionsAsJson;
    }

    public JsonObject toJson() {
        return permissionsAsJson = Objects.isNull(permissionsAsJson) ? rolePermissionBuilder.build(permissions) : permissionsAsJson;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RolePermissions rolePermissions = (RolePermissions) o;

        return Objects.equals(hashCode(), rolePermissions.hashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, roleId);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
