package com.tlc.social.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Channel;
import com.tlc.social.server.manager.ChatManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Optional;

/**
 * A listener which listens to the channel fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = CommonSocialEvents.RETRIEVE_CHANNEL)
public class ChannelFetchHandler implements EventConsumer<byte[]> {

    private final ChatManager chatManager;

    public ChannelFetchHandler() {
        chatManager = ChatManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response as bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final Long chatId = data.getLong(ChatFields.CHAT_ID);
        final Optional<Channel> optionalChannel = chatManager.getChannel(chatId);

        if (optionalChannel.isPresent()) {
            final Channel channel = optionalChannel.get();

            response.put(ChatFields.CHANNEL_ID, channel.id());
            response.put(ChatFields.CHANNEL_NAME, channel.getName());
            response.put(ChatFields.CHANNEL_TYPE, channel.getType().getId());
        } else {
            throw ErrorCode.get(SocialErrorCodes.CHANNEL_NOT_FOUND, "i18n.social.channel.not.found");
        }

        return response.getBytes();
    }
}