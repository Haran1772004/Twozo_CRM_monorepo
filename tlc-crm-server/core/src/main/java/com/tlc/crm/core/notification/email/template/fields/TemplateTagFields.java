package com.tlc.crm.core.notification.email.template.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;

/**
 * <p>
 * Represents fields related to {@link TemplateTag}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum TemplateTagFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    ORG_ID("orgId"),
    CREATED_TIME("createdTime"),
    UPDATED_TIME("updatedTime"),
    OWNER_ID("ownerId"),
    CREATED_BY("createdBy"),
    LAST_USED_TIME("lastUsedTime"),
    NA("NA"),
    FIELD("field"),
    SORT("sort"),
    CREATED_AT("createdAt"),
    DESC("desc"),
    TYPE("type"),
    ;

    private final String fieldName;

    TemplateTagFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getName() {
        return this.fieldName;
    }
}
