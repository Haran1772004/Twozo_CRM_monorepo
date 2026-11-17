package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * A model that holds the activity notification reminder settings
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class ActivityReminderNotificationSettings extends ReminderNotificationSettings {

    public ActivityReminderNotificationSettings(final boolean enabled, final ReminderTime reminderTime,
                                                final ReminderType reminderType) {
        super(enabled, reminderTime, reminderType);
    }

    public ActivityReminderNotificationSettings(final String config) {
        super(config);
    }

    public ActivityReminderNotificationSettings(final JsonObject jsonObject, final ReminderType reminderType) {
        super(jsonObject, reminderType);
    }
}
