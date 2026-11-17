package com.tlc.social.server.service;

import com.tlc.commons.json.JsonObject;

import com.tlc.social.server.internal.webhook.event.model.DeliveryMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.ReadMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.PageTextMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.TextMessageEvent;

/**
 * Provides abstract services to handle the webhook events
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface WebhookInboundService {

    /**
     * Handles the test message event
     *
     * @param textMessageEvent Represents the {@link PageTextMessageEvent}
     * @return the json response
     */
    JsonObject handleTextMessageEvent(final TextMessageEvent textMessageEvent);

    /**
     * Handles the delivery message event
     *
     * @param deliveryMessageEvent Represents the {@link DeliveryMessageEvent}
     */
    JsonObject handleDeliveryMessageEvent(final DeliveryMessageEvent deliveryMessageEvent);

    /**
     * Handles the read message event
     *
     * @param readMessageEvent Represents the {@link ReadMessageEvent}
     */
    JsonObject handleReadMessageEvent(final ReadMessageEvent readMessageEvent);
}