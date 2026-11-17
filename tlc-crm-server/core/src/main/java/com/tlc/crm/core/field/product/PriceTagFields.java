package com.tlc.crm.core.field.product;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum PriceTagFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    PRICE("price"),
    CURRENCY("currency"),
    CURRENCY_ID("currencyId"),
    PRODUCT_ID("productId"),
    VARIATION_ID("variationId"),
    TAX("tax"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    DISCOUNT("discount"),
    UNIT_PRICE("unitPrice"),
    SYMBOL("symbol"),
    ;


    private final String name;

    PriceTagFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
