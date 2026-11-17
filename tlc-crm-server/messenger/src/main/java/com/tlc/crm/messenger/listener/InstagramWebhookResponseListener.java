package com.tlc.crm.messenger.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.messenger.api.channel.manager.InstagramPageManager;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.conversation.manager.ConversationManager;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.CrmSocialErrorCodes;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.social.common.model.Platform;
import com.tlc.sql.common.api.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Listens to instagram's webhook response event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramWebhookResponseListener extends SocialWebhookResponseListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramWebhookResponseListener.class);

    private final InstagramPageManager instagramPageManager;
    private final ConversationManager conversationManager;
    private final CrmUserManager crmUserManager;

    public InstagramWebhookResponseListener() {
        this.instagramPageManager = InstagramPageManager.getInstance();
        this.conversationManager = ConversationManager.getInstance();
        this.crmUserManager = CrmUserManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Instagram: Received webhook response. Data loading process initiated");
        final JsonArray entryResponseArray = Json.object(streamMessage.data()).getJsonArray(MessengerFields.ENTRIES);
        final int size = entryResponseArray.size();

        try {
            for (int entryIndex = 0; entryIndex < size; entryIndex++) {
                final JsonArray messageResponseArray = entryResponseArray.getJsonArray(entryIndex);
                final int responseSize = messageResponseArray.size();

                for (int index = 0; index < responseSize; index++) {
                    final JsonObject messageResponse = messageResponseArray.getJsonObject(index);

                    if (messageResponse.containsKey(MessengerFields.ERRORS)) {
                        final List<String> errors = (List<String>) messageResponse.get(MessengerFields.ERRORS);

                        LoggerUtil.logError(LOGGER, ErrorCode.get(CrmSocialErrorCodes.WEBHOOK_PAYLOAD_VALIDATION_FAILED), "Instagram: Webhook data validation failed. Reason:{}", errors.get(NumericalConstant.ZERO));
                    } else if (Objects.equals(MessengerFields.TEXT_EVENT, messageResponse.getNullableString(MessengerFields.EVENT))) {
                        final Long chatId = messageResponse.getLong(MessengerFields.CHAT_ID);
                        final boolean isMessageEcho = messageResponse.getBoolean(MessengerFields.IS_MESSAGE_ECHO);
                        final Long pageId = messageResponse.getLong(MessengerFields.PAGE_ID);
                        final Collection<CrmChat> chats = new ArrayList<>();
                        final Collection<Long> associatedOrgIds = instagramPageManager.getAssociatedOrgIds(pageId);
                        final Map<Long, Long> orgCreatorMap = isMessageEcho ? crmUserManager.getPrimaryUserMap(associatedOrgIds) : Collections.emptyMap();
                        final DataContainer targetContainer = DataContainer.create();
                        final Map<Long, CrmChat> newChats = new HashMap<>();

                        conversationManager.loadMessage(associatedOrgIds, chats, chatId, orgCreatorMap, isMessageEcho, messageResponse, targetContainer, newChats);
                        instagramPageManager.linkChatToPage(associatedOrgIds, chats, pageId, targetContainer);

                        if (!isMessageEcho) {
                            sendNotification(chatId, newChats, Platform.INSTAGRAM);
                        }
                    }
                }
            }
            LOGGER.info("Instagram: Webhook response payload has been loaded successfully");
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Instagram: Failed to load webhook response payload: Reason:{}", throwable.getMessage());
        } finally {
            streamMessage.clean();
        }
    }
}