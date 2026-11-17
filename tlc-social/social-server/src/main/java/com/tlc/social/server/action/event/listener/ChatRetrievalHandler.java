package com.tlc.social.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.converter.ChatConverter;
import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Chat;
import com.tlc.social.server.manager.ChatManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;

/**
 * A handler which listens to the chat retrieval event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = CommonSocialEvents.RETRIEVE_CHATS)
public class ChatRetrievalHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response in byte array
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final JsonArray chatArray = Json.array();
        final Collection<Long> chatIds = (Collection<Long>) data.get(ChatFields.CHAT_IDS);
        final ChatManager chatManager = ChatManager.getInstance();
        final ChatConverter chatConverter = ChatConverter.getInstance();

        if (!chatManager.exists(chatIds)) {
            throw ErrorCode.get(SocialErrorCodes.CHAT_NOT_FOUND, "i18n.social.chat.not.found");
        }

        for (final Chat chat : chatManager.getAllChats(chatIds)) {
            final JsonObject chatJson = chatConverter.convert(chat);

            chatArray.put(chatJson);
        }
        response.put(ChatFields.CHATS, chatArray);

        return response.getBytes();
    }
}
