package com.tlc.crm.email.notification.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * <p>
 * An enum defines the send type of email/message either single or bulk
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public enum MessageDispatchType {

    SINGLE((short) 1), BULK((short) 2);

    private final short id;

    MessageDispatchType(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the send type for message
     * </p>
     *
     * @return the send type
     */
    public short getId() {
        return this.id;
    }

    /**
     * Gets the {@link MessageDispatchType} by id
     *
     * @param messageDispatchTypeId represents the id of the message dispatch type.
     * @return {@link MessageDispatchType}
     */
    public static MessageDispatchType get(final short messageDispatchTypeId) {

        return switch (messageDispatchTypeId) {
            case 1 -> SINGLE;
            case 2 -> BULK;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_MESSAGE_DISPATCH_TYPE);
        };
    }
}
