package com.tlc.crm.notifications.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.model.ChatNotificationSettings;
import com.tlc.crm.notification.app.model.InAppNotificationSoundSettings;
import com.tlc.crm.notification.app.model.MentionAlertNotificationSettings;
import com.tlc.crm.notification.app.model.UserNotificationSettings;
import com.tlc.crm.notification.app.model.ActivityReminderNotificationSettings;
import com.tlc.crm.notification.app.model.EmailReminderNotificationSettings;
import com.tlc.crm.notification.app.model.ReminderNotificationSettings;
import com.tlc.crm.notification.app.model.EmailNotificationSettings;
import com.tlc.crm.notification.app.model.SummaryNotificationSettings;
import com.tlc.crm.notification.app.model.AssignmentNotificationSettings;
import com.tlc.crm.notification.app.model.TrackingNotificationSettings;
import com.tlc.crm.notifications.fields.NotificationFields;

/**
 * <p>
 * Provides different ways to convert the data from database into {@link UserNotificationSettings}
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class UserNotificationSettingsConverter {

    private UserNotificationSettingsConverter() {
    }

    private static class Instance {
        private static final UserNotificationSettingsConverter INSTANCE = new UserNotificationSettingsConverter();
    }

    /**
     * Gets the {@link UserNotificationSettingsConverter} Instance
     *
     * @return {@link UserNotificationSettingsConverter}
     */
    public static UserNotificationSettingsConverter getInstance() {
        return UserNotificationSettingsConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link UserNotificationSettings}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents an user id
     * @param jsonObject Represents the {@link JsonObject} that contains request values
     * @return the {@link UserNotificationSettings}
     */
    public UserNotificationSettings convert(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final UserNotificationSettings userNotificationSettings = new UserNotificationSettings();
        final JsonObject emailAlertJson = jsonObject.getJsonObject(NotificationFields.EMAIL_ALERT.getName());
        final JsonObject summaryJson = jsonObject.getJsonObject(NotificationFields.SUMMARY.getName());
        final JsonObject reminderJson = jsonObject.getJsonObject(NotificationFields.REMINDER.getName());
        final JsonObject assignmentJson = jsonObject.getJsonObject(NotificationFields.ASSIGNMENT.getName());
        final JsonObject trackingJson = jsonObject.getJsonObject(NotificationFields.TRACKING.getName());
        final JsonObject chatJson = jsonObject.getJsonObject(NotificationFields.CHAT.getName());
        final JsonObject mentionAlertJson = jsonObject.getJsonObject(NotificationFields.MENTION_ALERT.getName());
        final JsonObject notificationSoundJson = jsonObject.getJsonObject(NotificationFields.NOTIFICATION_SOUND.getName());
        final ActivityReminderNotificationSettings activityReminderNotificationSettings = new ActivityReminderNotificationSettings(reminderJson, ReminderNotificationSettings.ReminderType.ACTIVITY);
        final EmailReminderNotificationSettings emailReminderNotificationSettings = new EmailReminderNotificationSettings(reminderJson, ReminderNotificationSettings.ReminderType.EMAIL);

        userNotificationSettings.setOrgId(orgId);
        userNotificationSettings.setUserId(userId);
        userNotificationSettings.setEmailNotificationSettings(new EmailNotificationSettings(emailAlertJson));
        userNotificationSettings.setSummaryNotificationSettings(new SummaryNotificationSettings(summaryJson));
        userNotificationSettings.setActivityReminderNotificationSettings(activityReminderNotificationSettings);
        userNotificationSettings.setEmailReminderNotificationSettings(emailReminderNotificationSettings);
        userNotificationSettings.setAssignmentNotificationSettings(new AssignmentNotificationSettings(assignmentJson));
        userNotificationSettings.setTrackingNotificationSettings(new TrackingNotificationSettings(trackingJson));
        userNotificationSettings.setChatNotificationSettings(new ChatNotificationSettings(chatJson));
        userNotificationSettings.setNoteMentionNotificationSettings(new MentionAlertNotificationSettings(mentionAlertJson));
        userNotificationSettings.setInAppNotificationSoundSettings(new InAppNotificationSoundSettings(notificationSoundJson));

        return userNotificationSettings;
    }

    /**
     * <p>
     * Converts the {@link UserNotificationSettings} into {@link JsonObject}.
     * </p>
     *
     * @param userNotificationSettings Represents the entity of {@link UserNotificationSettings}
     * @return entity of {@link JsonObject}
     */
    public JsonObject convertNotificationSettingsToJson(final UserNotificationSettings userNotificationSettings) {
        final ActivityReminderNotificationSettings activityReminderNotificationSettings = userNotificationSettings.getActivityReminderNotificationSettings();
        final EmailReminderNotificationSettings emailReminderNotificationSettings = userNotificationSettings.getEmailReminderNotificationSettings();
        final JsonObject jsonObject = Json.object();
        final JsonObject settingsObject = Json.object();
        final JsonObject reminderConfigObject = Json.object();

        reminderConfigObject.put(emailReminderNotificationSettings.getReminderType().getType(), emailReminderNotificationSettings.configAsJson());
        reminderConfigObject.put(activityReminderNotificationSettings.getReminderType().getType(), activityReminderNotificationSettings.configAsJson());

        settingsObject.put(NotificationFields.EMAIL_ALERT.getName(), userNotificationSettings.getEmailNotificationSettings().configAsJson());
        settingsObject.put(NotificationFields.SUMMARY.getName(), userNotificationSettings.getSummaryNotificationSettings().configAsJson());
        settingsObject.put(NotificationFields.ASSIGNMENT.getName(), userNotificationSettings.getAssignmentNotificationSettings().configAsJson());
        settingsObject.put(NotificationFields.TRACKING.getName(), userNotificationSettings.getTrackingNotificationSettings().configAsJson());
        settingsObject.put(NotificationFields.REMINDER.getName(), reminderConfigObject);
        settingsObject.put(NotificationFields.CHAT.getName(), userNotificationSettings.getChatNotificationSettings().configAsJson());
        settingsObject.put(NotificationFields.MENTION_ALERT.getName(), userNotificationSettings.getMentionsAlertNotificationSettings().configAsJson());
        settingsObject.put(NotificationFields.NOTIFICATION_SOUND.getName(), userNotificationSettings.getInAppNotificationSoundSettings().configAsJson());

        jsonObject.put(NotificationFields.ID.getName(), userNotificationSettings.id());
        jsonObject.put(NotificationFields.ORG_ID.getName(), userNotificationSettings.orgId());
        jsonObject.put(NotificationFields.SETTINGS.getName(), settingsObject);

        return jsonObject;
    }
}
