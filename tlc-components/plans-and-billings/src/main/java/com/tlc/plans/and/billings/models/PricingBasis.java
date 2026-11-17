package com.tlc.plans.and.billings.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.plans.and.billings.internal.status.SubscriptionErrorCodes;

/**
 * Enum representing the basis on which a subscription plan is priced.
 * <ul>
 *     <li>{@code USER} – Pricing is determined by the number of users</li>
 *     <li>{@code FEATURE} – Pricing is based on enabled features</li>
 * </ul>
 *
 * Provides a utility method to retrieve a pricing basis by its numeric identifier.
 *
 * @author Sathish
 * @version 1.0
 */
public enum PricingBasis {

    USER((short) 1),
    FEATURE((short) 2),
    ;

    private final short id;

    PricingBasis(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Retrieves the {@link PricingBasis} enum constant corresponding to a given ID.
     *
     * @param id numeric ID of the pricing basis
     * @return matching {@link PricingBasis} constant
     */
    public static PricingBasis get(final short id) {
        return switch (id) {
            case 1 -> USER;
            case 2 -> FEATURE;
            default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_PRICING_BASIS);
        };
    }
}
