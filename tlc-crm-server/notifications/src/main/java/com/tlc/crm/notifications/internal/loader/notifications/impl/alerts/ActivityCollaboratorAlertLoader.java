package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationAttributes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Loads activity collaborator alert.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ActivityCollaboratorAlertLoader extends AbstractActivityAlertLoader {

    private ActivityCollaboratorAlertLoader() {
    }

    private static class Instance {
        private static final ActivityCollaboratorAlertLoader INSTANCE = new ActivityCollaboratorAlertLoader();
    }

    /**
     * <p>
     * Gets {@link ActivityCollaboratorAlertLoader}.
     * </p>
     *
     * @return {@link ActivityCollaboratorAlertLoader}.
     */
    public static ActivityCollaboratorAlertLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id.
     * @param activities {@link ActivityHolder} Collection.
     */
    @Override
    protected void loadAlerts(final long orgId, final JsonArray activities) {
        final Long userId = LoggedInUserInfo.getUser().id();
        final long size = activities.size();

        for (int index = 0; index < size; index++) {
            final JsonObject json = activities.getJsonObject(index);
            final JsonArray collaboratorIds = json.optJsonArray(NotificationAttributes.COLLABORATOR_IDS);

            if (Objects.nonNull(collaboratorIds)) {
                final Collection<Long> notifiableUserIds = new HashSet<>();
                final int collaboratorSize = collaboratorIds.size();

                for (int collaboratorIndex = 0; collaboratorIndex < collaboratorSize; collaboratorIndex++) {
                    final Long id = collaboratorIds.getLong(collaboratorIndex);

                    if (!Objects.equals(userId, id)) {
                        notifiableUserIds.add(id);
                    }
                }

                if (!notifiableUserIds.isEmpty()) {
                    final JsonObject activityJson = Json.object();

                    activityJson.put(NotificationAttributes.ID, json.getLong(NotificationAttributes.ID));
                    activityJson.put(NotificationAttributes.ACTIVITY_TITLE, json.getString(NotificationAttributes.ACTIVITY_TITLE));

                    notificationAlertManager.storeActivityCollaboratorAlert(notificationAlertConverter.convertActivityCollaboratorAlert(
                            orgId, activityJson), notifiableUserIds);
                }
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
        return AppNotificationEventType.COLLABORATOR_ADDED;
    }
}