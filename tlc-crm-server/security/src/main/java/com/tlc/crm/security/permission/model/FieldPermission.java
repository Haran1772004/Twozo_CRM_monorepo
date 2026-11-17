package com.tlc.crm.security.permission.model;

import com.tlc.validator.TlcModel;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the field-level permission configuration for a given role and organization.
 * <p>
 * Each {@code FieldPermission} instance defines access control for a specific field,
 * including its access level (read, write, etc.), requirement flag, and related identifiers.
 * </p>
 *
 * @author  Sathish
 * @version 1.0
 */
public class FieldPermission implements Permission, TlcModel, Serializable, Comparable<FieldPermission> {

    private Long orgId;
    private Long roleId;
    private Long sourceId;
    private Long fieldId;
    private FieldAccessLevel accessLevel;
    private int order;
    private boolean isRequired;
    private boolean isRequiredOnDataFields;

    public Long getFieldId() {
        return fieldId;
    }

    @Override
    public Long orgId() {
        return orgId;
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

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(final Long sourceId) {
        this.sourceId = sourceId;
    }

    public void setFieldId(final Long fieldId) {
        this.fieldId = fieldId;
    }

    public FieldAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(final FieldAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(final boolean required) {
        isRequired = required;
    }

    public boolean isRequiredOnDataFields() {
        return isRequiredOnDataFields;
    }

    public void setRequiredOnDataFields(final boolean requiredOnDataFields) {
        isRequiredOnDataFields = requiredOnDataFields;
    }

    @Override
    public Long id() {
        return null;
    }

    @Override
    public Object identity() {
        return null;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final FieldPermission fieldPermission = (FieldPermission) object;

        return Objects.equals(orgId, fieldPermission.orgId)
                && Objects.equals(roleId, fieldPermission.roleId)
                && Objects.equals(sourceId, fieldPermission.sourceId)
                && Objects.equals(fieldId, fieldPermission.fieldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, roleId, sourceId, fieldId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final FieldPermission fieldPermission) {
        int result = Long.compare(sourceId, fieldPermission.getSourceId());

        if (result == 0) {
            result = Integer.compare(order, fieldPermission.getOrder());
        }

        return result;
    }
}
