package com.tlc.crm.notifications.internal.handler.impl;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notifications.internal.handler.registry.NotifiableModelHandlerRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Handles {@link CrmChat} for notification/alert purposes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotifiableModelHandlerRegistry.class)
public final class NotifiableChatHandler extends AbstractNotifiableModelHandler<CrmChat> {

    private NotifiableChatHandler() {
    }

    private static class Instance {
        private static final NotifiableChatHandler INSTANCE = new NotifiableChatHandler();
    }

    /**
     * <p>
     * Gets {@link NotifiableChatHandler}.
     * </p>
     *
     * @return {@link NotifiableChatHandler}.
     */
    public static NotifiableChatHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * @param orgId  Represents an organization id.
     * @param crmChats Collection {@link CrmChat}.
     */
    @Override
    public void handle(final Long orgId, final Collection<CrmChat> crmChats) {
        final Collection<AppNotificationEventType> notificationEventTypes = new ArrayList<>(Arrays.asList(
                CrmChat.class.getAnnotation(Notifiable.class).types()));
        final Optional<CrmChat> optionalCrmChat = crmChats.stream().findFirst();

        if (optionalCrmChat.isPresent()) {
            final CrmChat chat = optionalCrmChat.get();

            notificationEventTypes.remove(chat.isNewChat() ? AppNotificationEventType.CHAT_ASSIGNED :
                    AppNotificationEventType.CHAT_INITIATED);
            load(orgId, crmChats, notificationEventTypes);
        }
    }

    /**
     * <p>
     * Gets {@link CrmModelType}.
     * </p>
     *
     * @return {@link CrmModelType}.
     */
    @Override
    public CrmModelType getType() {
        return CrmModelType.CHAT;
    }
}