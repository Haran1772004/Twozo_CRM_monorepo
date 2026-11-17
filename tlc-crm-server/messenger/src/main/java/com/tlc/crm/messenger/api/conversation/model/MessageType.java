package com.tlc.crm.messenger.api.conversation.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.messenger.internal.status.MessengerErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Message types.
 * </p>
 *
 * @author Ajay.
 *  @version 1.0.
 */
@AllArgsConstructor
@Getter
public enum MessageType {

    MESSAGE((short) 1, "message"),
    ACTIVITY((short) 2, "activity");

    private final short id;
    private final String name;

    /**
     * <p>
     * Gets the {@link MessageType} based on the provided id.
     * </p>
     *
     * @param id Represents the enum id.
     * @return the {@link MessageType}.
     */
    public static MessageType get(final short id) {
        return switch (id) {
            case 1 -> MESSAGE;
            case 2 -> ACTIVITY;
            default -> throw ErrorCode.get(MessengerErrorCodes.INVALID_MESSAGE_TYPE, "i18n.messenger.chat.invalid.message.type");
        };
    }
}