package com.tlc.crm.notifications.internal.handler.impl;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.notification.email.api.model.NotificationTrackingDetails;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.model.TrackingNotificationSettings;
import com.tlc.crm.notifications.internal.handler.registry.NotifiableModelHandlerRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Handles {@link NotificationTrackingDetails} for notification/alert purposes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotifiableModelHandlerRegistry.class)
public final class NotifiableTrackingHandler extends AbstractNotifiableModelHandler<NotificationTrackingDetails> {

    private NotifiableTrackingHandler() {
    }

    private static class Instance {
        private static final NotifiableTrackingHandler INSTANCE = new NotifiableTrackingHandler();
    }

    /**
     * <p>
     * Gets {@link NotifiableTrackingHandler}.
     * </p>
     *
     * @return {@link NotifiableTrackingHandler}.
     */
    public static NotifiableTrackingHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * @param orgId                       Represents an organization id.
     * @param notificationTrackingDetails Collection {@link NotificationTrackingDetails}.
     */
    @Override
    public void handle(final Long orgId, final Collection<NotificationTrackingDetails> notificationTrackingDetails) {
        final Collection<AppNotificationEventType> notificationEventTypes = Arrays.asList(
                NotificationTrackingDetails.class.getAnnotation(Notifiable.class).types());
        final Optional<Long> userId = notificationTrackingDetails.stream().findFirst().map(NotificationTrackingDetails::getUserId);

        if (userId.isPresent()) {
            final Optional<TrackingNotificationSettings> trackingNotificationSettings = notificationSettingsManager.retrieveTrackingNotificationSettings(orgId, userId.get());

            if (trackingNotificationSettings.isPresent()) {
                final TrackingNotificationSettings settings = trackingNotificationSettings.get();

                if (!settings.isEmailOpened()) {
                    notificationEventTypes.remove(AppNotificationEventType.EMAIL_OPENED);
                }

                if (!settings.isEmailLinkClicked()) {
                    notificationEventTypes.remove(AppNotificationEventType.EMAIL_CLICKED);
                }
            }
            load(orgId, notificationTrackingDetails, notificationEventTypes);
        }
    }

    /**
     * <p>
     * Gets {@link CrmModelType}.
     * </p>
     *
     * @return {@link CrmModelType}.
     */
    @Override
    public CrmModelType getType() {
        return CrmModelType.TRACKING;
    }
}