package com.tlc.crm.core.features.field.activity;

import com.tlc.crm.entity.models.FieldNameHolder;

import java.util.Objects;

/**
 * Holds the source related activity fields
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum ActivityHolderFields implements FieldNameHolder {

    CONTACT_ID("contactId"),
    DEAL_ID("dealId"),
    DEAL("deal"),
    DEAL_AGE("dealAge"),
    COMPANY("company"),
    COMPANY_ID("companyId"),
    CONTACT_IDS("contactIds"),
    CONTACTS("contacts"),
    ACTIVITY_DATE("activityDate"),
    ACTIVITY_TYPE("activityType"),
    ENTITY_ID("entityId"),
    SOURCE("source"),
    ACTIVITY_TYPE_IDS("activityTypeIds"),
    GUESTS("guests"),
    EMAILS("emails"),
    ID("id"),
    EMAIL_ID("emailId"),
    FILTER_TYPE("filterType"),
    ACTIVITY_ID("ACTIVITY_ID"),
    ;

    private final String name;

    ActivityHolderFields(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
