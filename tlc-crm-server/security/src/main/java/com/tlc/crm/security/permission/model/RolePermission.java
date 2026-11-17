package com.tlc.crm.security.permission.model;

import com.tlc.crm.security.internal.permission.service.resolver.PermissionScopeResolver;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 * A model that represents the permission of the role.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RolePermission implements Permission, TlcModel, Serializable {

    private Long id;
    private Long orgId;
    private Long roleId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.role.permission.category.required")
    private Category category;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.role.permission.subcategory.required")
    private SubCategory subCategory;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.role.permission.action.required")
    private Action action;
    private Long permittedLimit;
    private Long actionConfigId;
    private Long subCategoryScopeId;
    private boolean isGranted;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(final Long roleId) {
        this.roleId = roleId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public SubCategory getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(final SubCategory subCategory) {
        this.subCategory = subCategory;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    public Scope getScope() {
        final Scope scope = subCategory.getScope();

        return Objects.isNull(roleId) ? scope : PermissionScopeResolver.resolve(orgId, roleId, scope, action.getDependencies());
    }

    public Long getPermittedLimit() {
        return permittedLimit;
    }

    public void setPermittedLimit(final Long permittedLimit) {
        this.permittedLimit = permittedLimit;
    }

    public boolean isGranted() {
        return isGranted;
    }

    public void setGranted(final boolean hasAction) {
        this.isGranted = hasAction;
    }

    public void setActionConfigId(final Long actionConfigId) {
        this.actionConfigId = actionConfigId;
    }

    public Long getActionConfigId() {
        return actionConfigId;
    }

    public void setSubCategoryScopeId(final Long subCategoryScopeId) {
        this.subCategoryScopeId = subCategoryScopeId;
    }

    public Long getSubCategoryScopeId() {
        return subCategoryScopeId;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean equals(final Object permission) {
        return this == permission || (permission instanceof RolePermission && hashCode() == permission.hashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orgId, category, subCategory, action);
    }

    @Override
    public Object identity() {
        return null;
    }
}