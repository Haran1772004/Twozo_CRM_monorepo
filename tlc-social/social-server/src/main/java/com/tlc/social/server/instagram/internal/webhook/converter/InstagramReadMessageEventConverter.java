package com.tlc.social.server.instagram.internal.webhook.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.server.instagram.internal.webhook.model.InstagramReadMessageEvent;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageReadMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.model.PageReadMessageEvent;

/**
 * <p>
 * Converts Instagram's read message event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramReadMessageEventConverter extends PageReadMessageEventConverter<InstagramReadMessageEvent> {

    private InstagramReadMessageEventConverter() {
    }

    private static final class Instance {
        private static final InstagramReadMessageEventConverter TEXT_MESSAGE_EVENT_CONVERTER = new InstagramReadMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramReadMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link InstagramReadMessageEventConverter}
     */
    public static InstagramReadMessageEventConverter getInstance() {
        return Instance.TEXT_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * <p>
     * Init read message event.
     * </p>
     *
     * @return {@link InstagramReadMessageEvent}.
     */
    @Override
    protected InstagramReadMessageEvent init() {
        return new InstagramReadMessageEvent();
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted.
     * @return the {@link PageReadMessageEvent}.
     */
    @Override
    public InstagramReadMessageEvent convert(final JsonObject data) {
        final InstagramReadMessageEvent readMessageEvent = init();

        readMessageEvent.setSenderId(data.getJsonObject(FBFields.SENDER).getLong(FBFields.ID));
        readMessageEvent.setReceiverId(data.getJsonObject(FBFields.RECIPIENT).getLong(FBFields.ID));
        readMessageEvent.setTimestamp(data.getLong(FBFields.TIMESTAMP));
        readMessageEvent.setMessageId(data.getJsonObject(FBFields.READ).getString(FBFields.MID));

        return readMessageEvent;
    }
}
