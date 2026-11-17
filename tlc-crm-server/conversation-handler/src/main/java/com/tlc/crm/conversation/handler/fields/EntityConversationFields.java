package com.tlc.crm.conversation.handler.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Represents fields related to entity conversations
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum EntityConversationFields implements FieldNameHolder {

    TYPE("type"),
    PARTICIPANT("participant"),
    CONVERSATIONS("conversations"),
    EMAIL("email"),
    DATA("data"),
    ICON("icon"),
    TIME_STAMP("timeStamp"),
    ID("id"),
    NAME("name"),
    ORG_ID("orgId"),
    ENTITY_CONVERSATION_ID("entityConversationId"),
    CATEGORY("category"),
    DEAL_CONTACT("dealContact"),
    VARIANT("variant"),
    ;

    private final String fieldName;

    EntityConversationFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * {@inheritDoc}
     *
     * @return The field name
     */
    @Override
    public String getName() {
        return this.fieldName;
    }
}