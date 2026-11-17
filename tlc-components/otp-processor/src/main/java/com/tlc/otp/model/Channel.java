package com.tlc.otp.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.otp.code.OTPErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Holds the otp channel types
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public enum Channel {

    EMAIL((short) 1, "email"),
    PHONE((short) 2, "phone"),
    ;

    private final short id;
    private final String type;

    /**
     * <p>
     * Fetches the {@link Channel} for the given id
     * </p>
     *
     * @param id Represents an id
     * @return The {@link Channel}
     */
    public static Channel getChannelById(final short id) {
        return switch (id) {
            case 1 -> EMAIL;
            case 2 -> PHONE;
            default -> throw ErrorCode.get(OTPErrorCodes.INVALID_OTP_CHANNEL);
        };
    }

    /**
     * <p>
     * Fetches the {@link Channel} for the given channel name
     * </p>
     *
     * @param type Represents the channel name
     * @return The {@link Channel}
     */
    public static Channel getChannelByType(final String type) {
        return switch (type.toLowerCase()) {
            case "email" -> EMAIL;
            case "phone" -> PHONE;
            default -> throw ErrorCode.get(OTPErrorCodes.INVALID_OTP_CHANNEL);
        };
    }
}