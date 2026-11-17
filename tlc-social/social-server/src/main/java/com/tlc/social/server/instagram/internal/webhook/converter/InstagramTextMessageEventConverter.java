package com.tlc.social.server.instagram.internal.webhook.converter;

import com.tlc.social.server.instagram.internal.webhook.model.InstagramTextMessageEvent;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageTextMessageEventConverter;

/**
 * <p>
 * Converts Instagram's text message event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramTextMessageEventConverter extends PageTextMessageEventConverter<InstagramTextMessageEvent> {

    private InstagramTextMessageEventConverter() {
    }

    private static final class Instance {
        private static final InstagramTextMessageEventConverter TEXT_MESSAGE_EVENT_CONVERTER = new InstagramTextMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramTextMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link InstagramTextMessageEventConverter}
     */
    public static InstagramTextMessageEventConverter getInstance() {
        return Instance.TEXT_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * <p>
     * Init text message event.
     * </p>
     *
     * @return {@link InstagramTextMessageEvent}.
     */
    @Override
    protected InstagramTextMessageEvent init() {
        return new InstagramTextMessageEvent();
    }
}
