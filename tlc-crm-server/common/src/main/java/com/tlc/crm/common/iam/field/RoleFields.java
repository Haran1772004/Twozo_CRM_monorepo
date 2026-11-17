package com.tlc.crm.common.iam.field;

/**
 * <p>
 * Represents the fields related to {@link com.tlc.crm.common.iam.api.model.CrmRole}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum RoleFields {

    ID("id"),
    NAME("name"),
    USERS("users"),
    ROLES("roles"),
    TYPE("type"),
    DATA("data"),
    ORG_ID("orgId"),
    ROLE("role"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    START("start"),
    LIMIT("limit"),
    CREATED_BY("createdBy"),
    UPDATED_BY("updatedBy"),
    USERS_COUNT("usersCount"),
    MESSAGE("message"),
    CREATED_TIME("createdTime"),
    UPDATED_TIME("updatedTime"),
    SORT("sort"),
    FIELD("field"),
    IS_RENAMABLE("isRenamable"),
    IS_ADMIN("isAdmin"),
    IS_DEFAULT("isDefault"),
    DEFAULT_SCOPE("defaultScope"),
    IS_DELETABLE("isDeletable"),
    ROLE_ID("roleId"),
    ;

    private final String name;

    RoleFields(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
