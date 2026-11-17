package com.tlc.social.server.whatsapp.internal.webhook.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.whatsapp.internal.webhook.model.WADeliveryMessageEvent;

import java.time.Instant;

/**
 * A converter which converts the WhatsApp webhook delivery message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WADeliveryMessageEventConverter implements WebhookMessageEventConverter<WADeliveryMessageEvent> {

    private WADeliveryMessageEventConverter() {
    }

    /**
     * Holds the instance of {@link WADeliveryMessageEventConverter}
     */
    private static final class InstanceHolder {
        private static final WADeliveryMessageEventConverter DELIVERY_MESSAGE_EVENT_CONVERTER = new WADeliveryMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WADeliveryMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link WADeliveryMessageEventConverter}
     */
    public static WADeliveryMessageEventConverter getInstance() {
        return InstanceHolder.DELIVERY_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link WADeliveryMessageEvent}
     */
    @Override
    public WADeliveryMessageEvent convert(final JsonObject data) {
        final WADeliveryMessageEvent waDeliveryMessageEvent = new WADeliveryMessageEvent();
        final Long externalPhoneNumberId = data.getJsonObject(WAFields.METADATA).getLong(WAFields.PHONE_NUMBER_ID);

        waDeliveryMessageEvent.setReceiverId(externalPhoneNumberId);
        final JsonObject statusJson = data.getJsonArray(WAFields.STATUSES).getJsonObject(NumericalConstant.ZERO);
        final String messageId = statusJson.getString(WAFields.ID);
        final Long timestamp = statusJson.getLong(WAFields.TIMESTAMP);
        final Long recipientId = statusJson.getLong(WAFields.RECIPIENT_ID);

        waDeliveryMessageEvent.setMessageId(messageId);
        waDeliveryMessageEvent.setSenderId(recipientId);
        waDeliveryMessageEvent.setTimestamp(Instant.ofEpochSecond(timestamp).toEpochMilli());

        return waDeliveryMessageEvent;
    }
}