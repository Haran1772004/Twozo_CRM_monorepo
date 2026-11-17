package com.tlc.crm.common.iam.field;

/**
 * <p>
 * Represents the fields related to {@link com.tlc.crm.common.iam.api.model.Team}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum TeamFields {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name"),
    DESCRIPTION("description"),
    USERS("users"),
    ADDED_USERS("addedUsers"),
    REMOVED_USERS("removedUsers"),
    UPDATED_USERS("updatedUsers"),
    TYPE("type"),
    DATA("data"),
    TEAM("team"),
    TEAMS("teams"),
    TEAM_ID("teamId"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    CREATED_BY("createdBy"),
    UPDATED_BY("updatedBy"),
    MESSAGE("message"),
    CREATED_TIME("createdTime"),
    UPDATED_TIME("updatedTime"),
    TEAM_MANAGERS("managers"),
    USER_ID("userId"),
    ASSIGN_AS_MANAGER("assignAsManager"),
    STATUS("status"),
    FETCH_MANAGERS("fetchManagers"),
    USERS_COUNT("usersCount"),
    FIELD("field"),
    VALUE("value"),
    SORT("sort"),
    TEAM_NAME("teamName"),
    ;

    private final String name;

    TeamFields(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
