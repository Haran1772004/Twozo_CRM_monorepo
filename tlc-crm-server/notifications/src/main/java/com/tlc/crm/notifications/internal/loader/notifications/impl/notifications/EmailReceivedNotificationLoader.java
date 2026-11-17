package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;

import java.util.Objects;

/**
 * <p>
 * Provides the service for the email received notification type.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class EmailReceivedNotificationLoader extends AbstractEmailNotificationLoader {

    private final ContactManager contactManager;

    private EmailReceivedNotificationLoader() {
        contactManager = ContactManager.getInstance();
    }

    private static class Instance {
        private static final EmailReceivedNotificationLoader INSTANCE = new EmailReceivedNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link EmailReceivedNotificationLoader}.
     * </p>
     *
     * @return {@link EmailReceivedNotificationLoader}.
     */
    public static EmailReceivedNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.EMAIL_RECEIVED;
    }

    /**
     * <p>
     * Processes the json with required data for building notification.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @param json  Represents the {@link JsonObject}.
     */
    protected void processEmailNotificationJson(final long orgId, final JsonObject json) {
        final String email = json.getString(NotificationAttributes.EMAIL);
        final Contact senderInfo = contactManager.getSenderInfo(orgId, email);

        if (Objects.nonNull(senderInfo)) {
            final JsonObject contactJson = Json.object();

            json.remove(NotificationAttributes.EMAIL);
            contactJson.put(NotificationAttributes.ID, senderInfo.id());
            contactJson.put(NotificationAttributes.NAME, senderInfo.getFullName());
            json.put(NotificationAttributes.CONTACT, contactJson);
        }
        json.remove(NotificationAttributes.USER_ID).remove(NotificationAttributes.IS_INITIAL_MESSAGE).remove(NotificationAttributes.SUBJECT);
    }
}