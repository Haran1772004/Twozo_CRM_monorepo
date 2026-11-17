package com.tlc.crm.notifications.internal.loader.registry;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.internal.loader.notifications.AppNotificationAlertLoader;
import com.tlc.validator.TlcModel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Holds {@link AppNotificationAlertLoader} instances .
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class NotificationAlertLoaderRegistry implements
        AutoServiceRegistry<AppNotificationEventType, AppNotificationAlertLoader<? extends TlcModel>> {

    private final Map<AppNotificationEventType, AppNotificationAlertLoader<? extends TlcModel>> notificationAlertLoaderMap;

    private NotificationAlertLoaderRegistry() {
        notificationAlertLoaderMap = new EnumMap<>(AppNotificationEventType.class);
    }

    private static class Instance {
        private static final NotificationAlertLoaderRegistry INSTANCE = new NotificationAlertLoaderRegistry();
    }

    /**
     * <p>
     * Gets {@link NotificationAlertLoaderRegistry}.
     * </p>
     *
     * @return {@link NotificationAlertLoaderRegistry}.
     */
    public static NotificationAlertLoaderRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}.
     * </p>
     *
     * @param eventType {@link AppNotificationEventType}.
     * @param loader    {@link AppNotificationAlertLoader}.
     */
    public void put(final AppNotificationEventType eventType,
                    final AppNotificationAlertLoader<? extends TlcModel> loader) {

        if (eventType.isAlertRequired()) {
            notificationAlertLoaderMap.put(eventType, loader);
        }
    }

    /**
     * <p>
     * Opts {@link AppNotificationAlertLoader}.
     * </p>
     *
     * @param eventType {@link AppNotificationEventType}.
     * @return {@link AppNotificationAlertLoader}.
     */
    public Optional<AppNotificationAlertLoader<?>> opt(final AppNotificationEventType eventType) {

        if (eventType.isAlertRequired()) {
            return Optional.ofNullable(notificationAlertLoaderMap.get(eventType));
        }
        return Optional.empty();
    }

}