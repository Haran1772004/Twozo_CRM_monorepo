package com.tlc.crm.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

/**
 * <p>
 * Represents the {@link NotificationState} details
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum NotificationState {

    ON((short) 1, "on"),
    OFF((short) 2, "off");

    private final short id;
    private final String name;

    NotificationState(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * <p>
     * Gets the notification state id
     * </p>
     *
     * @return the notification state id
     */
    public short getId() {
        return this.id;
    }

    /**
     * <p>
     * Gets the notification state name
     * </p>
     *
     * @return the notification state name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the value of {@link NotificationState} for the given id
     *
     * @param id Represents the id of {@link NotificationState}
     * @return {@link NotificationState}
     */
    public static NotificationState get(final short id) {

        return switch (id) {
            case 1 -> ON;
            case 2 -> OFF;
            default -> throw ErrorCode.get(CommonErrorCodes.INVALID_NOTIFICATION_STATE);
        };
    }

    /**
     * Gets the value of {@link NotificationState} for the given id
     *
     * @param name Represents the name of {@link NotificationState}
     * @return {@link NotificationState}
     */
    public static NotificationState getByName(final String name) {

        return switch (name) {
            case "on" -> ON;
            case "off" -> OFF;
            default -> throw ErrorCode.get(CommonErrorCodes.INVALID_NOTIFICATION_STATE);
        };
    }
}
