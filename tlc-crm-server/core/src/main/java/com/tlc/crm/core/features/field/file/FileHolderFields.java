package com.tlc.crm.core.features.field.file;

import com.tlc.crm.entity.models.FieldNameHolder;

import java.util.Objects;

/**
 * Represents the fields related to file holder.
 *
 * @author ThalaimalaiPandiyan
 * @version 1.0
 */
public enum FileHolderFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    DEAL_ID("dealId"),
    CONTACT_ID("contactId"),
    COMPANY_ID("companyId"),
    START("start"),
    LIMIT("limit"),
    FILES("files"),
    DATA("data"),
    TYPE("type"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    ATTACHMENTS("attachments"),
    SOURCE("source"),
    ENTITY_ID("entityId")
    ;

    private final String name;

    FileHolderFields(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

}