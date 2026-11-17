package com.tlc.social.common.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.social.common.internal.status.SocialErrorCodes;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An enum which represents the social media platform
 *
 * @author Maharaja S
 * @version 1.0
 */
@AllArgsConstructor
@Getter
public enum Platform {

    FACEBOOK((short) 1),
    WHATSAPP((short) 2),
    INSTAGRAM((short) 3),
    ;

    private final short id;

    /**
     * <p>
     * Gets the {@link Platform} based on the provided id
     * </p>
     *
     * @param id Represents the enum id
     * @return the {@link Platform}
     */
    public static Platform get(final short id) {
        return switch (id) {
            case 1 -> FACEBOOK;
            case 2 -> WHATSAPP;
            case 3 -> INSTAGRAM;
            default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_SOCIAL_PLATFORM, "i18n.social.unsupported.platform");
        };
    }

    /**
     * <p>
     * Gets the {@link Platform} based on the provided name
     * </p>
     *
     * @param name Represents the enum name
     * @return the {@link Platform}
     */
    public static Platform get(final String name) {
        return switch (name.toLowerCase()) {
            case "facebook" -> FACEBOOK;
            case "instagram" -> INSTAGRAM;
            case "whatsapp" -> WHATSAPP;
            default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_SOCIAL_PLATFORM, "i18n.social.unsupported.platform");
        };
    }
}
