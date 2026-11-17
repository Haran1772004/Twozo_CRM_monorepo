package com.tlc.crm.notifications.internal.handler.impl;

import com.tlc.crm.core.api.iam.manager.UserNotificationSettingsManager;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.internal.handler.NotifiableModelHandler;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Handles notifiable models.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractNotifiableModelHandler<T extends TlcModel> implements NotifiableModelHandler<T> {

    protected final UserNotificationSettingsManager notificationSettingsManager;

    private final NotificationAlertLoaderRegistry notificationAlertLoaderRegistry;
    private final NotificationLoaderRegistry notificationLoaderRegistry;

    protected AbstractNotifiableModelHandler() {
        notificationSettingsManager = UserNotificationSettingsManager.getInstance();
        notificationAlertLoaderRegistry = NotificationAlertLoaderRegistry.getInstance();
        notificationLoaderRegistry = NotificationLoaderRegistry.getInstance();
    }

    /**
     * <p>
     * Loads models in loaders.
     * </p>
     *
     * @param orgId                  Represents an organization id
     * @param models                 Collection {@link TlcModel}.
     * @param notificationEventTypes {@link AppNotificationEventType}.
     */
    protected void load(final Long orgId, final Collection<T> models,
                        final Collection<AppNotificationEventType> notificationEventTypes) {

        notificationEventTypes.forEach(eventType -> {

            if (eventType.isAlertRequired()) {
                notificationAlertLoaderRegistry.opt(eventType).ifPresent(alertLoader -> alertLoader.load(orgId, (Collection) models));
            } else {
                notificationLoaderRegistry.opt(eventType).ifPresent(alertLoader -> alertLoader.load(orgId, (Collection) models));
            }
        });
    }
}