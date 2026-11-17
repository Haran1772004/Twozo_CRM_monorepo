package com.tlc.crm.notification.app.service;

import com.tlc.crm.notification.app.AppNotificationEventType;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Holds the services that are required for the notification based on the types
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class NotificationLoaderHolder {

    private final Map<String, Map<AppNotificationEventType, NotificationLoader>> notificationHandlers;

    private NotificationLoaderHolder() {
        notificationHandlers = new HashMap<>();
    }

    private static class Instance {
        private static final NotificationLoaderHolder INSTANCE = new NotificationLoaderHolder();
    }

    /**
     * <p>
     * Gets the {@link NotificationLoaderHolder}.
     * </p>
     *
     * @return {@link NotificationLoaderHolder}
     */
    public static NotificationLoaderHolder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Puts the {@link NotificationLoader} instance into the map
     * </p>
     *
     * @param type                Represents the name of the action
     * @param notificationLoader Holds {@link NotificationLoader} instance
     */
    public void put(final String moduleName, final AppNotificationEventType type, final NotificationLoader notificationLoader) {
        notificationHandlers.computeIfAbsent(moduleName, grpName -> new HashMap<>()).put(type, notificationLoader);
    }

    /**
     * <p>
     * Gets {@link NotificationLoader}.
     * </p>
     *
     * @param type Represents {@link AppNotificationEventType}.
     * @return the {@link NotificationLoader}
     */
    public <T extends NotificationLoader> T get(final AppNotificationEventType type) {
        return (T) notificationHandlers.values().stream().map(notificationHandlers -> notificationHandlers.get(type)).findAny().orElseGet(() -> null);
    }

    /**
     * <p>
     * Removes the registered {@link NotificationLoader} of the given group ID.
     * </p>
     *
     * @param groupName Represents the group name.
     */
    public void remove(final String groupName) {
        notificationHandlers.remove(groupName);
    }
}
