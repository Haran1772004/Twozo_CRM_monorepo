package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;

/**
 * <p>
 * Loads calendar sync notification into DB.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class CalendarSyncNotificationLoader extends AbstractSyncNotificationLoader<CalendarSyncSettings> {

    private CalendarSyncNotificationLoader() {
    }

    private static class Instance {
        private static final CalendarSyncNotificationLoader INSTANCE = new CalendarSyncNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link CalendarSyncNotificationLoader}.
     * </p>
     *
     * @return {@link CalendarSyncNotificationLoader}.
     */
    public static CalendarSyncNotificationLoader getInstance() {
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
        return AppNotificationEventType.CALENDAR_SYNCED;
    }
}