package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.ActivityAlert;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Loads activity reminder alerts into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ActivityReminderAlertLoader extends AbstractActivityAlertLoader {

    private ActivityReminderAlertLoader() {
    }

    private static class Instance {
        private static final ActivityReminderAlertLoader INSTANCE = new ActivityReminderAlertLoader();
    }

    /**
     * <p>
     * Gets {@link ActivityReminderAlertLoader}.
     * </p>
     *
     * @return {@link ActivityReminderAlertLoader}.
     */
    public static ActivityReminderAlertLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets {@link AppNotificationEventType}.
     * </p>
     *
     * @return {@link AppNotificationEventType}.
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.ACTIVITY_REMINDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents an organization id.
     * @param activities Represents the {@link JsonArray}.
     */
    @Override
    protected void loadAlerts(final long orgId, final JsonArray activities) {
        final int size = activities.size();

        for (int index = 0; index < size; index++) {
            final JsonObject activityJson = activities.getJsonObject(index);
            final Reminder reminder = Reminder.getByName(activityJson.getString(NotificationAttributes.REMINDER));

            if (reminder.isRequired()) {
                final long startTime = activityJson.getLong(NotificationAttributes.START_TIME);
                final ActivityAlert activityReminderAlert = notificationAlertConverter.convertActivityReminderAlert(orgId,
                        computeReminderTime(reminder, startTime), reminder, activityJson);

                notificationAlertManager.storeActivityReminderAlert(activityReminderAlert);
            } else {
                final long activityId = activityJson.getLong(NotificationAttributes.ID);

                if (notificationAlertManager.hasActivityReminderAlert(orgId, activityId)) {
                    notificationAlertManager.deleteActivityReminderAlerts(
                            orgId, activityJson.getLong(NotificationAttributes.ASSIGNEE_ID), List.of(activityId));
                }
            }
        }
    }

    /**
     * <p>
     * Computes the reminder time based on the start time
     * </p>
     *
     * @param reminder  Represents the {@link Reminder}
     * @param startTime Represents the start time
     * @return the converted time
     */
    private long computeReminderTime(final Reminder reminder, final long startTime) {
        final long reminderTime = switch (reminder) {

            case NO_REMINDER -> 0L;
            case FIVE_MINUTES -> TimeUnit.MINUTES.toMillis(5);
            case TEN_MINUTES -> TimeUnit.MINUTES.toMillis(10);
            case FIFTEEN_MINUTES -> TimeUnit.MINUTES.toMillis(15);
            case THIRTY_MINUTES -> TimeUnit.MINUTES.toMillis(30);
        };

        return startTime - reminderTime;
    }
}