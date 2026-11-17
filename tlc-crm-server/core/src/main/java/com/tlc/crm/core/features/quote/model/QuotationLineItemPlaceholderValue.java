package com.tlc.crm.core.features.quote.model;

import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.ArrayTypePlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.LineItemPlaceholderValue;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents a single product/service line item row in a quotation template.
 * </p>
 *
 * <p>Placeholder keys populated by this class:</p>
 * <ul>
 *   <li>{@code ITEM_VALUE} – product/service name</li>
 *   <li>{@code ITEM_QUANTITY} – quantity</li>
 *   <li>{@code ITEM_DISCOUNT} – discount applied</li>
 *   <li>{@code ITEM_UNIT_PRICE} – price per unit</li>
 *   <li>{@code ITEM_TERM_TOTAL} – total for the term</li>
 * </ul>
 *
 * @author Sathish
 * @version 1.0
 * @see ArrayTypePlaceholderValue
 */
public final class QuotationLineItemPlaceholderValue extends LineItemPlaceholderValue {

    @Override
    public Map<PlaceholderKey, PlaceholderValue> toMap() {
        final Map<PlaceholderKey, PlaceholderValue> map = new HashMap<>();

        map.put(QuotationPlaceholderKey.ITEM_NAME, itemName);
        map.put(QuotationPlaceholderKey.ITEM_QUANTITY, itemQuantity);
        map.put(QuotationPlaceholderKey.ITEM_DISCOUNT, itemDiscount);
        map.put(QuotationPlaceholderKey.ITEM_UNIT_PRICE, itemUnitPrice);
        map.put(QuotationPlaceholderKey.ITEM_TERM_TOTAL, itemTermTotal);
        map.put(QuotationPlaceholderKey.ITEM_CURRENCY, itemCurrency);

        return map;
    }
}
