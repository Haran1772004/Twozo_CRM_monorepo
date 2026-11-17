package com.tlc.crm.core.features.field.dataimport;

import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * Contains {@link ImportInfo} related keys
 *
 * @author Sundar
 * @version 1.0
 */
public enum ImportFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    USER_ID("userId"),
    IMPORT_ID("importId"),
    ATTACHMENT("attachment"),
    STATUS("status"),
    NAME("name"),
    COUNT("count"),
    FIELDS_MAP("fieldsMap"),
    LIST("list"),
    ROWS("rows"),
    MERGE_DUPLICATES("mergeDuplicates"),
    SUGGESTED_MAPPINGS("suggestedMappings"),
    START("start"),
    LIMIT("limit"),
    TYPE("type"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    FIELD("field"),
    HEADER_INDEX("headerIndex"),
    HEADERS("headers"),
    VALUES("values"),
    SUBTYPE("subType"),
    PREVIEW("preview"),
    SOURCE("source"),
    INSERTED("inserted"),
    MERGED("merged"),
    UPDATED("updated"),
    SKIPPED("skipped"),
    DATA("data"),
    IS_REGISTERED_SOURCE("isRegisteredSource"),
    IS_ARRAY("isArray"),
    REASON("reason"),
    UPDATED_RECORDS("updatedRecords"),
    FAILED_RECORDS("failedRecords"),
    IS_CUSTOM_FIELD("isCustomField"),
    REQUEST_ID("requestId"),
    COMPLETION("completion"),
    IS_IN_PROGRESS("isInProgress"),
    IS_DONE("isDone"),
    ERROR("error"),
    STATE("state"),
    IS_MAPPED("isMapped"),
    MESSAGE("message"),
    CREATED_TIME("createdTime"),
    CREATED_AT("createdAt"),
    SOURCES("sources"),
    USER("user"),
    REVERT_ALLOWED("revertAllowed"),
    FILE("file"),
    ATTACHMENTS("attachments"),
    CATEGORY("category"),
    LOCALE("locale"),
    NOTE("note"),
    CONTENT("content"),
    ;

    private final String name;

    ImportFields(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }
}
