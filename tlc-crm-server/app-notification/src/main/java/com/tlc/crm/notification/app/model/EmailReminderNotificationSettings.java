package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * A model that holds the email notification reminder settings
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class EmailReminderNotificationSettings extends ReminderNotificationSettings {

    public EmailReminderNotificationSettings(final boolean enabled, final ReminderTime reminderTime,
                                             final ReminderType reminderType) {
        super(enabled, reminderTime, reminderType);
    }

    public EmailReminderNotificationSettings(final String config) {
        super(config);
    }

    public EmailReminderNotificationSettings(final JsonObject jsonObject, final ReminderType reminderType) {
        super(jsonObject, reminderType);
    }
}
