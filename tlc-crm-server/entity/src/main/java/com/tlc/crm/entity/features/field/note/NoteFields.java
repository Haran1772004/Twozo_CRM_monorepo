package com.tlc.crm.entity.features.field.note;

import com.tlc.crm.entity.models.FieldNameHolder;

import java.util.Objects;

/**
 * Represents fields related to note
 *
 * @author AswiniN
 * @version 1.0
 * @see FieldNameHolder
 */
public enum NoteFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    CONTENT("content"),
    CREATED_TIME("createdTime"),
    UPDATED_TIME("updatedTime"),
    CONTACT_ID("contactId"),
    DEAL_ID("dealId"),
    START("start"),
    LIMIT("limit"),
    DATA("data"),
    TYPE("type"),
    NOTES("notes"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    COMPANY_ID("companyId"),
    SOURCE("source"),
    ENTITY_ID("entityId"),
    CONTACT("contact"),
    COMPANY("company"),
    DEAL("deal"),
    NOTES_DOT_ID("notes.id"),
    RELATED_ENTITY_IDS("relatedEntityIds"),
    NOTE_ID("noteId"),
    NAME("name"),
    CREATED_BY("createdBy"),
    UPDATED_BY("updatedBy"),
    IS_EDITED("isEdited"),
    IS_PINNED("isPinned"),
    COMMENTS("comments"),
    COMMENTS_COUNT("commentsCount"),
    COMMENT("comment"),
    A_TAG("a"),
    HREF_TAG("href"),
    USER_ID_TAG("user-id"),
    LAST_ACTION_TIME("lastActionTime"),
    NOTE("note"),
    ;

    private final String name;

    NoteFields(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
