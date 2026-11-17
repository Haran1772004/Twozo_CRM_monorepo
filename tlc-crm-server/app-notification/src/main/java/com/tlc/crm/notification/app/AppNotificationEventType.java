package com.tlc.crm.notification.app;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.model.TlcEnum;
import com.tlc.crm.notification.app.status.AppNotificationErrorCodes;

/**
 * <p>
 * An enum that holds the types of notifications
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum AppNotificationEventType implements TlcEnum {

    CONTACT_ASSIGNED(1, true),
    COMPANY_ASSIGNED(2, true),
    DEAL_ASSIGNED(3, true),
    PRODUCT_ASSIGNED(4, true),
    NEW_ACTIVITY(5, true),
    EMAIL_REPLIED(6, false),
    COLLABORATOR_ADDED(7, true),
    CONTACT_SYNCED(8, false),
    CALENDAR_SYNCED(9, false),
    ADDED_TO_TEAM(10, false),
    ROLE_CHANGED(11, false),
    PRODUCTS_MODULE_ENABLED(12, false),
    EMAIL_RECEIVED(13, false),
    ACTIVITY_REMINDER(14, true),
    EMAIL_OPENED(15, false),
    EMAIL_CLICKED(16, false),//todo team inbox related types
    CHAT_ASSIGNED(17, true),
    CHAT_INITIATED(18, true),
    CONTACT_NOTE_MENTIONED(19, true),
    COMPANY_NOTE_MENTIONED(20, true),
    DEAL_NOTE_MENTIONED(21, true),
    ACTIVITY_PRIVATE_NOTE_MENTIONED(22, true),
    CONTACT_NOTE_COMMENT_MENTIONED(24, true),
    COMPANY_NOTE_COMMENT_MENTIONED(25, true),
    DEAL_NOTE_COMMENT_MENTIONED(26, true),
    EXPORTING(27, false),
    EXPORTED(28, true),
    ;

    private final int id;
    private final boolean isAlertRequired;

    AppNotificationEventType(final int id, final boolean isAlertRequired) {
        this.isAlertRequired = isAlertRequired;
        this.id = id;
    }

    /**
     * <p>
     * Gets the type id
     * </p>
     *
     * @return the type id
     */
    public int getId() {
        return this.id;
    }

    /**
     * <p>
     * Gets the type id
     * </p>
     *
     * @return the type id
     */
    public boolean isAlertRequired() {
        return this.isAlertRequired;
    }

    /**
     * Gets the value of {@link AppNotificationEventType} for the given id
     *
     * @param id Represents the id of {@link AppNotificationEventType}
     * @return {@link AppNotificationEventType}
     */
    public static AppNotificationEventType get(final int id) {

        return switch (id) {
            case 1 -> CONTACT_ASSIGNED;
            case 2 -> COMPANY_ASSIGNED;
            case 3 -> DEAL_ASSIGNED;
            case 4 -> PRODUCT_ASSIGNED;
            case 5 -> NEW_ACTIVITY;
            case 6 -> EMAIL_REPLIED;
            case 7 -> COLLABORATOR_ADDED;
            case 8 -> CONTACT_SYNCED;
            case 9 -> CALENDAR_SYNCED;
            case 10 -> ADDED_TO_TEAM;
            case 11 -> ROLE_CHANGED;
            case 12 -> PRODUCTS_MODULE_ENABLED;
            case 13 -> EMAIL_RECEIVED;
            case 14 -> ACTIVITY_REMINDER;
            case 15 -> EMAIL_OPENED;
            case 16 -> EMAIL_CLICKED;
            case 17 -> CHAT_ASSIGNED;
            case 18 -> CHAT_INITIATED;
            case 19 -> CONTACT_NOTE_MENTIONED;
            case 20 -> COMPANY_NOTE_MENTIONED;
            case 21 -> DEAL_NOTE_MENTIONED;
            case 22 -> ACTIVITY_PRIVATE_NOTE_MENTIONED;
            case 24 -> CONTACT_NOTE_COMMENT_MENTIONED;
            case 25 -> COMPANY_NOTE_COMMENT_MENTIONED;
            case 26 -> DEAL_NOTE_COMMENT_MENTIONED;
            case 27 -> EXPORTING;
            case 28 -> EXPORTED;
            default -> throw ErrorCode.get(AppNotificationErrorCodes.INVALID_NOTIFICATION_TYPE);
        };
    }
}
