package com.tlc.crm.notifications.internal.loader.registry;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.internal.loader.notifications.AppNotificationLoader;
import com.tlc.validator.TlcModel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Holds {@link AppNotificationLoader} instances .
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class NotificationLoaderRegistry implements
        AutoServiceRegistry<AppNotificationEventType, AppNotificationLoader<? extends TlcModel>> {

    private final Map<AppNotificationEventType, AppNotificationLoader<? extends TlcModel>> notificationLoaderMap;

    private NotificationLoaderRegistry() {
        notificationLoaderMap = new EnumMap<>(AppNotificationEventType.class);
    }

    private static class Instance {
        private static final NotificationLoaderRegistry INSTANCE = new NotificationLoaderRegistry();
    }

    /**
     * <p>
     * Gets {@link NotificationLoaderRegistry}.
     * </p>
     *
     * @return {@link NotificationLoaderRegistry}.
     */
    public static NotificationLoaderRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Puts instances.
     * </p>
     *
     * @param eventType {@link AppNotificationEventType}.
     * @param loader    {@link AppNotificationLoader}.
     */
    public void put(final AppNotificationEventType eventType, final AppNotificationLoader<? extends TlcModel> loader) {

        if (!eventType.isAlertRequired()) {
            notificationLoaderMap.put(eventType, loader);
        }
    }

    /**
     * <p>
     * Opts {@link Optional<AppNotificationLoader>}.
     * </p>
     *
     * @param eventType {@link AppNotificationEventType}.
     * @return {@link Optional<AppNotificationLoader>}.
     */
    public Optional<AppNotificationLoader<? extends TlcModel>> opt(final AppNotificationEventType eventType) {

        if (!eventType.isAlertRequired()) {
            return Optional.ofNullable(notificationLoaderMap.get(eventType));
        }
        return Optional.empty();
    }

}