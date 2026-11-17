package com.tlc.crm.notification.app.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;
import com.tlc.crm.notification.app.status.AppNotificationErrorCodes;

import java.util.Objects;

/**
 * <p>
 * A model that holds the notification reminder settings
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class ReminderNotificationSettings extends NotificationSettings {

    private final ReminderType reminderType;
    private final boolean isEnabled;
    private final ReminderTime reminderTime;

    public ReminderNotificationSettings(final boolean enabled, final ReminderTime reminderTime,
                                        final ReminderType reminderType) {
        this.isEnabled = enabled;
        this.reminderTime = reminderTime;
        this.reminderType = reminderType;
    }

    public ReminderNotificationSettings(final String config) {
        final JsonObject jsonObject = Json.object(config);

        this.reminderType = ReminderType.get(jsonObject.getString(AppNotificationAttributes.REMINDER_TYPE));
        this.isEnabled = jsonObject.getBoolean(AppNotificationAttributes.IS_ENABLED);
        this.reminderTime = ReminderTime.get(jsonObject.getShort(AppNotificationAttributes.TIME));
    }

    public ReminderNotificationSettings(final JsonObject jsonObject, final ReminderType reminderType) {
        final JsonObject remainderJson = jsonObject.getJsonObject(reminderType.getType());

        this.reminderType = reminderType;
        this.isEnabled = remainderJson.optBoolean(AppNotificationAttributes.IS_ENABLED, false);
        this.reminderTime = ReminderTime.get(remainderJson.optShort(AppNotificationAttributes.TIME, (short) 1));
    }

    /**
     * <p>
     * Represents the reminder type in notification settings configuration
     * </p>
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum ReminderType {

        ACTIVITY("activity"),
        EMAIL("email"),
        ;

        private final String name;

        ReminderType(final String name) {
            this.name = name;
        }

        public String getType() {
            return name;
        }

        public static ReminderType get(final String reminderType) {
            return switch (reminderType.toLowerCase()) {
                case "activity" -> ACTIVITY;
                case "email" -> EMAIL;
                default -> throw ErrorCode.get(AppNotificationErrorCodes.INVALID_REMINDER_TYPE,
                        "i18n.notifications.app.settings.invalid.reminderType");
            };
        }
    }

    /**
     * <p>
     * Represents the reminder time in notification settings configuration
     * </p>
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum ReminderTime {
        FIFTEEN_MINUTES((short) 1, (short) 15),
        THIRTY_MINUTES((short) 2, (short) 30),
        FORTY_FIVE_MINUTES((short) 3, (short) 45),
        SIXTY_MINUTES((short) 4, (short) 60),
        ;

        private final short id;
        private final short value;

        ReminderTime(final short id, final short value) {
            this.id = id;
            this.value = value;
        }

        public short getId() {
            return id;
        }

        public short getValue() {
            return value;
        }

        public static ReminderTime get(final short id) {
            return switch (id) {
                case 1 -> FIFTEEN_MINUTES;
                case 2 -> THIRTY_MINUTES;
                case 3 -> FORTY_FIVE_MINUTES;
                case 4 -> SIXTY_MINUTES;
                default -> throw ErrorCode.get(AppNotificationErrorCodes.INVALID_REMINDER_TIME,
                        "i18n.notifications.app.settings.invalid.reminderTime");
            };
        }
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public ReminderTime getReminderTime() {
        return reminderTime;
    }

    /**
     * <p>
     * Converts the {@link ReminderNotificationSettings} values into a JSON string representation.
     * </p>
     *
     * @return A string representing the notification reminder config in the specified format.
     */
    @Override
    public String getConfigAsJsonString() {

        if (Objects.isNull(jsonString) || jsonString.trim().isEmpty()) {
            jsonString = constructJsonString();
        }
        return jsonString;
    }

    /**
     * Constructs a JSON string representation of the notification reminder config.
     *
     * @return  A string representing the notification reminder config.
     */
    private String constructJsonString() {
        return String.format("{\"reminderType\":\"%s\",\"isEnabled\":%b,\"time\":%d}",
                reminderType.getType(), isEnabled, reminderTime.getId());
    }

    /**
     * <p>
     * Converts the configuration properties into a JSON object
     * </p>
     *
     * @return A JSON representation of the configuration settings
     */
    @Override
    public JsonObject configAsJson() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(AppNotificationAttributes.IS_ENABLED, isEnabled);
        jsonObject.put(AppNotificationAttributes.TIME, reminderTime.getId());

        return jsonObject;
    }
}