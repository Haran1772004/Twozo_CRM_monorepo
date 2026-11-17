package com.tlc.crm.entity.attachment.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

import java.util.Objects;

/**
 * Provides access to the attachment field names.
 *
 * @author ThalaimalaiPandiyanT
 */
public enum AttachmentFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    FILE_NAME("fileName"),
    FILE_SIZE("fileSize"),
    MEDIA_TYPE("mediaType"),
    EXTENSION("extension"),
    FILE("file"),
    UPLOADED_TIME("uploadedTime"),
    UPDATED_TIME("updatedTime"),
    DESCRIPTION("description"),
    STORAGE_TYPE("storageType"),
    ALTERNATE_NAME("alternateName"),
    OBJECT_RESPONSE("objectResponse"),
    DATA("data"),
    TYPE("type"),
    ATTACHMENTS("attachments"),
    VALIDATION_ERRORS("validationErrors"),
    UPLOADED_RECORDS("uploadedRecords"),
    CREATED_RECORDS("createdRecords"),
    FAILED_RECORDS("failedRecords"),
    ERRORS("errors"),
    UPDATE_STATUS("updateStatus"),
    REASON("reason"),
    ORG("org"),
    INDEX("index"),
    FOUND("found"),
    FILE_ID("fileId"),
    ENTITY_ID("entityId"),
    CONTACT("contact"),
    CONTACT_ID("contactId"),
    COMPANY("company"),
    COMPANY_ID("companyId"),
    DEAL("deal"),
    DEAL_ID("dealId"),
    RELATED_ENTITY_IDS("relatedEntityIds"),
    OWNER_NAME("ownerName"),
    ATTACHMENT_IDS("attachmentIds"),
    FILES("files"),
    NAME("name"),
    FILES_DOT_ID("files.id"),
    USER_ID("userId"),
    CATEGORY("category"),
    ERROR("error"),
    DURATION("duration"),
    ADDITIONAL_PARAMETERS("additionalParameters"),
    ;

    private final String name;

    AttachmentFields(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

}