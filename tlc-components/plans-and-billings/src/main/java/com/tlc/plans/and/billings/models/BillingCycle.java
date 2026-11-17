package com.tlc.plans.and.billings.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.plans.and.billings.internal.status.SubscriptionErrorCodes;

/**
 * Represents the available billing cycles for subscription plans.
 *
 * @author Sathish
 * @version 1.0
 */
public enum BillingCycle {

    MONTHLY((short) 1),
    YEARLY((short) 2),
    ;

    private final short id;

    BillingCycle(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Retrieves the {@link BillingCycle} enum based on its numeric ID.
     *
     * @param id the ID of the billing cycle
     * @return corresponding {@link BillingCycle}
     * @throws RuntimeException if the ID does not match any known billing cycle
     */
    public static BillingCycle get(final short id) {
        return switch (id) {
            case 1 -> MONTHLY;
            case 2 -> YEARLY;
            default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_BILLING_CYCLE);
        };
    }
}
