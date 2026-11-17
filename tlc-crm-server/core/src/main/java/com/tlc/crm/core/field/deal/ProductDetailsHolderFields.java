package com.tlc.crm.core.field.deal;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum ProductDetailsHolderFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    DEAL_ID("dealId"),
    TAX_INCLUSIVE("taxInclusive"),
    CURRENCY("currency"),
    ITEMS("items"),
    LIST("list"),
    REASON("reason"),
    IDS("ids"),
    DATA("data"),
    TYPE("type"),
    ERRORS("errors");

    private final String name;

    ProductDetailsHolderFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}