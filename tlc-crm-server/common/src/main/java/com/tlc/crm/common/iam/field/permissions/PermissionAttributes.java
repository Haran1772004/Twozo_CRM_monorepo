package com.tlc.crm.common.iam.field.permissions;

/**
 * <p>
 * Holds the entity of a {@link com.tlc.crm.security.permission.model.RolePermission} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum PermissionAttributes {

    ID("id"),
    ROLE_ID("roleId"),
    PERMITTED_LIMIT("permittedLimit"),
    SCOPE("scope"),
    PERMISSIONS("permissions"),
    FIELD_PERMISSIONS("fieldPermissions"),
    SOURCE_ID("sourceId"),
    FIELDS("fields"),
    ACCESS_LEVEL("accessLevel"),
    IS_REQUIRED("isRequired"),
    NAME("name"),
    ORDER("order"),
    CAN_EDIT("canEdit"),
    CAN_MAKE_HIDDEN("canMakeHidden"),
    CAN_MAKE_EDITABLE("canMakeEditable"),
    CAN_MAKE_READ_ONLY("canMakeReadOnly"),
    IS_REQUIRED_ON_DATA_FIELDS("isRequiredOnDataFields"),
    IS_HIDDEN_ON_DATA_FIELDS("isHiddenOnDataFields"),
    ;

    private final String name;

    PermissionAttributes(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
