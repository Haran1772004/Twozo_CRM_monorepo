package com.tlc.crm.notifications.api.call.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.call.CallType;

/**
 * Enum representing different call recording preferences.
 * <p>
 * Each record type corresponds to a specific recording rule for handling calls.
 * </p>
 *
 * <ul>
 *     <li>{@link #RECORD_ALL_CALLS} - Records all calls.</li>
 *     <li>{@link #DO_NOT_RECORD_CALLS} - Disables call recording.</li>
 *     <li>{@link #RECORD_INCOMING_CALLS} - Records only incoming calls.</li>
 *     <li>{@link #RECORD_OUTGOING_CALLS} - Records only outgoing calls.</li>
 * </ul>
 *
 * @author Sathish
 * @version 1.0
 */
public enum RecordType {

    RECORD_ALL_CALLS((short) 1),
    DO_NOT_RECORD_CALLS((short) 2),
    RECORD_INCOMING_CALLS((short) 3),
    RECORD_OUTGOING_CALLS((short) 4);

    private final short id;

    RecordType(final short id) {
        this.id = id;
    }

    /**
     * Retrieves the unique ID of the record type.
     *
     * @return The record type ID.
     */
    public short getId() {
        return id;
    }

    /**
     * Retrieves the {@code RecordType} based on the given ID.
     *
     * @param id The record type ID.
     * @return The corresponding {@link RecordType} instance.
     * @throws ErrorCode If the provided ID does not match any valid record type.
     */
    public static RecordType get(final short id) {
        return switch (id) {
            case 1 -> RECORD_ALL_CALLS;
            case 2 -> DO_NOT_RECORD_CALLS;
            case 3 -> RECORD_INCOMING_CALLS;
            case 4 -> RECORD_OUTGOING_CALLS;
            default -> throw ErrorCode.get(
                    NotificationErrorCodes.INVALID_RECORD_TYPE,
                    "I18n.notifications.call.record.type.invalid"
            );
        };
    }

    /**
     * Provides a flag that indicates whether the given {@link CallType} needs the call recording or not.
     *
     * @param callType Represents the {@link CallType}.
     * @return Call recording needed flag.
     */
    public boolean isCallRecordingEnabled(final CallType callType) {
        if (this == RECORD_ALL_CALLS) {
            return true;
        }

        return switch (callType) {
            case INCOMING -> this == RECORD_INCOMING_CALLS;
            case OUTGOING -> this == RECORD_OUTGOING_CALLS;
        };
    }
}