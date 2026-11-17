package com.tlc.crm.core.notification.email.template.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Represents fields related to template
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public enum TemplateFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    DATA("data"),
    TYPE("type"),
    NAME("name"),
    SUBJECT("subject"),
    CONTENT("content"),
    IS_SHARED("isShared"),
    CREATED_TIME("createdTime"),
    LIST("list"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    TEMPLATE("template"),
    SOURCE("source"),
    FIELD_ID("fieldId"),
    DISPLAY_NAME("displayName"),
    VALUE("value"),
    TEMPLATE_TEXT("{text}"),
    ENTITY_ID("entityId"),
    ATTACHMENT_IDS("attachmentIds"),
    TAGS("tags"),
    FOLDER_ID("folderId"),
    STATISTICS("statistics"),
    SENT_COUNT("sentCount"),
    OPENED_PERCENTAGE("openedPercentage"),
    CLICKED_PERCENTAGE("clickedPercentage"),
    REPLIED_PERCENTAGE("repliedPercentage"),
    UNSUBSCRIBED_PERCENTAGE("unsubscribedPercentage"),
    ORDER("order"),
    FILTER_TYPE("filterType"),
    SEARCH_TEXT("searchText"),
    PLACEHOLDERS("placeHolders"),
    SOURCE_ID("sourceId"),
    FIELDS("fields"),
    ERROR("error"),
    CONTACT_ID("contactId"),
    OWNER("owner"),
    MESSAGE("message"),
    FIELD("field"),
    SORT("sort"),
    CREATED_AT("createdAt"),
    DESC("desc"),
    FILE_NAME("fileName"),
    FILE_SIZE("fileSize"),
    ;

    private final String fieldName;

    TemplateFields(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getName() {
        return this.fieldName;
    }
}