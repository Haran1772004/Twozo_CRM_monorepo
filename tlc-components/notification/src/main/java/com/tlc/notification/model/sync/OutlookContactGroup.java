package com.tlc.notification.model.sync;

import com.tlc.commons.code.ErrorCode;
import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * <p>
 * Represents the Outlook's contact groups.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum OutlookContactGroup implements ContactGroup {

    ALL_CONTACTS(1),
    ;

    private final int id;

    OutlookContactGroup(final int id) {
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
     * Gets the {@link OutlookContactGroup} by id.
     *
     * @param id Represents the id of the group.
     * @return {@link OutlookContactGroup}.
     */
    public static OutlookContactGroup valueOf(final int id) {

        if (1 == id) {
            return ALL_CONTACTS;
        } else {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_GROUP);
        }
    }
}
