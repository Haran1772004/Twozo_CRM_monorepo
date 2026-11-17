package com.tlc.crm.core.field.product;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum CategoryFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name");

    private final String name;

    CategoryFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
