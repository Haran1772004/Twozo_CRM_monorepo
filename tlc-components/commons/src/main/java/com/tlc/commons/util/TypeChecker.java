package com.tlc.commons.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * <p>
 * A checker that will check the field type based on the field value
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface TypeChecker {

    /**
     * <p>
     * Checks whether the given value is non decimal number or not
     * </p>
     *
     * @param value An object that represents a number
     * @return If the given object represents a non-decimal number it will return true otherwise false will be returned
     */
    static boolean isNonDecimalNumber(final Object value) {

        if (value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            return true;
        } else if (value instanceof final String stringValue) {
            return isInteger(stringValue) || isLong(stringValue);
        }
        return false;
    }

    /**
     * <p>
     * Checks whether the given value represents a decimal number or not
     * </p>
     *
     * @param value An object that represents a number
     * @return If the given object represents a decimal number it will return true otherwise false will be returned
     */
    static boolean isDecimalNumber(final Object value) {

        if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            return true;
        } else if (value instanceof final String stringValue) {
            return stringValue.matches("-?\\d+\\.\\d+([fFdD]?|[eE][-+]?\\d+)?");
        }
        return false;
    }

    /**
     * <p>
     * Checks whether the given value represents a string or not
     * </p>
     *
     * @param value An object that represents a number
     * @return If the given object represents a string it will return true otherwise false will be returned
     */
    static boolean isString(final Object value) {
        return value instanceof String;
    }

    /**
     * <p>
     * Checks whether the given value represents an integer value or not
     * </p>
     *
     * @param stringValue Represents the numeric value is string format
     * @return If the given object represents an integer it will return true otherwise false will be returned
     */
    static boolean isInteger(final String stringValue) {
        return stringValue.matches("-?\\d+");
    }

    /**
     * <p>
     * Checks whether the given value represents an long value or not
     * </p>
     *
     * @param stringValue Represents the numeric value is string format
     * @return If the given object represents a long it will return true otherwise false will be returned
     */
    static boolean isLong(final String stringValue) {
        return stringValue.matches("-?\\d+[lL]?");
    }

    /**
     * <p>
     * Checks whether the given value represents a number
     * </p>
     *
     * @param value Represents the numeric value
     * @return true if the value is a number, false otherwise
     */
    static boolean isNumber(final Object value) {
        return TypeChecker.isDecimalNumber(value) || TypeChecker.isNonDecimalNumber(value);
    }

    /**
     * <p>
     * Checks whether the given value represents an decimal value or not
     * </p>
     *
     * @param value Represents the value
     * @return whether value is negative
     */
    static boolean isNegativeNumber(final Object value) {

        if (value instanceof final String stringValue) {
            return stringValue.matches("^-\\d+(\\.\\d+)?$");
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue() < 0;
        }
        return false;
    }
}
