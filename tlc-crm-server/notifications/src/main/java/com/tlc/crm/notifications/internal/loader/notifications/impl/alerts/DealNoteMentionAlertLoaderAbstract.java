package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

/**
 * <p>
 * Loads deal note mention alerts into DB.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public class DealNoteMentionAlertLoaderAbstract extends AbstractEntityNoteMentionAlertLoader {

    private DealNoteMentionAlertLoaderAbstract() {
    }

    private static class Instance {
        private static final DealNoteMentionAlertLoaderAbstract INSTANCE = new DealNoteMentionAlertLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link DealNoteMentionAlertLoaderAbstract}.
     * </p>
     *
     * @return {@link DealNoteMentionAlertLoaderAbstract}.
     */
    public static DealNoteMentionAlertLoaderAbstract getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AppNotificationEventType}
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.DEAL_NOTE_MENTIONED;
    }

    /**
     * {@inheritDoc}
     *
     * @return Represents the {@link NotificationAlert.Category }
     */
    @Override
    protected NotificationAlert.Category getCategory() {
        return NotificationAlert.Category.DEAL_NOTE_MENTIONED;
    }
}
