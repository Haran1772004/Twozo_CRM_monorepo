package com.tlc.social.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.converter.MessageTimestampsConverter;
import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.MessageTimestamps;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.Map;

/**
 * A handler which listens to the message timestamps retrieval event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = CommonSocialEvents.RETRIEVE_MESSAGE_TIMESTAMPS)
public class MessageTimestampsRetrievalHandler implements EventConsumer<byte[]> {

    private final MessageTimestampsConverter messageTimestampsConverter;
    private final ChatManager chatManager;

    public MessageTimestampsRetrievalHandler() {
        messageTimestampsConverter = MessageTimestampsConverter.getInstance();
        chatManager = ChatManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Represents the data to be consumed
     * @return the response as bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final JsonObject messageTimestampsJson = Json.object();
        final Collection<Long> messageIds = (Collection<Long>) data.get(MessageFields.MESSAGE_IDS);
        final Map<Long, MessageTimestamps> messageTimestamps = chatManager.getMessageTimestamps(messageIds);

        messageTimestamps.forEach((messageId, timestamps) ->
                messageTimestampsJson.put(String.valueOf(messageId), messageTimestampsConverter.convert(timestamps)));
        response.put(MessageFields.MESSAGE_TIMESTAMPS, messageTimestampsJson);

        return response.getBytes();
    }
}