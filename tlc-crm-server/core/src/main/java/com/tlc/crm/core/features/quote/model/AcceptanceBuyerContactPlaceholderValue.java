package com.tlc.crm.core.features.quote.model;

import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.ArrayTypePlaceholderValue;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents a single buyer contact row in the Acceptance section of a quotation template.
 * </p>
 *
 * <p>Placeholder keys populated by this class:</p>
 * <ul>
 *   <li>{@code ACCEPTANCE_BUYER_CONTACT_VALUE} – contact name</li>
 *   <li>{@code ACCEPTANCE_BUYER_CONTACT_EMAIL_VALUE} – contact email</li>
 *   <li>{@code ACCEPTANCE_BUYER_CONTACT_ICON_VALUE} – contact icon/initials</li>
 *   <li>{@code ACCEPTANCE_BUYER_CONTACT_ID_VALUE} – contact id</li>
 * </ul>
 *
 * @author Sathish
 * @version 1.0
 * @see ArrayTypePlaceholderValue
 */
public final class AcceptanceBuyerContactPlaceholderValue implements ArrayTypePlaceholderValue {

    private PlaceholderValue contactValue;
    private PlaceholderValue contactEmailValue;
    private PlaceholderValue contactIconValue;
    private PlaceholderValue contactSignerValue;
    private PlaceholderValue contactIdValue;

    public AcceptanceBuyerContactPlaceholderValue setContactValue(final PlaceholderValue contactValue) {
        this.contactValue = contactValue;
        return this;
    }

    public AcceptanceBuyerContactPlaceholderValue setContactEmailValue(final PlaceholderValue contactEmailValue) {
        this.contactEmailValue = contactEmailValue;
        return this;
    }

    public AcceptanceBuyerContactPlaceholderValue setContactIconValue(final PlaceholderValue contactIconValue) {
        this.contactIconValue = contactIconValue;
        return this;
    }

    public AcceptanceBuyerContactPlaceholderValue setContactSignerValue(final PlaceholderValue contactSignerValue) {
        this.contactSignerValue = contactSignerValue;
        return this;
    }

    public AcceptanceBuyerContactPlaceholderValue setContactIdValue(final PlaceholderValue contactIdValue) {
        this.contactIdValue = contactIdValue;
        return this;
    }

    @Override
    public Map<PlaceholderKey, PlaceholderValue> toMap() {
        final Map<PlaceholderKey, PlaceholderValue> map = new HashMap<>();
        
        map.put(QuotationPlaceholderKey.ACCEPTANCE_BUYER_CONTACT_VALUE, contactValue);
        map.put(QuotationPlaceholderKey.ACCEPTANCE_BUYER_CONTACT_EMAIL_VALUE, contactEmailValue);
        map.put(QuotationPlaceholderKey.ACCEPTANCE_BUYER_CONTACT_ICON_VALUE, contactIconValue);
        map.put(QuotationPlaceholderKey.ACCEPTANCE_BUYER_CONTACT_SIGNER_VALUE, contactSignerValue);
        map.put(QuotationPlaceholderKey.ACCEPTANCE_BUYER_CONTACT_ID_VALUE, contactIdValue);
        return map;
    }
}
