package com.tlc.social.server.internal.webhook.event.model;

/**
 * A model which represents the webhook text message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class TextMessageEvent extends WebhookMessageEvent {

    private String messageId;
    private String text;

    public TextMessageEvent() {
        super(MessageEventType.TEXT);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }
}
