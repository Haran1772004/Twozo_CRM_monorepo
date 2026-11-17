package com.tlc.crm.common.iam.field;

/**
 * <p>
 * Represents the fields related to {@link com.tlc.crm.common.iam.api.model.Territory}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum TerritoryFields {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name"),
    DESCRIPTION("description"),
    USERS("users"),
    ADDED_USERS("addedUsers"),
    REMOVED_USERS("removedUsers"),
    TEAMS("teams"),
    TYPE("type"),
    DATA("data"),
    TERRITORY("territory"),
    TERRITORIES("territories"),
    TERRITORY_ID("territoryId"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    START("start"),
    LIMIT("limit"),
    CREATED_TIME("createdTime"),
    CREATED_BY("createdBy"),
    UPDATED_TIME("updatedTime"),
    UPDATED_BY("updatedBy"),
    USERS_COUNT("usersCount"),
    MESSAGE("message"),
    SORT("sort"),
    FIELD("field"),
    CRITERIA("criteria"),
    VALUE("value"),
    ;


    private final String name;

    TerritoryFields(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
