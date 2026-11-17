package com.tlc.notification.model.sync;

import com.tlc.commons.code.ErrorCode;

import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * <p>
 * Represents the synchronization connection status.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum ConnectionStatus {

    /**
     * Indicates an active or connected status.
     */
    ACTIVE((short) 1),

    /**
     * Indicates an inactive or disconnected status.
     */
    INACTIVE((short) 2),

    /**
     * Indicates a removed status.
     */
    REMOVED((short) 3),
    ;

    private final short id;

    ConnectionStatus(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link ConnectionStatus} by id
     * </p>
     *
     * @param id Represents the connection status id
     * @return {@link ConnectionStatus}
     */
    public static ConnectionStatus get(final short id) {
        return switch (id) {
            case 1 -> ACTIVE;
            case 2 -> INACTIVE;
            case 3 -> REMOVED;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_CONNECTION_STATUS,
                    "i18n.notification.connection.status.invalid");
        };
    }

    /**
     * <p>
     * Checks whether the connection status is active.
     * </p>
     *
     * @return true if the status is {@code ACTIVE}, false otherwise
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * <p>
     * Checks whether the connection status is removed.
     * </p>
     *
     * @return true if the status is {@code REMOVED}, false otherwise
     */
    public boolean isRemoved() {
        return this == REMOVED;
    }
}
