package com.tlc.crm.common.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;

/**
 * It offers convenient methods for converting various data types to numeric values.
 *
 * @author sundar
 * @version 1.0
 */
public class NumberUtils {

    /**
     * Parses the given String value into appropriate Number
     *
     * @param value To be parsed
     * @return the appropriate number
     */
    public static Number parseNumber(final Object value) {
        if (value == null) {
            return null;
        }
        final String stringValue = String.valueOf(value).trim();

        if (TypeChecker.isDecimalNumber(stringValue)) {
            return Double.parseDouble(stringValue);
        } else if (TypeChecker.isLong(stringValue)) {
            return stringValue.matches("-?\\d+[lL]")
                    ? Long.parseLong(stringValue.substring(0, stringValue.length() - 1)) : Long.parseLong(stringValue);
        } else if (TypeChecker.isInteger(stringValue)) {
            return Integer.parseInt(stringValue);
        } else {
            throw ErrorCode.get(CommonErrorCodes.INVALID_NUMBER_FORMAT, String.format("Invalid number format: %s", stringValue));
        }
    }

    /**
     * Parses the given value into Double
     *
     * @param value To be parsed
     * @return Double value if the value in decimal format
     */
    public static Double parseDouble(final Object value) {

        if (null == value) {
            return null;
        }

        if (value instanceof final Number number) {
            return number.doubleValue();
        }
        final String stringValue = String.valueOf(value).trim();

        if (TypeChecker.isDecimalNumber(stringValue)) {
            return Double.parseDouble(stringValue);
        } else {
            throw ErrorCode.get(CommonErrorCodes.INVALID_NUMBER_FORMAT, String.format("Invalid number format: %s", stringValue));
        }
    }
}
