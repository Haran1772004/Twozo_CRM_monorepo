package com.tlc.notification.model.sync;

import com.tlc.commons.code.ErrorCode;
import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * <p>
 * Represents the Google's contact groups.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum GmailContactGroup implements ContactGroup {

    ALL_CONTACTS(1),
    STARRED(2),
    ;

    private final int id;

    GmailContactGroup(final int id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the group id
     * </p>
     *
     * @return the group id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Gets the {@link GmailContactGroup} by id
     *
     * @param id Represents the id of the group
     * @return {@link GmailContactGroup}
     */
    public static GmailContactGroup valueOf(final int id) {
        return switch (id) {
            case 1 -> ALL_CONTACTS;
            case 2 -> STARRED;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_GROUP);
        };
    }
}
