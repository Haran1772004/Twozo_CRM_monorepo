package com.tlc.social.server.facebook.internal.webhook.converter;

import com.tlc.social.server.facebook.internal.webhook.model.FBDeliveryMessageEvent;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageDeliveryMessageEventConverter;

/**
 * <p>
 * Converts Facebook's delivery message event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class FBDeliveryMessageEventConverter extends PageDeliveryMessageEventConverter<FBDeliveryMessageEvent> {

    private FBDeliveryMessageEventConverter() {
    }

    private static final class Instance {
        private static final FBDeliveryMessageEventConverter TEXT_MESSAGE_EVENT_CONVERTER = new FBDeliveryMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link FBDeliveryMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link FBDeliveryMessageEventConverter}
     */
    public static FBDeliveryMessageEventConverter getInstance() {
        return Instance.TEXT_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * <p>
     * Init delivery message event.
     * </p>
     *
     * @return {@link FBDeliveryMessageEvent}.
     */
    @Override
    protected FBDeliveryMessageEvent init() {
        return new FBDeliveryMessageEvent();
    }
}
