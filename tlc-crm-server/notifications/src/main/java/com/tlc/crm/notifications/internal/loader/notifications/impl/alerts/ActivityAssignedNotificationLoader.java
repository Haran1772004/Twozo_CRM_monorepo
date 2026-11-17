package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.notification.app.model.AssignmentNotificationSettings;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationAttributes;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Loads activity assigned notification into DB.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ActivityAssignedNotificationLoader extends AbstractActivityAlertLoader {

    private ActivityAssignedNotificationLoader() {
    }

    private static class Instance {
        private static final ActivityAssignedNotificationLoader INSTANCE = new ActivityAssignedNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link ActivityAssignedNotificationLoader}.
     * </p>
     *
     * @return {@link ActivityAssignedNotificationLoader}.
     */
    public static ActivityAssignedNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id.
     * @param activities {@link JsonArray} Collection.
     */
    @Override
    protected void loadAlerts(final long orgId, final JsonArray activities) {
        final Long currentUser = LoggedInUserInfo.getUser().id();
        final Long assigneeId = activities.getJsonObject(NumericalConstant.ZERO).getLong(NotificationAttributes.ASSIGNEE_ID);

        if (Objects.equals(currentUser, assigneeId)) {
            return;
        }
        final Optional<AssignmentNotificationSettings> settings =
                notificationSettingsManager.retrieveAssignmentNotificationSettings(orgId, assigneeId);

        if (settings.isPresent() && settings.get().isActivityAssigned()) {
            final long size = activities.size();

            for (int index = 0; index < size; index++) {
                final JsonObject activityJson = activities.getJsonObject(index);

                notificationAlertManager.storeNewActivityAlert(notificationAlertConverter.convertNewActivityAlert(
                        orgId, activityJson));
            }
        }
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
        return AppNotificationEventType.NEW_ACTIVITY;
    }
}
