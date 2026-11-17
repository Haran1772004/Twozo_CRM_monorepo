package com.tlc.social.client.service.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.models.Attachment;
import com.tlc.social.client.service.ChatService;
import com.tlc.social.common.converter.ChatConverter;
import com.tlc.social.common.converter.MessageConverter;
import com.tlc.social.common.converter.MessageTimestampsConverter;
import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.MessageTimestamps;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.common.model.Platform;

import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the implementation of {@link ChatService} which provides the chat related services
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ChatServiceImpl implements ChatService {

    final MessageTimestampsConverter messageTimestampsConverter;

    private ChatServiceImpl() {
        messageTimestampsConverter = MessageTimestampsConverter.getInstance();
    }

    private static final class InstanceHolder {
        private static final ChatServiceImpl CHAT_SERVICE = new ChatServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link ChatServiceImpl}
     * </p>
     *
     * @return the instance of {@link ChatServiceImpl}
     */
    public static ChatServiceImpl getInstance() {
        return InstanceHolder.CHAT_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param ids                   Message ids.
     * @param messagesFetchCallback Represents the callback to fetch the messages
     */
    @Override
    public void fetchMessages(final Collection<Long> ids, final EventCallback<Map<Long, Message>> messagesFetchCallback) {
        final Event<byte[]> messagesFetchEvent = new Event<>(CommonSocialEvents.RETRIEVE_MESSAGES, Json.object().put(MessageFields.IDS, ids).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final JsonObject data = Json.object(bytes);
                    final Map<Long, Message> messages = new HashMap<>();
                    final JsonArray messageArray = data.getJsonArray(MessageFields.MESSAGES);
                    final int messageSize = messageArray.size();
                    final MessageConverter messageConverter = MessageConverter.getInstance();

                    for (int index = 0; index < messageSize; index++) {
                        final Message message = messageConverter.convert(messageArray.getJsonObject(index));

                        messages.put(message.id(), message);
                    }
                    messagesFetchCallback.onSuccess(messages);
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                messagesFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(messagesFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param messageIds                    Represents the message ids
     * @param messageTimestampsFetchCallback Represents the callback to fetch message timestamps
     */
    @Override
    public void fetchMessageTimestamps(final Collection<Long> messageIds, final EventCallback<Map<Long, MessageTimestamps>> messageTimestampsFetchCallback) {
        final Event<byte[]> messageTimestampsFetchEvent = new Event<>(CommonSocialEvents.RETRIEVE_MESSAGE_TIMESTAMPS,
                Json.object().put(MessageFields.MESSAGE_IDS, messageIds).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final JsonObject data = Json.object(bytes);
                    final JsonObject messageTimestampsJson = data.getJsonObject(MessageFields.MESSAGE_TIMESTAMPS);
                    final Map<Long, MessageTimestamps> messageTimestampsMap = new HashMap<>(messageTimestampsJson.size());

                    messageTimestampsJson.forEach((key, value) ->
                            messageTimestampsMap.put(Long.valueOf(key), messageTimestampsConverter.convert(Json.object((Map<String, Long>) value))));
                    messageTimestampsFetchCallback.onSuccess(messageTimestampsMap);
                } catch (final Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                messageTimestampsFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(messageTimestampsFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param chatIds            Represents the collection of chat ids
     * @param chatsFetchCallback Represents the callback to fetch the chats
     */
    @Override
    public void fetchChats(final Collection<Long> chatIds, final EventCallback<Map<Long, Chat>> chatsFetchCallback) {
        fetchChats(new Event<>(CommonSocialEvents.RETRIEVE_CHATS, Json.object().put(ChatFields.CHAT_IDS, chatIds).getBytes()), chatsFetchCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param messageJson         Represents the json which contains message details
     * @param messageSendCallback Represents the callback to send the message
     */
    @Override
    public void sendMessage(final JsonObject messageJson, final EventCallback<JsonObject> messageSendCallback) {
        final Event<byte[]> messageSendEvent = new Event<>(CommonSocialEvents.SEND_OUTBOUND_MESSAGE, messageJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                messageSendCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                messageSendCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(messageSendEvent, eventCallback);
    }

    /**
     * <p>
     * Fetches the chats
     * </p>
     *
     * @param chatsFetchEvent    {@link Event}.
     * @param chatsFetchCallback {@link EventCallback}.
     */
    private void fetchChats(final Event<byte[]> chatsFetchEvent, final EventCallback<Map<Long, Chat>> chatsFetchCallback) {
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final JsonObject data = Json.object(bytes);
                    final ChatConverter chatConverter = ChatConverter.getInstance();
                    final Map<Long, Chat> chatMap = new LinkedHashMap<>();
                    final JsonArray chatsArray = data.getJsonArray(ChatFields.CHATS);
                    final int chatSize = chatsArray.size();

                    for (int index = 0; index < chatSize; index++) {
                        final Chat chat = chatConverter.convert(chatsArray.getJsonObject(index));

                        chatMap.put(chat.id(), chat);
                    }
                    chatsFetchCallback.onSuccess(chatMap);
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                chatsFetchCallback.onFailure(throwable);
            }
        };
        EventNotifier.get().send(chatsFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param readReceiptJson         Represents the read receipt json data
     * @param readReceiptSendCallback Represents the callback to send read receipt
     */
    @Override
    public void sendReadReceipt(final JsonObject readReceiptJson, EventCallback<JsonObject> readReceiptSendCallback) {
        final Event<byte[]> readReceiptSendEvent = new Event<>(CommonSocialEvents.SEND_READ_RECEIPT, readReceiptJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                readReceiptSendCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                readReceiptSendCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(readReceiptSendEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param chatId            Represents the chat id
     * @param chatEventCallback Represents the callback to fetch the chat
     */
    @Override
    public void fetchPlatformAndRecipientProfile(final Long chatId, final EventCallback<Chat> chatEventCallback) {
        final Event<byte[]> readReceiptSendEvent = new Event<>(CommonSocialEvents.RETRIEVE_PLATFORM_AND_RECIPIENT_PROFILE, Json.object().put(ChatFields.CHAT_ID, chatId).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final JsonObject chatJson = Json.object(bytes);
                    final RecipientProfile recipientProfile = new RecipientProfile();
                    final Chat chat = new Chat();

                    recipientProfile.setName(chatJson.getString(ChatFields.RECIPIENT_NAME));

                    if (chatJson.containsKey(ChatFields.PLATFORM)) {
                        chat.setPlatform(Platform.get(chatJson.getShort(ChatFields.PLATFORM)));
                    }

                    if (chatJson.containsKey(ChatFields.PROFILE_PICTURE_ID)) {
                        final Attachment attachment = new Attachment();

                        attachment.setId(chatJson.getLong(ChatFields.PROFILE_PICTURE_ID));
                        recipientProfile.setProfilePicture(attachment);
                    }
                    chat.setProfile(recipientProfile);
                    chatEventCallback.onSuccess(chat);
                } catch (Throwable throwable) {
                    chatEventCallback.onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                chatEventCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(readReceiptSendEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param chatId               Represents the chat id
     * @param channelFetchCallback Represents the callback to fetch the channel
     */
    @Override
    public void fetchChannel(final Long chatId, final EventCallback<Channel> channelFetchCallback) {
        final Event<byte[]> channelFetchEvent = new Event<>(CommonSocialEvents.RETRIEVE_CHANNEL, Json.object().put(ChatFields.CHAT_ID, chatId).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final JsonObject channelJson = Json.object(bytes);
                    final Channel channel = new Channel();

                    channel.setId(channelJson.getLong(ChatFields.CHANNEL_ID));
                    channel.setName(channelJson.getString(ChatFields.CHANNEL_NAME));
                    channel.setType(Channel.ChannelType.get(channelJson.getShort(ChatFields.CHANNEL_TYPE)));

                    channelFetchCallback.onSuccess(channel);
                } catch (Throwable throwable) {
                    channelFetchCallback.onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                channelFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(channelFetchEvent, eventCallback);
    }
}