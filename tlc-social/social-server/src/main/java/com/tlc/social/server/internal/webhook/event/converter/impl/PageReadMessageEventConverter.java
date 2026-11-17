package com.tlc.social.server.internal.webhook.event.converter.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.model.PageReadMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.PageTextMessageEvent;

/**
 * A converter which converts the {@link PageReadMessageEvent}.
 *
 * @author Maharaja S.
 * @version 1.0.
 */
public abstract class PageReadMessageEventConverter <T extends PageReadMessageEvent> implements WebhookMessageEventConverter<T> {

    protected PageReadMessageEventConverter() {
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
     * @param data Represents the data to be converted.
     * @return the {@link PageReadMessageEvent}.
     */
    @Override
    public T convert(final JsonObject data) {
        final T readMessageEvent = init();

        readMessageEvent.setSenderId(data.getJsonObject(FBFields.SENDER).getLong(FBFields.ID));
        readMessageEvent.setReceiverId(data.getJsonObject(FBFields.RECIPIENT).getLong(FBFields.ID));
        readMessageEvent.setTimestamp(data.getLong(FBFields.TIMESTAMP));
        readMessageEvent.setWatermark(data.getJsonObject(FBFields.READ).getLong(FBFields.WATERMARK));

        return readMessageEvent;
    }
}