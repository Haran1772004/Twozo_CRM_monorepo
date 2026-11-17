package com.tlc.social.server.instagram.internal.webhook.converter;

import com.tlc.social.server.instagram.internal.webhook.model.InstagramDeliveryMessageEvent;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageDeliveryMessageEventConverter;

/**
 * <p>
 * Converts Instagram's delivery message event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramDeliveryMessageEventConverter extends PageDeliveryMessageEventConverter<InstagramDeliveryMessageEvent> {

    private InstagramDeliveryMessageEventConverter() {
    }

    private static final class Instance {
        private static final InstagramDeliveryMessageEventConverter TEXT_MESSAGE_EVENT_CONVERTER = new InstagramDeliveryMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramDeliveryMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link InstagramDeliveryMessageEventConverter}
     */
    public static InstagramDeliveryMessageEventConverter getInstance() {
        return InstagramDeliveryMessageEventConverter.Instance.TEXT_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * <p>
     * Init delivery message event.
     * </p>
     *
     * @return {@link InstagramDeliveryMessageEvent}.
     */
    @Override
    protected InstagramDeliveryMessageEvent init() {
        return new InstagramDeliveryMessageEvent();
    }
}
