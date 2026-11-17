package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;

/**
 * <p>
 * Provides the service for the email replied notification type.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class EmailRepliedNotificationLoader extends AbstractEmailNotificationLoader {

    private EmailRepliedNotificationLoader() {
    }

    private static class Instance {
        private static final EmailRepliedNotificationLoader INSTANCE = new EmailRepliedNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link EmailRepliedNotificationLoader}.
     * </p>
     *
     * @return {@link EmailRepliedNotificationLoader}.
     */
    public static EmailRepliedNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.EMAIL_REPLIED;
    }

    /**
     * <p>
     * Processes the json with required data for building notification.
     * </p>
     *
     * @param emailJson Represents the {@link JsonObject}.
     */
    protected void processEmailNotificationJson(long orgId, JsonObject emailJson) {
        emailJson.remove(NotificationAttributes.USER_ID).remove(NotificationAttributes.IS_INITIAL_MESSAGE).remove(NotificationAttributes.EMAIL);
    }
}