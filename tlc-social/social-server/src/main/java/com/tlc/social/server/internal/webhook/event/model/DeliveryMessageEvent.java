package com.tlc.social.server.internal.webhook.event.model;

/**
 * A model which represents the webhook message delivery event
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class DeliveryMessageEvent extends WebhookMessageEvent {

    public DeliveryMessageEvent() {
        super(MessageEventType.DELIVERY);
    }
}
