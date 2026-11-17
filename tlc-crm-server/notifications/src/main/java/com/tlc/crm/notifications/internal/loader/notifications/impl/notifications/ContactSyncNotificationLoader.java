package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.notification.app.AppNotificationEventType;
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
public final class ContactSyncNotificationLoader extends AbstractSyncNotificationLoader<ContactSyncSettings> {

    private ContactSyncNotificationLoader() {
    }

    private static class Instance {
        private static final ContactSyncNotificationLoader INSTANCE = new ContactSyncNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link ContactSyncNotificationLoader}.
     * </p>
     *
     * @return {@link ContactSyncNotificationLoader}.
     */
    public static ContactSyncNotificationLoader getInstance() {
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
        return AppNotificationEventType.CONTACT_SYNCED;
    }
}
