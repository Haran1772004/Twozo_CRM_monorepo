package com.tlc.social.server.internal.webhook.event.converter.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.model.PageDeliveryMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.PageTextMessageEvent;

import java.util.Collection;

/**
 * A converter which converts the {@link PageDeliveryMessageEvent}.
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class PageDeliveryMessageEventConverter <T extends PageDeliveryMessageEvent> implements WebhookMessageEventConverter<T> {

    protected PageDeliveryMessageEventConverter() {
    }

    /**
     * <p>
     * Init text message event.
     * </p>
     *
     * @return {@link PageTextMessageEvent}.
     */
    protected abstract T init();

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link PageDeliveryMessageEvent}
     */
    @Override
    public T convert(final JsonObject data) {
        final T deliveryMessageEvent = init();
        final JsonObject deliveryJson = data.getJsonObject(FBFields.DELIVERY);

        deliveryMessageEvent.setSenderId(data.getJsonObject(FBFields.SENDER).getLong(FBFields.ID));
        deliveryMessageEvent.setReceiverId(data.getJsonObject(FBFields.RECIPIENT).getLong(FBFields.ID));
        deliveryMessageEvent.setTimestamp(data.getLong(FBFields.TIMESTAMP));
        deliveryMessageEvent.setMessageIds((Collection<String>) deliveryJson.get(FBFields.MIDS));
        deliveryMessageEvent.setWatermark(deliveryJson.getLong(FBFields.WATERMARK));

        return deliveryMessageEvent;
    }
}
