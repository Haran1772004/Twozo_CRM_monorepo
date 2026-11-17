package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;

import java.util.Collection;
import java.util.Set;

/**
 * Enum representing the status of a subscription plan for an organization.
 *
 * <p>
 * This enum defines the various possible states a subscription can be in: TRIAL, PAID, CANCELLED, TRIAL_EXPIRED, or EXPIRED.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum SubscriptionStatus {

    TRIAL((short) 1),
    PAYMENT_PENDING((short) 2),
    PAID((short) 3),
    CANCELLED((short) 4),
    TRIAL_EXPIRED((short) 5),
    EXPIRED((short) 6),
    ;

    private static final Collection<Short> ACTIVE_STATUS_IDS = Set.of(TRIAL.id, PAID.id, CANCELLED.id);

    private final short id;

    SubscriptionStatus(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Retrieves the subscription status corresponding to the given id.
     *
     * @param id the short value representing a specific status
     * @return the matching {@link SubscriptionStatus}
     * @throws ErrorCode if the id is not valid
     */
    public static SubscriptionStatus get(final short id) {
        return switch (id) {
            case 1 -> TRIAL;
            case 2 -> PAYMENT_PENDING;
            case 3 -> PAID;
            case 4 -> CANCELLED;
            case 5 -> TRIAL_EXPIRED;
            case 6 -> EXPIRED;
            default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_SUBSCRIPTION_STATUS);
        };
    }

    public static Collection<Short> getActiveStatusIds() {
        return ACTIVE_STATUS_IDS;
    }
}
