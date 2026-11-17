package com.tlc.social.server.whatsapp.internal.webhook.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;

import java.util.Objects;

/**
 * A converter which converts the WhatsApp webhook message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAWebhookMessageEventConverter implements WebhookMessageEventConverter<WebhookMessageEvent> {

    private final WATextMessageEventConverter textMessageEventConverter;
    private final WADeliveryMessageEventConverter deliveryMessageEventConverter;
    private final WAReadMessageEventConverter readMessageEventConverter;
    private final WAFailedMessageEventConverter failedMessageEventConverter;

    private WAWebhookMessageEventConverter() {
        textMessageEventConverter = WATextMessageEventConverter.getInstance();
        deliveryMessageEventConverter = WADeliveryMessageEventConverter.getInstance();
        readMessageEventConverter = WAReadMessageEventConverter.getInstance();
        failedMessageEventConverter = WAFailedMessageEventConverter.getInstance();
    }

    /**
     * Holds the instance of {@link WAWebhookMessageEventConverter}
     */
    private static final class InstanceHolder {
        private static final WAWebhookMessageEventConverter WA_WEBHOOK_MESSAGE_EVENT_CONVERTER = new WAWebhookMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WAWebhookMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link WAWebhookMessageEventConverter}
     */
    public static WAWebhookMessageEventConverter getInstance() {
        return InstanceHolder.WA_WEBHOOK_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link WebhookMessageEvent}
     */
    @Override
    public WebhookMessageEvent convert(final JsonObject data) {

        if (data.containsKey(WAFields.MESSAGES)) {
            return textMessageEventConverter.convert(data);
        } else if (data.containsKey(WAFields.STATUSES)) {
            final JsonObject statusJson = data.getJsonArray(WAFields.STATUSES).getJsonObject(NumericalConstant.ZERO);
            final String status = statusJson.getString(WAFields.STATUS);

            if (Objects.equals(WAFields.DELIVERED, status)) {
                return deliveryMessageEventConverter.convert(data);
            } else if (Objects.equals(WAFields.READ, status)) {
                return readMessageEventConverter.convert(data);
            } else if (Objects.equals(WAFields.FAILED, status)) {
                return failedMessageEventConverter.convert(data);
            }
        }

        return null;
    }
}
