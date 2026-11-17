package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;
import com.tlc.plans.and.billings.models.BillingCycle;

/**
 * Enum representing different statuses of a subscription plan.
 *
 * @author Sathish
 * @version 1.0
 */
public enum PlanStoryType {

    /** The subscription plan is currently in its trial period, allowing limited or free usage before paid activation. */
    IN_TRIAL((short) 1),

    /** The plan is fully active and charges are billed on a monthly basis. */
    BILLED_MONTHLY((short) 2),

    /** The plan is fully active with charges billed once a year. */
    BILLED_ANNUALLY((short) 3),

    /** The subscription has been cancelled by the user or system and is no longer active. */
    SUBSCRIPTION_CANCELLED((short) 4),

    /** The subscription has reached its expiry date without renewal and access is terminated. */
    SUBSCRIPTION_EXPIRED((short) 5),

    /** The trial period has ended without converting to a paid subscription, thus trial access is revoked. */
    TRIAL_EXPIRED((short) 6),
    ;

    private final short id;

    PlanStoryType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Maps a billing cycle to the corresponding plan status.
     *
     * @param billingCycle the billing cycle of the subscription
     * @return corresponding {@link PlanStoryType} enum (BILLED_MONTHLY or BILLED_ANNUALLY)
     */
    public static PlanStoryType getByBillingCycle(final BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> BILLED_MONTHLY;
            case YEARLY -> BILLED_ANNUALLY;
        };
    }

    /**
     * Maps subscription status and billing cycle to appropriate plan status.
     *
     * @param subscriptionStatus the subscription status
     * @param billingCycle       the billing cycle
     * @return matching {@link PlanStoryType} enum
     * @throws ErrorCode if no matching plan status is found
     */
    public static PlanStoryType getBySubscriptionStatus(final SubscriptionStatus subscriptionStatus, final BillingCycle billingCycle) {
        return switch (subscriptionStatus) {
            case TRIAL -> IN_TRIAL;
            case PAID -> getByBillingCycle(billingCycle);
            case CANCELLED -> SUBSCRIPTION_CANCELLED;
            case TRIAL_EXPIRED -> TRIAL_EXPIRED;
            case EXPIRED -> SUBSCRIPTION_EXPIRED;
            default -> throw ErrorCode.get(SubscriptionErrorCodes.NO_PLAN_STATUS_FOUND,
                    String.format("No plan status found for the given subscription status - %s and billing cycle - %s", subscriptionStatus.name(), billingCycle.name()));
        };
    }
}
