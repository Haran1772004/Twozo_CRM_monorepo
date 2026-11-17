package com.tlc.crm.messenger.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.messenger.api.channel.manager.FBPageManager;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
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
 * A consumer that consumes the fb webhook response payload and process it
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBWebhookResponseListener extends SocialWebhookResponseListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBWebhookResponseListener.class);

    private final FBPageManager fbChannelManager;

    public FBWebhookResponseListener() {
        this.fbChannelManager = FBPageManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Facebook: Received webhook response. Data loading process initiated");
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

                        LoggerUtil.logError(LOGGER, ErrorCode.get(CrmSocialErrorCodes.WEBHOOK_PAYLOAD_VALIDATION_FAILED), "Facebook: Webhook data validation failed. Reason:{}", errors.get(NumericalConstant.ZERO));
                    } else if (Objects.equals(MessengerFields.TEXT_EVENT, messageResponse.getNullableString(MessengerFields.EVENT))) {
                        final Long chatId = messageResponse.getLong(MessengerFields.CHAT_ID);
                        final boolean isMessageEcho = messageResponse.getBoolean(MessengerFields.IS_MESSAGE_ECHO);
                        final Long pageId = messageResponse.getLong(MessengerFields.PAGE_ID);
                        final Collection<CrmChat> chats = new ArrayList<>();
                        final Collection<Long> associatedOrgIds = fbChannelManager.getAssociatedOrgIds(pageId);
                        final Map<Long, Long> orgCreatorMap = isMessageEcho ? crmUserManager.getPrimaryUserMap(associatedOrgIds) : Collections.emptyMap();
                        final DataContainer targetContainer = DataContainer.create();
                        final Map<Long, CrmChat> newChats = new HashMap<>();

                        conversationManager.loadMessage(associatedOrgIds, chats, chatId, orgCreatorMap, isMessageEcho, messageResponse, targetContainer, newChats);
                        fbChannelManager.linkChatToPage(associatedOrgIds, chats, pageId, targetContainer);

                        if (!isMessageEcho) {
                            sendNotification(chatId, newChats, Platform.FACEBOOK);
                        }
                    }
                }
            }
            LOGGER.info("Facebook: Webhook response payload has been loaded successfully");
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to load webhook response payload: Reason:{}", throwable.getMessage());
        } finally {
            streamMessage.clean();
        }
    }
}