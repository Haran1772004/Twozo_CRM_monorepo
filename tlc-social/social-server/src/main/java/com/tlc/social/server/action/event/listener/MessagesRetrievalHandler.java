package com.tlc.social.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.converter.MessageConverter;
import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.Message;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;

/**
 * A handler which listens to the messages retrieval event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = CommonSocialEvents.RETRIEVE_MESSAGES)
public class MessagesRetrievalHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param bytes Represents the data to be consumed
     * @return the bytes array
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final JsonArray messageArray = Json.array();
        final Collection<Long> ids = (Collection<Long>) data.get(MessageFields.IDS);
        final MessageConverter messageConverter = MessageConverter.getInstance();
        final Collection<Message> messages = ChatManager.getInstance().getMessages(ids);

        for (final Message message : messages) {
            final JsonObject messageJson = messageConverter.convert(message);

            messageArray.put(messageJson);
        }
        response.put(MessageFields.MESSAGES, messageArray);

        return response.getBytes();
    }
}