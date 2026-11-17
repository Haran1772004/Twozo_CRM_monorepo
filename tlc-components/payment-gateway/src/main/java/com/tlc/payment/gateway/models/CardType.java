package com.tlc.payment.gateway.models;

/**
 * Enumeration of supported card types.
 * <p>
 * Defines the types of cards accepted for payment processing, currently
 * including CREDIT and DEBIT card types.
 * </p>
 * <p>
 * Provides a utility method {@link #getByName(String)} to convert a string
 * representation to the corresponding {@code CardType} enum constant.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum CardType {

    CREDIT,
    DEBIT;

    public static CardType getByName(final String name) {
        return switch (name) {
            case "credit" -> CREDIT;
            case "debit" -> DEBIT;
            default -> null;
        };
    }
}
