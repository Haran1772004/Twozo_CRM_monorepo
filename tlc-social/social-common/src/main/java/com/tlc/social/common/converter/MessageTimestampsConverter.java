package com.tlc.social.common.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.MessageTimestamps;

/**
 * Converts {@link MessageTimestamps} to and from JSON.
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MessageTimestampsConverter {

    private MessageTimestampsConverter() {
    }

    private static final class InstanceHolder {
        private static final MessageTimestampsConverter INSTANCE = new MessageTimestampsConverter();
    }

    public static MessageTimestampsConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Converts JSON into {@link MessageTimestamps}.
     *
     * @param jsonObject Represents the json data
     * @return the message timestamps
     */
    public MessageTimestamps convert(final JsonObject jsonObject) {
        final Long sendTime = jsonObject.containsKey(MessageFields.SENT_TIME) ? jsonObject.getLong(MessageFields.SENT_TIME) : null;
        final Long deliveredTime = jsonObject.containsKey(MessageFields.DELIVERED_TIME) ? jsonObject.getLong(MessageFields.DELIVERED_TIME) : null;
        final Long readTime = jsonObject.containsKey(MessageFields.READ_TIME) ? jsonObject.getLong(MessageFields.READ_TIME) : null;

        return new MessageTimestamps(sendTime, deliveredTime, readTime);
    }

    /**
     * Converts {@link MessageTimestamps} into JSON.
     *
     * @param messageTimestamps Represents the message timestamps
     * @return the json data
     */
    public JsonObject convert(final MessageTimestamps messageTimestamps) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(MessageFields.SENT_TIME, messageTimestamps.sentTime());
        jsonObject.put(MessageFields.DELIVERED_TIME, messageTimestamps.deliveredTime());
        jsonObject.put(MessageFields.READ_TIME, messageTimestamps.readTime());

        return jsonObject;
    }
}