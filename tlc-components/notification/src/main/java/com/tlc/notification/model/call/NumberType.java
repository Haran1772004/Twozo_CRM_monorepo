package com.tlc.notification.model.call;

import java.util.Optional;

/**
 * Enum representing different types of phone numbers.
 *
 * <p>
 * Each number type is assigned an order, which determines its priority.
 * The order can be used for sorting or determining the next available number type.
 * </p>
 *
 * <p>
 * The enum supports transitioning to the next number type via {@link #nextType()}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum NumberType {

    /**
     * Represents a local phone number.
     */
    LOCAL((short) 1),

    /**
     * Represents a toll-free phone number.
     */
    TOLL_FREE((short) 2),

    /**
     * Represents a mobile phone number.
     */
    MOBILE((short) 3),

    /**
     * Represents a VoIP (Voice over IP) phone number.
     */
    VOIP((short) 4);

    private final short order; // Order representing the priority of the number type

    /**
     * Constructs a NumberType with a given order.
     *
     * @param order The order of the number type, used for sorting and prioritization.
     */
    NumberType(short order) {
        this.order = order;
    }

    /**
     * Retrieves the order value of this number type.
     *
     * @return The order value.
     */
    public short getOrder() {
        return order;
    }

    /**
     * Retrieves the next number type in order, if available.
     *
     * <p>
     * This method returns the next priority number type based on the predefined order.
     * If there is no next type, it returns an empty {@link Optional}.
     * </p>
     *
     * @return An {@link Optional} containing the next {@link NumberType}, or empty if there is none.
     */
    public Optional<NumberType> nextType() {
        return Optional.ofNullable(switch (this) {
            case LOCAL -> TOLL_FREE;
            case TOLL_FREE -> MOBILE;
            case MOBILE -> VOIP;
            case VOIP -> null;
        });
    }
}
