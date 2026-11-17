package com.tlc.crm.entity.features.field.activity;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * Represents fields related to activityType
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see FieldNameHolder
 */
public enum ActivityTypeFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name"),
    IS_ACTIVE("isActive"),
    ICON_ID("iconId"),
    ICON("icon"),
    DEACTIVATE("deActivate"),
    ACTIVITY_TYPES("activityTypes"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    TYPE_ID("typeId"),
    ORDER("order"),
    OTHERS("others"),
    IDS("ids"),
    MESSAGE("message"),
    SYNC_TYPE("syncType"),
    CALENDAR_EVENT_ID("calendarEventId"),
    ERROR("error"),
    VALUE("value")
    ;

    private final String name;

    ActivityTypeFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
