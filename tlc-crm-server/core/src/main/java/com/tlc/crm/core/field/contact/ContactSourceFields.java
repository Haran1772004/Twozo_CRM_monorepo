package com.tlc.crm.core.field.contact;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum ContactSourceFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name");

    private final String name;

    ContactSourceFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
