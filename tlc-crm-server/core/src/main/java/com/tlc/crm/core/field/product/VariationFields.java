package com.tlc.crm.core.field.product;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum VariationFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name"),
    PRODUCT_ID("productId"),
    PRICE_TAGS("priceTags"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    LIST("list"),
    VARIATION_ID("variationId"),
    IS_PRICE_VARIATIONS_ENABLED("isPriceVariationsEnabled"),
    INCLUDE_PRODUCT("includeProduct"),
    PRODUCT("product"),
    ;

    private final String name;

    VariationFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}

