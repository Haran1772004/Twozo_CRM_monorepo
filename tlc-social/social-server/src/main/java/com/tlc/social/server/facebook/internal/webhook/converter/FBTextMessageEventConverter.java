package com.tlc.social.server.facebook.internal.webhook.converter;

import com.tlc.social.server.facebook.internal.webhook.model.FBTextMessageEvent;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageTextMessageEventConverter;

/**
 * <p>
 * Converts Facebook's text message event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class FBTextMessageEventConverter extends PageTextMessageEventConverter<FBTextMessageEvent> {

    private FBTextMessageEventConverter() {
    }

    private static final class Instance {
        private static final FBTextMessageEventConverter TEXT_MESSAGE_EVENT_CONVERTER = new FBTextMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link FBTextMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link FBTextMessageEventConverter}
     */
    public static FBTextMessageEventConverter getInstance() {
        return Instance.TEXT_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * <p>
     * Init text message event.
     * </p>
     *
     * @return {@link FBTextMessageEvent}.
     */
    @Override
    protected FBTextMessageEvent init() {
        return new FBTextMessageEvent();
    }
}
