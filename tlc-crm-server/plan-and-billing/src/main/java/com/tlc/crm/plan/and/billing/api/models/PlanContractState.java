package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;

/**
 * Enum representing the contract state of a subscription plan.
 *
 * <p>
 * This enum defines the possible contract states that a plan can be in: PREVIOUS, PRESENT, or UPCOMING.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum PlanContractState {

    PREVIOUS((short) 1),
    PRESENT((short) 2),
    UPCOMING((short) 3),
    ;

    private final short id;

    PlanContractState(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Retrieves the contract state based on its numeric id.
     *
     * @param id the identifier of the desired contract state
     * @return the matching {@link PlanContractState}
     * @throws ErrorCode if the id is invalid
     */
    public static PlanContractState get(final short id) {
        return switch (id) {
            case 1 -> PREVIOUS;
            case 2 -> PRESENT;
            case 3 -> UPCOMING;
            default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_CONTRACT_STATE);
        };
    }
}
