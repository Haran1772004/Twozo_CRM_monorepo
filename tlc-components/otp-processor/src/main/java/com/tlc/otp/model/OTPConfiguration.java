package com.tlc.otp.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.otp.code.OTPErrorCodes;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Represents the otp configurations
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public record OTPConfiguration(int digit, Type type) {

    /**
     * <p>
     * Holds the otp types
     * </p>
     */
    @AllArgsConstructor
    @Getter
    public enum Type {

        NUMERIC("numeric"),
        ALPHANUMERIC("alphanumeric");

        private final String name;

        /**
         * <p>
         * Fetches the {@link Type}
         * </p>
         *
         * @param type Represents an otp type
         * @return the {@link Type}
         */
        public static Type get(final String type) {
            return switch (type.toLowerCase()) {
                case "numeric" -> NUMERIC;
                case "alphanumeric" -> ALPHANUMERIC;
                default -> throw ErrorCode.get(OTPErrorCodes.INVALID_OTP_TYPE);
            };
        }
    }
}
