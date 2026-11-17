package com.tlc.social.client.service;

import com.tlc.commons.json.JsonObject;

import com.tlc.social.client.service.internal.ChatServiceImpl;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.MessageTimestamps;

import com.tlc.social.common.model.RecipientProfile;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Map;

/**
 * Provides chat related services
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface ChatService {

    /**
     * <p>
     * Gets the instance of {@link ChatService}
     * </p>
     *
     * @return the instance of {@link ChatService}
     */
    static ChatService getInstance() {
        return ChatServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the messages form the provided chat id
     * </p>
     *
     * @param ids                   Message ids.
     * @param messagesFetchCallback Represents a callback to fetch messages
     */
    void fetchMessages(final Collection<Long> ids, final EventCallback<Map<Long, Message>> messagesFetchCallback);

    /**
     * <p>
     * Fetches sent, delivered, and read timestamps for the provided message ids.
     * </p>
     *
     * @param messageIds                    Represents the message ids
     * @param messageTimestampsFetchCallback Represents the callback to fetch message timestamps
     */
    void fetchMessageTimestamps(final Collection<Long> messageIds, final EventCallback<Map<Long, MessageTimestamps>> messageTimestampsFetchCallback);

    /**
     * <p>
     * Fetches the chats of the provided chat ids
     * </p>
     *
     * @param chatIds            Represents the collection of chat ids
     * @param chatsFetchCallback Represents the callback to fetch the chats
     */
    void fetchChats(final Collection<Long> chatIds, final EventCallback<Map<Long, Chat>> chatsFetchCallback);

    /**
     * <p>
     * Sends the message to the recipient
     * </p>
     *
     * @param messageJson         Represents the json which contains message details
     * @param messageSendCallback Represents the callback to send the message
     */
    void sendMessage(final JsonObject messageJson, final EventCallback<JsonObject> messageSendCallback);

    /**
     * <p>
     * Sends the read receipt
     * </p>
     *
     * @param readReceiptJson         Represents the read receipt json data
     * @param readReceiptSendCallback Represents the callback to send read receipt
     */
    void sendReadReceipt(final JsonObject readReceiptJson, final EventCallback<JsonObject> readReceiptSendCallback);

    /**
     * <p>
     * Fetches the {@link RecipientProfile} of the chat
     * </p>
     *
     * @param chatId            Represents the chat id
     * @param chatEventCallback Represents the callback to fetch the chat
     */
    void fetchPlatformAndRecipientProfile(final Long chatId, final EventCallback<Chat> chatEventCallback);

    /**
     * <p>
     * Fetches the {@link Channel} of the chat
     * </p>
     *
     * @param chatId               Represents the chat id
     * @param channelFetchCallback Represents the callback to fetch the channel
     */
    void fetchChannel(final Long chatId, final EventCallback<Channel> channelFetchCallback);
}
