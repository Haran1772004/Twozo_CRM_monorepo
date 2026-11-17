package com.tlc.crm.core.features.sales.template.model;

import com.tlc.commons.json.resolver.model.PlaceholderValue;

/**
 * <p>
 * Represents the common placeholder values for a sales document line item.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public abstract class LineItemPlaceholderValue implements ArrayTypePlaceholderValue {

    protected PlaceholderValue itemName;
    protected PlaceholderValue itemQuantity;
    protected PlaceholderValue itemDiscount;
    protected PlaceholderValue itemUnitPrice;
    protected PlaceholderValue itemTermTotal;
    protected PlaceholderValue itemCurrency;

    /**
     * <p>
     * Sets the item name.
     * </p>
     *
     * @param itemName Represents the placeholder name of the item
     */
    public void setItemName(final PlaceholderValue itemName) {
        this.itemName = itemName;
    }

    /**
     * <p>
     * Sets the item quantity.
     * </p>
     *
     * @param itemQuantity Represents the placeholder value of the item quantity
     */
    public void setItemQuantity(final PlaceholderValue itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    /**
     * <p>
     * Sets the item discount.
     * </p>
     *
     * @param itemDiscount Represents the placeholder value of the item discount
     */
    public void setItemDiscount(final PlaceholderValue itemDiscount) {
        this.itemDiscount = itemDiscount;
    }

    /**
     * <p>
     * Sets the item unit price.
     * </p>
     *
     * @param itemUnitPrice Represents the placeholder value of the item unit price
     */
    public void setItemUnitPrice(final PlaceholderValue itemUnitPrice) {
        this.itemUnitPrice = itemUnitPrice;
    }

    /**
     * <p>
     * Sets the total value of the line item.
     * </p>
     *
     * @param itemTermTotal Represents the placeholder value of the line-item total
     */
    public void setItemTermTotal(final PlaceholderValue itemTermTotal) {
        this.itemTermTotal = itemTermTotal;
    }

    /**
     * <p>
     * Sets the currency of the line item.
     * </p>
     *
     * @param itemCurrency Represents the placeholder value of the item currency
     */
    public void setItemCurrency(final PlaceholderValue itemCurrency) {
        this.itemCurrency = itemCurrency;
    }
}