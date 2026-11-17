package com.tlc.social.common.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.RecipientProfile;

/**
 * A converter which provides various ways to convert the data from / into {@link Chat}.
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ChatConverter implements SocialEntityConverter<Chat> {

    protected ChatConverter() {
    }

    private static final class InstanceHolder {
        private static final ChatConverter CHAT_CONVERTER = new ChatConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link ChatConverter}
     * </p>
     *
     * @return the instance of {@link ChatConverter}
     */
    public static ChatConverter getInstance() {
        return InstanceHolder.CHAT_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Represents the json data to be converted
     * @return the {@link Chat}
     */
    @Override
    public Chat convert(final JsonObject jsonObject) {
        final Chat chat = new Chat();
        final Message lastMessage = new Message();
        final RecipientProfile recipientProfile = new RecipientProfile();
        final Channel channel = new Channel();

        recipientProfile.setName(jsonObject.getString(ChatFields.RECIPIENT_NAME));
        channel.setName(jsonObject.getString(ChatFields.CHANNEL_NAME));
        lastMessage.setSentTime(jsonObject.getLong(ChatFields.LAST_MESSAGE_TIME));

        chat.setId(jsonObject.getLong(ChatFields.ID));
        chat.setPlatform(Platform.get(jsonObject.getShort(ChatFields.PLATFORM)));
        chat.setUnreadCount(jsonObject.getInt(ChatFields.UNREAD_COUNT));
        chat.setLastMessage(lastMessage);
        chat.setProfile(recipientProfile);
        chat.setChannel(channel);

        return chat;
    }

    /**
     * {@inheritDoc}
     *
     * @param chat Represents the {@link Chat}
     * @return the converted json data
     */
    @Override
    public JsonObject convert(final Chat chat) {
        final JsonObject chatJson = Json.object();

        chatJson.put(ChatFields.ID, chat.id());
        chatJson.put(ChatFields.PLATFORM, chat.getPlatform().getId());
        chatJson.put(ChatFields.LAST_MESSAGE_TIME, chat.getLastMessageTime());
        chatJson.put(ChatFields.RECIPIENT_NAME, chat.getProfile().getName());
        chatJson.put(ChatFields.CHANNEL_NAME, chat.getChannel().getName());
        chatJson.put(ChatFields.UNREAD_COUNT, chat.getUnreadCount());

        return chatJson;
    }
}
