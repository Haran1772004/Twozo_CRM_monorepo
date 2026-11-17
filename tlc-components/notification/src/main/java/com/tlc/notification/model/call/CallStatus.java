package com.tlc.notification.model.call;

import com.tlc.commons.code.ErrorCode;

import com.tlc.notification.internal.status.NotificationErrorCodes;

import java.util.Collection;
import java.util.List;

/**
 * Enum representing different statuses of a call.
 * <p>
 * This enum provides status identifiers and methods to retrieve
 * statuses based on either an ID or a string representation.
 * </p>
 *
 * <p>Handles invalid statuses by throwing an error with an appropriate message.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum CallStatus {

    YET_TO_CONNECT((short) 1),
    INITIATED((short) 2),
    RINGING((short) 3),
    IN_PROGRESS((short) 4),
    COMPLETED((short) 5),
    BUSY((short) 6),
    FAILED((short) 7),
    NO_ANSWER((short) 8),
    CANCELED((short) 9),
    WAITING((short) 10),
    ;

    private final short id;

    CallStatus(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static CallStatus get(final short id) {
        return switch (id) {
            case 1 -> YET_TO_CONNECT;
            case 2 -> INITIATED;
            case 3 -> RINGING;
            case 4 -> IN_PROGRESS;
            case 5 -> COMPLETED;
            case 6 -> BUSY;
            case 7 -> FAILED;
            case 8 -> NO_ANSWER;
            case 9 -> CANCELED;
            case 10 -> WAITING;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_CALL_STATUS, "i18n.notification.call.status.invalid");
        };
    }

    public static CallStatus get(final String type) {
        return switch (type.toLowerCase()) {
            case "initiated", "queued", "started" -> INITIATED;
            case "ringing", "connecting" -> RINGING;
            case "inprogress", "in-progress" -> IN_PROGRESS;
            case "completed", "answered", "hangup", "answer" -> COMPLETED;
            case "busy" -> BUSY;
            case "failed" -> FAILED;
            case "noanswer", "no-answer", "missed", "executive busy" -> NO_ANSWER;
            case "canceled", "cancel" -> CANCELED;
            case "waiting" -> WAITING;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_CALL_STATUS, "i18n.notification.call.status.invalid");
        };
    }

    /**
     * <p>
     * Fetches the completion related status ids.
     * </p>
     *
     * @return Completion related status ids.
     */
    public static Collection<Short> getCompletedStatuses() {
        return List.of(COMPLETED.id, BUSY.id, FAILED.id, NO_ANSWER.id, CANCELED.id);
    }

    /**
     * <p>
     * Returns if the given call status supports in the conversation
     * </p>
     */
    public boolean isCompletedStatus() {
        return this == CallStatus.COMPLETED || this == CallStatus.CANCELED || this == CallStatus.NO_ANSWER
                || this == CallStatus.BUSY || this == CallStatus.FAILED;
    }

}
