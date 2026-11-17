package com.tlc.crm.core.field.deal;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum ParticipantFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    DEAL_ID("dealId"),
    CONTACT_ID("contactId"),
    START("start"),
    LIMIT("limit"),
    LIST("list"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    CONTACT_IDS("contactIds"),
    FIRST_NAME_FIELD_ID("firstNameFieldId"),
    LAST_NAME_FIELD_ID("lastNameFieldId"),
    RELATED_FIELD_IDS("relatedFieldIds"),
    ;

    private final String name;

    ParticipantFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
