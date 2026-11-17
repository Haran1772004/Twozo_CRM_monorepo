package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonArray;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.model.ChatNotificationSettings;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Handles the notification of the chat initiation type
 *
 * @author Maharaja S
 * @version 1.0
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ChatInitiatedAlertLoader extends AbstractAppNotificationAlertLoader<CrmChat> {

    private final CrmUserManager crmUserManager;

    private ChatInitiatedAlertLoader() {
        crmUserManager = CrmUserManager.getInstance();
    }

    private static class Instance {
        private static final ChatInitiatedAlertLoader INSTANCE = new ChatInitiatedAlertLoader();
    }

    /**
     * <p>
     * Gets {@link ChatInitiatedAlertLoader}.
     * </p>
     *
     * @return {@link ChatInitiatedAlertLoader}.
     */
    public static ChatInitiatedAlertLoader getInstance() {
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
        return AppNotificationEventType.CHAT_INITIATED;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param crmChats Represents the {@link CrmChat}.
     */
    @Override
    public void load(final long orgId, final Collection<CrmChat> crmChats) {
        final JsonArray chatArray = appNotificationConverter.convertChatsToJson(crmChats).
                getJsonArray(NotificationAttributes.CHATS);
        final int size = chatArray.size();
        final Collection<Long> userIds = crmUserManager.getUserIds(orgId);
        final Collection<Long> notifiableUsers = new ArrayList<>();

        for (final Long userId : userIds) {
            final Optional<ChatNotificationSettings> optionalChatNotificationSettings =
                    notificationSettingsManager.retrieveChatNotificationSettings(orgId, userId);

            if (optionalChatNotificationSettings.isPresent() && optionalChatNotificationSettings.get().isChatInitiated()) {
                notifiableUsers.add(userId);
            }
        }

        for (int index = NumericalConstant.ZERO; index < size; index++) {
            notificationAlertManager.storeChatAlert(notificationAlertConverter.convertChatAlert(
                    orgId, notifiableUsers, NotificationAlert.Category.CHAT_INITIATED, chatArray.getJsonObject(index)));
        }
    }
}
