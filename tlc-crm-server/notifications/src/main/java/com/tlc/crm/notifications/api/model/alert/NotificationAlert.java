package com.tlc.crm.notifications.api.model.alert;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.validator.TlcModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * Pertains to in app alert related fields.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Setter
@Getter
@NoArgsConstructor
public abstract class NotificationAlert implements TlcModel {

    private Long id;
    private Long orgId;
    private Long timeStamp;
    private Category category;
    private Severity severity;
    private JsonObject content;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Alert categories.
     * </p>
     */
    @Getter
    @AllArgsConstructor
    public enum Category {
        ACTIVITY_REMINDER((short) 1),
        CHAT_INITIATED((short) 2),
        CHAT_ASSIGNED((short) 3),
        CONTACT_ASSIGNED((short) 4),
        COMPANY_ASSIGNED((short) 5),
        DEAL_ASSIGNED((short) 6),
        PRODUCT_ASSIGNED((short) 7),
        NEW_ACTIVITY((short) 8),
        COLLABORATOR_ADDED((short) 9),
        CONTACT_NOTE_MENTIONED((short) 10),
        COMPANY_NOTE_MENTIONED((short) 11),
        DEAL_NOTE_MENTIONED((short) 12),
        ACTIVITY_PRIVATE_NOTE_MENTIONED((short) 13),
        CONTACT_NOTE_COMMENT_MENTIONED((short) 14),
        COMPANY_NOTE_COMMENT_MENTIONED((short) 15),
        DEAL_NOTE_COMMENT_MENTIONED((short) 16),
        EXPORTED((short) 17),
        ;

        private final short id;

        /**
         * <p>
         * Gets the {@link Category}.
         * </p>
         *
         * @param id Represents the enum id.
         * @return the {@link Category}.
         */
        public static Category get(final short id) {
            return switch (id) {
                case 1 -> ACTIVITY_REMINDER;
                case 2 -> CHAT_INITIATED;
                case 3 -> CHAT_ASSIGNED;
                case 4 -> CONTACT_ASSIGNED;
                case 5 -> COMPANY_ASSIGNED;
                case 6 -> DEAL_ASSIGNED;
                case 7 -> PRODUCT_ASSIGNED;
                case 8 -> NEW_ACTIVITY;
                case 9 -> COLLABORATOR_ADDED;
                case 10 -> CONTACT_NOTE_MENTIONED;
                case 11 -> COMPANY_NOTE_MENTIONED;
                case 12 -> DEAL_NOTE_MENTIONED;
                case 13 -> ACTIVITY_PRIVATE_NOTE_MENTIONED;
                case 14 -> CONTACT_NOTE_COMMENT_MENTIONED;
                case 15 -> COMPANY_NOTE_COMMENT_MENTIONED;
                case 16 -> DEAL_NOTE_COMMENT_MENTIONED;
                case 17 -> EXPORTED;
                default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_ALERT_TYPE, "i18n.app.alert.invalid.type");
            };
        }
    }

    /**
     * <p>
     * Alert severity.
     * </p>
     */
    @Getter
    @AllArgsConstructor
    public enum Severity {

        ALERT((short) 1),
        WARNING((short) 2),
        ERROR((short) 3),
        ;

        private final short id;

        /**
         * <p>
         * Gets the {@link Category}.
         * </p>
         *
         * @param id Represents the enum id.
         * @return the {@link Category}.
         */
        public static Severity get(final short id) {
            return switch (id) {
                case 1 -> ALERT;
                case 2 -> WARNING;
                case 3 -> ERROR;
                default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_SEVERITY_TYPE, "i18n.app.alert.severity.type");
            };
        }
    }
}