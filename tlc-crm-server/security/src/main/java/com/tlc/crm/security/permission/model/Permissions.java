package com.tlc.crm.security.permission.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * A model that represents the permissions of the role.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class Permissions<T extends Permission> implements TlcModel {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.permission.orgId.required")
    protected Long orgId;

    @Valid
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.permission.required")
    protected Collection<T> permissions;

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setPermissions(final Collection<T> permissions) {
        this.permissions = new HashSet<>(permissions);
    }

    public void addPermission(final T permission) {
        if (Objects.isNull(permissions)) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }

    public Collection<T> getPermissions() {
        return permissions;
    }

    @Override
    public Object identity() {
        return null;
    }

    @Override
    public Long id() {
        return null;
    }
}
