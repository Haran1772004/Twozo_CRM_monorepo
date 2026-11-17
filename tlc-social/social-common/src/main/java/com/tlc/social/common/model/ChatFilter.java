package com.tlc.social.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An enum which contains common chat filters
 *
 * @author Maharaja S
 * @version 1.0
 */
@AllArgsConstructor
@Getter
public enum ChatFilter {

    UNREAD((short) 1, "Unread");

    private final short id;
    private final String name;

    /**
     * <p>
     * Gets the {@link ChatFilter} by the provided id
     * </p>
     *
     * @param id Represents the id of the enum
     * @return the {@link ChatFilter}
     */
    public static ChatFilter get(final short id) {
        return switch (id) {
            case 1 -> UNREAD;
            default -> throw ErrorCode.get(SocialErrorCodes.INVALID_CHAT_FILTER, "i18n.social.chat.invalid.filter");
        };
    }

    /**
     * <p>
     * Gets the {@link ChatFilter} by the provided name
     * </p>
     *
     * @param name Represents the name of the enum
     * @return the {@link ChatFilter}
     */
    public static ChatFilter get(final String name) {
        return switch (name.toLowerCase()) {
            case "unread" -> UNREAD;
            default -> throw ErrorCode.get(SocialErrorCodes.INVALID_CHAT_FILTER, "i18n.social.chat.invalid.filter");
        };
    }
}
