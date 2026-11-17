package com.tlc.crm.core.field.product;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum CurrencyFields implements FieldNameHolder {

    ID("id"),
    NAME("name");

    private final String name;

    CurrencyFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
