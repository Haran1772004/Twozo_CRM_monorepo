package com.tlc.social.server.internal.webhook.event.model;

/**
 * A model which represents the webhook message read event
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class ReadMessageEvent extends WebhookMessageEvent {

    public ReadMessageEvent() {
        super(MessageEventType.READ);
    }
}
