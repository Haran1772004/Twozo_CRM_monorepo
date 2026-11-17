package com.tlc.crm.common.sync;

import com.tlc.commons.code.ErrorCode;

import com.tlc.notification.internal.status.NotificationErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Represents the synchronization mode.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum SyncMode {

    /**
     * Performs one-way synchronization.
     */
    ONE_WAY("oneWay", 1),

    /**
     * Perform two-way synchronization.
     */
    TWO_WAY("twoWay", 2),
    ;

    private final int id;
    private final String mode;

    SyncMode(final String mode, final int id) {
        this.mode = mode;
        this.id = id;
    }

    public int id() {
        return id;
    }

    public String getMode() {
        return mode;
    }

    /**
     * <p>
     * Gets the {@link SyncMode}.
     * </p>
     *
     * @param id Represents the sync mode id
     * @return {@link SyncMode}
     */
    public static SyncMode get(final int id) {
        return switch (id) {
            case 1 -> ONE_WAY;
            case 2 -> TWO_WAY;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_SYNC_MODE,
                    "i18n.notification.sync.mode.invalid");
        };
    }


    /**
     * <p>
     * Gets the {@link SyncMode}.
     * </p>
     *
     * @param mode Represents the mode
     * @return {@link SyncMode}
     */
    public static SyncMode getMode(final String mode) {
        return switch (mode) {
            case "oneWay" -> ONE_WAY;
            case "twoWay" -> TWO_WAY;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_SYNC_MODE,
                    "i18n.notification.sync.mode.invalid");
        };
    }

    /**
     * <p>
     * Checks whether the input given is matching with {@link SyncMode}.
     * </p>
     *
     * @param syncMode     Represents the value of {@link SyncMode}.
     * @param syncModeName input value referring sync mode name.
     * @return whether the input is matching.
     */
    public static boolean isEquals(final SyncMode syncMode, final String syncModeName) {
        if (Objects.nonNull(syncMode)) {
            return syncMode.name().equalsIgnoreCase(syncModeName);
        }

        return false;
    }
}
