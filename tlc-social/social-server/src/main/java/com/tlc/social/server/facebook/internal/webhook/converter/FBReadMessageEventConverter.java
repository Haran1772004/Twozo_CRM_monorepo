package com.tlc.social.server.facebook.internal.webhook.converter;

import com.tlc.social.server.facebook.internal.webhook.model.FBReadMessageEvent;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageReadMessageEventConverter;

/**
 * <p>
 * Converts Facebook's read message event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class FBReadMessageEventConverter extends PageReadMessageEventConverter<FBReadMessageEvent> {

    private FBReadMessageEventConverter() {
    }

    private static final class Instance {
        private static final FBReadMessageEventConverter TEXT_MESSAGE_EVENT_CONVERTER = new FBReadMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link FBReadMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link FBReadMessageEventConverter}
     */
    public static FBReadMessageEventConverter getInstance() {
        return Instance.TEXT_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * <p>
     * Init read message event.
     * </p>
     *
     * @return {@link FBReadMessageEvent}.
     */
    @Override
    protected FBReadMessageEvent init() {
        return new FBReadMessageEvent();
    }
}
