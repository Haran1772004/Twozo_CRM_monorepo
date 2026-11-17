package com.tlc.crm.security.permission.model;

import com.tlc.commons.json.JsonArray;

import com.tlc.crm.security.internal.permission.builder.FieldPermissionBuilder;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Represents a collection of {@link FieldPermission} objects for a specific role within an organization.
 * <p>
 * This class provides utility methods to build and manage field-level permissions from
 * JSON configuration data and convert them back to JSON format when needed.
 * It also ensures data consistency and validation for role-based permission management.
 * </p>
 *
 * @author  Sathish
 * @version 1.0
 */
public class FieldPermissions extends Permissions<FieldPermission> {

    @NotNull(groups = Group.Update.class, message = "i18n.role.permission.roleId.required")
    private Long roleId;

    private JsonArray permissionsAsJson;
    private String permissionJsonString;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(final Long roleId) {
        this.roleId = roleId;
    }

    public void setPermissionsAsJson(final JsonArray permissionsAsJson) {
        Objects.requireNonNull(orgId, "Org ID should not be null.");
        this.permissions = FieldPermissionBuilder.getInstance().build(orgId, roleId, permissionsAsJson);
    }

    public JsonArray toJson() {
        return permissionsAsJson = Objects.isNull(permissionsAsJson) ? FieldPermissionBuilder.getInstance().build(permissions) : permissionsAsJson;
    }

    @Override
    public void addPermission(final FieldPermission permission) {
        if (Objects.isNull(permissions)) {
            permissions = new TreeSet<>();
        } else {
            permissions.remove(permission);
        }
        permissions.add(permission);
        permissionsAsJson = null;
    }

    @Override
    public void setPermissions(final Collection<FieldPermission> permissions) {
        if (Objects.nonNull(permissions)) {
            this.permissions = new TreeSet<>(permissions);
            permissionsAsJson = null;
        }
    }

    @Override
    public Collection<FieldPermission> getPermissions() {
        return permissions = Objects.isNull(permissions) && Objects.nonNull(permissionsAsJson) ? FieldPermissionBuilder.getInstance().build(orgId, roleId, permissionsAsJson) : permissions;
    }

    public void removePermission(final FieldPermission fieldPermission) {
        final Collection<FieldPermission> fieldPermissions = getPermissions();

        Objects.requireNonNull(fieldPermissions, "Field Permissions should be non null while removing any permission");
        fieldPermissions.remove(fieldPermission);
        permissionsAsJson = null;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final FieldPermissions fieldPermissions = (FieldPermissions) object;

        return Objects.equals(hashCode(), fieldPermissions.hashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, roleId);
    }

    @Override
    public String toString() {
        return permissionJsonString = Objects.isNull(permissionJsonString) ? toJson().toString() : permissionJsonString;
    }
}
