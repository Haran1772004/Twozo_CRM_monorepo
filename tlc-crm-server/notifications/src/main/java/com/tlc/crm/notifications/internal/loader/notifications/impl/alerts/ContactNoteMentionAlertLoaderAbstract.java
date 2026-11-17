package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

/**
 * <p>
 * Loads contact note mention alerts into DB.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public class ContactNoteMentionAlertLoaderAbstract extends AbstractEntityNoteMentionAlertLoader {

    private ContactNoteMentionAlertLoaderAbstract() {
    }

    private static class Instance {
        private static final ContactNoteMentionAlertLoaderAbstract INSTANCE = new ContactNoteMentionAlertLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link ContactNoteMentionAlertLoaderAbstract}.
     * </p>
     *
     * @return {@link ContactNoteMentionAlertLoaderAbstract}.
     */
    public static ContactNoteMentionAlertLoaderAbstract getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AppNotificationEventType}
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.CONTACT_NOTE_MENTIONED;
    }

    /**
     * {@inheritDoc}
     *
     * @return Represents the {@link NotificationAlert.Category }
     */
    @Override
    protected NotificationAlert.Category getCategory() {
        return NotificationAlert.Category.CONTACT_NOTE_MENTIONED;
    }
}