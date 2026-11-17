package com.tlc.crm.core.features.invoice.model;

import com.tlc.commons.json.resolver.model.PlaceholderKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Defines all placeholder key constants used by the default invoice JSON template.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum InvoicePlaceholderKey implements PlaceholderKey {

    INVOICE_URL("INVOICE_URL"),
    LANGUAGE("LANGUAGE"),
    LOCALE("LOCALE"),
    CURRENCY("CURRENCY"),

    HEADER_TITLE_VALUE("HEADER_TITLE_VALUE"),
    HEADER_INVOICE_DATE_LABEL("HEADER_INVOICE_DATE_LABEL"),
    HEADER_INVOICE_DATE_VALUE("HEADER_INVOICE_DATE_VALUE"),
    HEADER_DUE_DATE_LABEL("HEADER_DUE_DATE_LABEL"),
    HEADER_DUE_DATE_FORMAT("HEADER_DUE_DATE_FORMAT"),
    HEADER_DUE_DATE_VALUE("HEADER_DUE_DATE_VALUE"),
    HEADER_PO_NUMBER_LABEL("HEADER_PO_NUMBER_LABEL"),
    HEADER_PO_NUMBER_VALUE("HEADER_PO_NUMBER_VALUE"),
    HEADER_COMPANY_NAME_VALUE("HEADER_COMPANY_NAME_VALUE"),
    HEADER_LOGO_VALUE("HEADER_LOGO_VALUE"),

    PARTY_BUYER_TITLE_VALUE("PARTY_BUYER_TITLE_VALUE"),
    PARTY_BUYER_COMPANY_LABEL("PARTY_BUYER_COMPANY_LABEL"),
    PARTY_BUYER_COMPANY_VALUE("PARTY_BUYER_COMPANY_VALUE"),
    PARTY_BUYER_COMPANY_NAME_VALUE("PARTY_BUYER_COMPANY_NAME_VALUE"),
    PARTY_BUYER_COMPANY_ADDRESS_VALUE("PARTY_BUYER_COMPANY_ADDRESS_VALUE"),
    PARTY_BUYER_CONTACT_LABEL("PARTY_BUYER_CONTACT_LABEL"),
    PARTY_BUYER_CONTACT_EMAIL_VALUE("PARTY_BUYER_CONTACT_EMAIL_VALUE"),
    PARTY_BUYER_CONTACT_NAME_VALUE("PARTY_BUYER_CONTACT_NAME_VALUE"),
    PARTY_BUYER_CONTACT_VALUE("PARTY_BUYER_CONTACT_VALUE"),

    PARTY_BILLING_SAME_AS_COMPANY_VALUE("PARTY_BILLING_SAME_AS_COMPANY_VALUE"),
    PARTY_BILLING_ADDRESS_VALUE("PARTY_BILLING_ADDRESS_VALUE"),
    PARTY_BILLING_VALUE("PARTY_BILLING_VALUE"),

    PARTY_SHIPPING_ADDRESS_LABEL("PARTY_SHIPPING_ADDRESS_LABEL"),
    PARTY_SHIPPING_SAME_AS_COMPANY_VALUE("PARTY_SHIPPING_SAME_AS_COMPANY_VALUE"),
    PARTY_SHIPPING_ADDRESS_VALUE("PARTY_SHIPPING_ADDRESS_VALUE"),
    PARTY_SHIPPING_VALUE("PARTY_SHIPPING_VALUE"),

    LINE_ITEMS("LINE_ITEMS"),
    ITEM_VALUE("ITEM_VALUE"),
    ITEM_NAME("ITEM_NAME"),
    ITEM_QUANTITY("ITEM_QUANTITY"),
    ITEM_DISCOUNT("ITEM_DISCOUNT"),
    ITEM_UNIT_PRICE("ITEM_UNIT_PRICE"),
    ITEM_TAX("ITEM_TAX"),
    ITEM_TOTAL("ITEM_TOTAL"),
    ITEM_CURRENCY("ITEM_CURRENCY"),

    ITEM_SUB_TOTAL_LABEL("ITEM_SUB_TOTAL_LABEL"),
    ITEM_SUB_TOTAL_VALUE("ITEM_SUB_TOTAL_VALUE"),
    ITEM_SUB_TOTAL_CURRENCY_VALUE("ITEM_SUB_TOTAL_CURRENCY_VALUE"),

    ITEM_DISCOUNT_LABEL("ITEM_DISCOUNT_LABEL"),
    ITEM_DISCOUNT_FORMAT("ITEM_DISCOUNT_FORMAT"),
    ITEM_DISCOUNT_AMOUNT_VALUE("ITEM_DISCOUNT_AMOUNT_VALUE"),
    ITEM_DISCOUNT_PERCENTAGE_VALUE("ITEM_DISCOUNT_PERCENTAGE_VALUE"),
    ITEM_DISCOUNT_PERCENTAGE_AND_AMOUNT_VALUE("ITEM_DISCOUNT_PERCENTAGE_AND_AMOUNT_VALUE"),
    ITEM_DISCOUNT_CURRENCY_VALUE("ITEM_DISCOUNT_CURRENCY_VALUE"),

    ITEM_TAX_LABEL("ITEM_TAX_LABEL"),
    ITEM_TAX_VALUE("ITEM_TAX_VALUE"),
    ITEM_TAX_CURRENCY_VALUE("ITEM_TAX_CURRENCY_VALUE"),

    ITEM_TOTAL_VALUE_LABEL("ITEM_TOTAL_VALUE_LABEL"),
    ITEM_TOTAL_VALUE("ITEM_TOTAL_VALUE"),
    ITEM_TOTAL_VALUE_CURRENCY("ITEM_TOTAL_VALUE_CURRENCY"),

    ITEM_SUB_TOTAL_FORMAT("ITEM_SUB_TOTAL_FORMAT"),

    SOURCE_COMPANY("SOURCE_COMPANY"),
    SOURCE_PRODUCT("SOURCE_PRODUCT");

    private static final Map<String, PlaceholderKey> PLACEHOLDER_KEY_MAP = new HashMap<>();

    static {
        for (final InvoicePlaceholderKey invoicePlaceholderKey : values()) {
            PLACEHOLDER_KEY_MAP.put(invoicePlaceholderKey.getKey(), invoicePlaceholderKey);
        }
    }

    private final String key;

    /**
     * <p>
     * Creates an invoice placeholder key.
     * </p>
     *
     * @param key Represents the placeholder key value.
     */
    InvoicePlaceholderKey(final String key) {
        this.key = key;
    }

    /**
     * <p>
     * Gets the placeholder key value.
     * </p>
     *
     * @return The placeholder key value.
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * <p>
     * Gets the string representation of the placeholder key.
     * </p>
     *
     * @return The placeholder key value.
     */
    @Override
    public String toString() {
        return key;
    }

    /**
     * <p>
     * Provides invoice placeholder keys based on their string representation.
     * </p>
     */
    public static class Provider implements PlaceholderKey.Provider {

        /**
         * <p>
         * Gets the invoice placeholder key for the specified key value.
         * </p>
         *
         * @param key Represents the placeholder key value.
         * @return The corresponding invoice placeholder key.
         * @throws IllegalArgumentException If the specified key is not a valid invoice placeholder key.
         */
        @Override
        public PlaceholderKey getKey(final String key) {
            final PlaceholderKey placeholderKey = PLACEHOLDER_KEY_MAP.get(key);

            if (Objects.isNull(placeholderKey)) {
                throw new IllegalArgumentException("Invalid invoice placeholder key: " + key);
            }

            return placeholderKey;
        }
    }
}