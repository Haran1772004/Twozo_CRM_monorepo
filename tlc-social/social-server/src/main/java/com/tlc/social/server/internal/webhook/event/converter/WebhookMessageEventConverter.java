package com.tlc.social.server.internal.webhook.event.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.model.MessageEvent;

/**
 * A converter which converts the various webhook message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface WebhookMessageEventConverter<T extends MessageEvent> {

    /**
     * <p>
     * Converts the provided json data into the {@link MessageEvent}
     * </p>
     *
     * @param data Represents the data to be converted
     * @return the type model that extends {@link MessageEvent}
     */
    T convert(final JsonObject data);
}
