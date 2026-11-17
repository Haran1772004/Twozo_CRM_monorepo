package com.tlc.crm.messenger.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import com.tlc.social.common.model.Platform;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.sql.common.api.DataContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A consumer that consumes the WhatsApp webhook response payload and process it
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAWebhookResponseListener extends SocialWebhookResponseListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAWebhookResponseListener.class);

    private final WABusinessAccountManager waBusinessAccountManager;

    public WAWebhookResponseListener() {
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Whatsapp: Received webhook response. Data loading process initiated");
        final JsonArray entryResponseArray = Json.object(streamMessage.data()).getJsonArray(MessengerFields.ENTRIES);
        final int size = entryResponseArray.size();

        try {
            for (int entryIndex = 0; entryIndex < size; entryIndex++) {
                final JsonArray messageResponseArray = entryResponseArray.getJsonArray(entryIndex);
                final int responseSize = messageResponseArray.size();

                for (int index = 0; index < responseSize; index++) {
                    final JsonObject messageResponse = messageResponseArray.getJsonObject(index);
                    final String eventName = messageResponse.getNullableString(MessengerFields.EVENT);

                    if (messageResponse.containsKey(MessengerFields.ERRORS)) {
                        final List<String> errors = (List<String>) messageResponse.get(MessengerFields.ERRORS);

                        LoggerUtil.logError(LOGGER, ErrorCode.get(WAErrorCodes.WEBHOOK_PAYLOAD_VALIDATION_FAILED), "Whatsapp: Webhook data validation failed. Reason:{}", errors.get(NumericalConstant.ZERO));
                    } else if (Objects.equals(MessengerFields.TEXT_EVENT, eventName)) {
                        handleTextEventResponse(messageResponse);
                    } else if (Objects.equals(WAFields.PHONE_NUMBER_DELETION_EVENT, eventName)) {
                        handlePhoneNumberDeletionEvent(messageResponse);
                    }
                }
            }
            LOGGER.info("Whatsapp: Webhook response payload has been loaded successfully");
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to load webhook response payload: Reason:{}", throwable.getMessage());
        } finally {
            streamMessage.clean();
        }
    }

    /**
     * <p>
     *  Handles the response of the text event
     * </p>
     * @param messageResponse Represents the message response
     */
    private void handleTextEventResponse(final JsonObject messageResponse) {
        final Long chatId = messageResponse.getLong(MessengerFields.CHAT_ID);
        final Long phoneNumberId = messageResponse.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID);
        final Collection<CrmChat> chats = new ArrayList<>();
        final Collection<Long> associatedOrgIds = waBusinessAccountManager.getAssociatedOrgIds(phoneNumberId);

        if (!associatedOrgIds.isEmpty()) {
            final Map<Long, Long> orgCreatorMap = crmUserManager.getPrimaryUserMap(associatedOrgIds);
            final DataContainer targetContainer = DataContainer.create();
            final Map<Long, CrmChat> newChats = new HashMap<>();

            conversationManager.loadMessage(associatedOrgIds, chats, chatId, orgCreatorMap, false, messageResponse, targetContainer, newChats);
            waBusinessAccountManager.linkChatToPhoneNumber(associatedOrgIds, chats, phoneNumberId, targetContainer);

            if (!newChats.isEmpty()) {
                sendNotification(chatId, newChats, Platform.WHATSAPP);
            }
        }
    }

    /**
     * <p>
     * Handles the response of the phone number deletion event
     * </p>
     *
     * @param messageResponse Represents the message response
     */
    private void handlePhoneNumberDeletionEvent(final JsonObject messageResponse) {
        waBusinessAccountManager.deleteBusinessPhoneNumber(messageResponse.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID));
    }
}