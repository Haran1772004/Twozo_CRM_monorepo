package com.tlc.crm.core.features.quote.model;

import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.ArrayTypePlaceholderValue;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents a single seller contact row in the Acceptance section of a quotation template.
 * </p>
 *
 * <p>Placeholder keys populated by this class:</p>
 * <ul>
 *   <li>{@code ACCEPTANCE_SELLER_CONTACT_VALUE} – contact name</li>
 *   <li>{@code ACCEPTANCE_SELLER_CONTACT_EMAIL_VALUE} – contact email</li>
 *   <li>{@code ACCEPTANCE_SELLER_CONTACT_ICON_VALUE} – contact icon/initials</li>
 * </ul>
 *
 * @author Sathish
 * @version 1.0
 * @see ArrayTypePlaceholderValue
 */
public final class AcceptanceSellerContactPlaceholderValue implements ArrayTypePlaceholderValue {

    private PlaceholderValue contactValue;
    private PlaceholderValue contactEmailValue;
    private PlaceholderValue contactIconValue;
    private PlaceholderValue userIdValue;

    public AcceptanceSellerContactPlaceholderValue setContactValue(final PlaceholderValue contactValue) {
        this.contactValue = contactValue;
        return this;
    }

    public AcceptanceSellerContactPlaceholderValue setContactEmailValue(final PlaceholderValue contactEmailValue) {
        this.contactEmailValue = contactEmailValue;
        return this;
    }

    public AcceptanceSellerContactPlaceholderValue setContactIconValue(final PlaceholderValue contactIconValue) {
        this.contactIconValue = contactIconValue;
        return this;
    }

    public AcceptanceSellerContactPlaceholderValue setUserIdValue(final PlaceholderValue userIdValue) {
        this.userIdValue = userIdValue;
        return this;
    }

    @Override
    public Map<PlaceholderKey, PlaceholderValue> toMap() {
        final Map<PlaceholderKey, PlaceholderValue> map = new HashMap<>();

        map.put(QuotationPlaceholderKey.ACCEPTANCE_SELLER_CONTACT_VALUE, contactValue);
        map.put(QuotationPlaceholderKey.ACCEPTANCE_SELLER_CONTACT_EMAIL_VALUE, contactEmailValue);
        map.put(QuotationPlaceholderKey.ACCEPTANCE_SELLER_CONTACT_ICON_VALUE, contactIconValue);
        map.put(QuotationPlaceholderKey.ACCEPTANCE_SELLER_USER_ID_VALUE, userIdValue);
        return map;
    }
}
