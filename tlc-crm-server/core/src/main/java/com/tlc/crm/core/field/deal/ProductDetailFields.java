package com.tlc.crm.core.field.deal;

import com.tlc.crm.entity.models.FieldNameHolder;

public enum ProductDetailFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    PRICE("price"),
    TAX("tax"),
    PRODUCT_ID("productId"),
    QUANTITY("quantity"),
    DISCOUNT("discount"),
    AMOUNT("amount"),
    SUB_TOTAL("subTotal"),
    TAX_TOTAL("taxTotal"),
    TOTAL("total"),
    ITEMS("items"),
    DEAL_ID("dealId"),
    VARIATION_ID("variationId"),
    COMMENT("comment"),
    IS_ACTIVE("isActive"),
    PRODUCT("product"),
    COUNT("count"),
    CURRENCY("currency"),
    VARIATION("variation"),
    ;

    private final String name;

    ProductDetailFields(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}