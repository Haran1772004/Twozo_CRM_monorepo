package com.tlc.crm.core.features.invoice.model;

import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.LineItemPlaceholderValue;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents a single row in the invoice line items table.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class InvoiceLineItemPlaceholderValue extends LineItemPlaceholderValue {

    private PlaceholderValue itemTax;
    private PlaceholderValue itemValue;

    /**
     * <p>
     * Converts the invoice line item placeholder values into a map of
     * placeholder keys and values.
     * </p>
     *
     * @return A map containing the invoice line item placeholder values.
     */
    @Override
    public Map<PlaceholderKey, PlaceholderValue> toMap() {
        final Map<PlaceholderKey, PlaceholderValue> map = new HashMap<>();

        map.put(InvoicePlaceholderKey.ITEM_VALUE, itemValue);
        map.put(InvoicePlaceholderKey.ITEM_NAME, itemName);
        map.put(InvoicePlaceholderKey.ITEM_QUANTITY, itemQuantity);
        map.put(InvoicePlaceholderKey.ITEM_DISCOUNT, itemDiscount);
        map.put(InvoicePlaceholderKey.ITEM_UNIT_PRICE, itemUnitPrice);
        map.put(InvoicePlaceholderKey.ITEM_TAX, itemTax);
        map.put(InvoicePlaceholderKey.ITEM_TOTAL, itemTermTotal);
        map.put(InvoicePlaceholderKey.ITEM_CURRENCY, itemCurrency);

        return map;
    }

    /**
     * <p>
     * Sets the tax placeholder value for the invoice line item.
     * </p>
     *
     * @param itemTax Represents the tax placeholder value.
     */
    public void setItemTax(final PlaceholderValue itemTax) {
        this.itemTax = itemTax;
    }

    /**
     * <p>
     * Sets the product value placeholder value for the invoice line item.
     * </p>
     *
     * @param itemValue Represents the product placeholder value.
     */
    public void setItemValue(final PlaceholderValue itemValue) {
        this.itemValue = itemValue;
    }
}