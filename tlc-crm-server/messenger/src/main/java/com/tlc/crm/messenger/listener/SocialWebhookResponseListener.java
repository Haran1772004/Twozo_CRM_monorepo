package com.tlc.crm.messenger.listener;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.conversation.manager.ConversationManager;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.notification.app.event.NotificationEvent;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.RecipientProfile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides abstract layer which consumes the webhook response
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class SocialWebhookResponseListener implements SecureUserTaskProcessor {

    private final EventDataHandler eventDataHandler;
    protected final CrmUserManager crmUserManager;
    protected final ConversationManager conversationManager;

    protected SocialWebhookResponseListener() {
        eventDataHandler = EventDataHandler.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        conversationManager = ConversationManager.getInstance();
    }

    /**
     * <p>
     * Sends the chat initiated notification
     * NOTE - OwnerId is set null for new chats because assigneeId column for alerts is nullable.
     * </p>
     *
     * @param chatId   Represents the chat id
     * @param newChats Represents the collection of new {@link CrmChat} grouped by org id
     */
    protected void sendNotification(final Long chatId, final Map<Long, CrmChat> newChats, final Platform platform) {
        for (final Map.Entry<Long, CrmChat> chatEntry : newChats.entrySet()) {
            final Long orgId = chatEntry.getKey();
            final CrmChat chat = chatEntry.getValue();
            final Long crmChatId = chat.getCrmChatId();

            if (chat.isNewChat()) {
                final Long userId = crmUserManager.getUserIds(orgId).stream().findFirst().orElseGet(() -> null);

                if (Objects.nonNull(userId)) {
                    secureProcess(orgId, userId, () -> {
                        final Message lastMessage = chat.getLastMessage();
                        final RecipientProfile recipientProfile = chat.getProfile();
                        final CrmChat crmChat = new CrmChat();
                        final EventData eventData = new EventData();

                        crmChat.setId(chatId);
                        crmChat.setOrgId(orgId);
                        crmChat.setNewChat(true);
                        crmChat.setOwnerId(null);
                        crmChat.setCrmChatId(crmChatId);
                        crmChat.setLastMessage(lastMessage);
                        crmChat.setProfile(recipientProfile);
                        crmChat.setPlatform(platform);

                        eventData.setValues(List.of(crmChat));
                        eventData.setAction(Action.CREATE);
                        eventData.setOrgId(orgId);
                        eventData.setUserId(userId);

                        eventDataHandler.handleEvent(NotificationEvent.APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
                    });
                }
            }
        }
    }
}