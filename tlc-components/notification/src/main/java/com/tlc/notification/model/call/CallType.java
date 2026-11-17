package com.tlc.notification.model.call;

import com.tlc.commons.code.ErrorCode;
import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * Defines the type of call in the Twilio WebRTC system.
 *
 * <p>
 *     This enum represents whether a call is an <b>INCOMING</b> call (from a customer to an agent)
 * or an <b>OUTGOING</b> call (from an agent to a customer).
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum CallType {

    INCOMING((short) 1, "incoming"),
    OUTGOING((short) 2, "outgoing"),
    ;

    private final short id;
    private final String name;

    CallType(final short id, String name) {
        this.id = id;
        this.name = name;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static CallType get(final String type) {
        return switch (type.toLowerCase()) {
            case "outbound", "outgoing" -> OUTGOING;
            default -> INCOMING;
        };
    }

    public static CallType get(final short id) {
        return switch (id) {
            case 1 -> INCOMING;
            case 2 -> OUTGOING;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_CALL_TYPE, "i18n.notification.call.type.invalid");
        };
    }
}
