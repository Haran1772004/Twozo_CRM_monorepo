package com.tlc.plans.and.billings.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.plans.and.billings.internal.status.SubscriptionErrorCodes;

/**
 * Enum representing types of subscription plans available in the system.
 * <ul>
 *     <li>{@code ORDINARY} – A standard standalone plan</li>
 *     <li>{@code ADD_ON} – A supplemental plan that can be added to a primary one</li>
 * </ul>
 *
 * This enum also provides a utility method to get the type by its numeric ID.
 *
 * @author Sathish
 * @version 1.0
 */
public enum PlanType {

    ORDINARY((short) 1),
    ADD_ON((short) 2),
    ;

    private final short id;

    PlanType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Retrieves the {@link PlanType} based on its ID.
     *
     * @param id numeric ID of the plan type
     * @return matching {@link PlanType}
     */
    public static PlanType get(final short id) {
        return switch (id) {
            case 1 -> ORDINARY;
            case 2 -> ADD_ON;
            default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_PLAN_TYPE);
        };
    }
}
