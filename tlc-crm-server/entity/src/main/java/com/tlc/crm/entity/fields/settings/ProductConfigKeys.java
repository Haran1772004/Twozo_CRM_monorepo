package com.tlc.crm.entity.fields.settings;

/**
 * Holds the {@link ProductConfigKeys} related keys.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ProductConfigKeys {

    IS_PRICE_VARIATIONS_ENABLED("isPriceVariationsEnabled"),
    IS_PRODUCT_DURATIONS_ENABLED("isProductDurationsEnabled"),
    ;

    private final String name;

    ProductConfigKeys(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}