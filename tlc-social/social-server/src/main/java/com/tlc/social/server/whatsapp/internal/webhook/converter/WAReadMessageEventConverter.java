package com.tlc.social.server.whatsapp.internal.webhook.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAReadMessageEvent;

import java.time.Instant;

/**
 * A converter which converts the WhatsApp webhook read message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAReadMessageEventConverter implements WebhookMessageEventConverter<WAReadMessageEvent> {

    private WAReadMessageEventConverter() {
    }

    /**
     * Holds the instance of {@link WAReadMessageEventConverter}
     */
    private static final class InstanceHolder {
        private static final WAReadMessageEventConverter READ_MESSAGE_EVENT_CONVERTER = new WAReadMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WAReadMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link WAReadMessageEventConverter}
     */
    public static WAReadMessageEventConverter getInstance() {
        return InstanceHolder.READ_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link WAReadMessageEvent}
     */
    @Override
    public WAReadMessageEvent convert(final JsonObject data) {
        final WAReadMessageEvent waReadMessageEvent = new WAReadMessageEvent();
        final Long externalPhoneNumberId = data.getJsonObject(WAFields.METADATA).getLong(WAFields.PHONE_NUMBER_ID);

        waReadMessageEvent.setReceiverId(externalPhoneNumberId);
        final JsonObject statusJson = data.getJsonArray(WAFields.STATUSES).getJsonObject(NumericalConstant.ZERO);
        final String messageId = statusJson.getString(WAFields.ID);
        final Long timestamp = statusJson.getLong(WAFields.TIMESTAMP);
        final Long recipientId = statusJson.getLong(WAFields.RECIPIENT_ID);

        waReadMessageEvent.setMessageId(messageId);
        waReadMessageEvent.setSenderId(recipientId);
        waReadMessageEvent.setTimestamp(Instant.ofEpochSecond(timestamp).toEpochMilli());

        return waReadMessageEvent;
    }
}