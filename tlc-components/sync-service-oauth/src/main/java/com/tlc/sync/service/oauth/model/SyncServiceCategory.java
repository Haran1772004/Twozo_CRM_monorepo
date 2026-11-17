package com.tlc.sync.service.oauth.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;

/**
 * <p>
 * Enum that represents the type of service
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum SyncServiceCategory {

    EMAIL((short) 1),
    CALENDAR((short) 2),
    CONTACT((short) 3),
    FACEBOOK((short) 4),
    INSTAGRAM((short) 5),
    WHATSAPP((short) 6),
    ;

    private final short id;

    SyncServiceCategory(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the type id
     * </p>
     *
     * @return the type id
     */
    public short getId() {
        return this.id;
    }

    /**
     * Gets the {@link SyncServiceCategory} by id
     *
     * @param id Represents the id of the service type
     * @return {@link SyncServiceCategory}
     */
    public static SyncServiceCategory valueOf(final short id) {
        return switch (id) {
            case 1 -> EMAIL;
            case 2 -> CALENDAR;
            case 3 -> CONTACT;
            case 4 -> FACEBOOK;
            case 5 -> INSTAGRAM;
            case 6 -> WHATSAPP;
            default -> throw ErrorCode.get(SyncErrorCodes.INVALID_SERVICE_TYPE);
        };
    }

    /**
     * <p>
     * Checks whether the provided {@link SyncServiceCategory} is social category or not
     * </p>
     *
     * @param syncServiceCategory Represents the {@link SyncServiceCategory}
     * @return true if it comes under social category, false otherwise
     */
    public static boolean isSocialCategory(final SyncServiceCategory syncServiceCategory) {
        return switch (syncServiceCategory) {
            case FACEBOOK, WHATSAPP, INSTAGRAM -> true;
            default -> false;
        };
    }
}
