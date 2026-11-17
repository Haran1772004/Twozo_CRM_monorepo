package com.tlc.crm.entity.features.field.tag;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * Represents fields related to tag
 *
 * @author EswariNivethaVU
 * @version 1.0
 * @see FieldNameHolder
 */
public enum TagFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    TAG("tag"),
    UPDATED_TIME("updatedTime"),
    CREATED_TIME("createdTime"),
    CREATED_BY("createdBy"),
    LAST_USED_TIME("lastUsedTime"),
    DATA("data"),
    TYPE("type"),
    TAG_IDS("tagIds"),
    ERROR("error"),
    SOURCE("source"),
    ENTITY_IDS("entityIds"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    LIST("list"),
    START("start"),
    LIMIT("limit"),
    TAG_ID("tagId"),
    NA("NA"),
    SORT("sort"),
    FIELD("field"),
    CREATED_AT("createdAt"),
    DESC("desc"),
    LAST_USED("lastUsed"),
    VALUE("value"),
    TAGS("tags"),
    VALIDATION_ERRORS("validationErrors"),
    COLOR("color"),
    SEARCH_TEXT("searchText"),
    CRITERIA("criteria"),
    ORDER("order");
    ;

    private final String name;

    TagFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
