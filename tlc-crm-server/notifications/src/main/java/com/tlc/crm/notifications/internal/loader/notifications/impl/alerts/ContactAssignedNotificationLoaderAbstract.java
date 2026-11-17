package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.Collection;

/**
 * <p>
 * Loads contact assigned notification into DB.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ContactAssignedNotificationLoaderAbstract extends AbstractEntityAssignedNotificationLoader<Contact> {

    private ContactAssignedNotificationLoaderAbstract() {
    }

    private static class Instance {
        private static final ContactAssignedNotificationLoaderAbstract INSTANCE = new ContactAssignedNotificationLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link ContactAssignedNotificationLoaderAbstract}.
     * </p>
     *
     * @return {@link ContactAssignedNotificationLoaderAbstract}.
     */
    public static ContactAssignedNotificationLoaderAbstract getInstance() {
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
        return AppNotificationEventType.CONTACT_ASSIGNED;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param contacts {@link Contact} collection.
     */
    @Override
    protected JsonObject buildEntityAssignedJson(final Long orgId, final Collection<Contact> contacts) {
        return appNotificationConverter.convertContactsToJson(contacts);
    }

    /**
     * <p>
     * Gets {@link NotificationAlert.Category}.
     * </p>
     *
     * @return {@link NotificationAlert.Category}.
     */
    @Override
    protected NotificationAlert.Category getCategory() {
        return NotificationAlert.Category.CONTACT_ASSIGNED;
    }
}