package com.tlc.crm.core.features.sales.template.model;

import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.core.features.quote.model.AcceptanceBuyerContactPlaceholderValue;
import com.tlc.crm.core.features.quote.model.AcceptanceSellerContactPlaceholderValue;
import com.tlc.crm.core.features.quote.model.PartyBuyerContactPlaceholderValue;

import java.util.Map;

/**
 * <p>
 * Marker interface for POJO types that represent a single row in an array-valued
 * section of a quotation template.  Every implementation must be able to convert
 * itself to a {@code Map<String, PlaceholderValue>} whose keys correspond to the
 * placeholder-key constants used inside that array section.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see LineItemPlaceholderValue
 * @see PartyBuyerContactPlaceholderValue
 * @see AcceptanceBuyerContactPlaceholderValue
 * @see AcceptanceSellerContactPlaceholderValue
 */
public interface ArrayTypePlaceholderValue {

    /**
     * Converts this row into a map of placeholder-key → value entries.
     *
     * @return a non-null {@link Map} representing the row's placeholder data
     */
    Map<PlaceholderKey, PlaceholderValue> toMap();
}
