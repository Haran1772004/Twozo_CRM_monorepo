package com.tlc.social.server.internal.webhook.event.model;

/**
 * A model which represents the webhook failed message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class FailedMessageEvent extends WebhookMessageEvent {

    private String messageId;
    private String failedReason;

    public FailedMessageEvent() {
        super(MessageEventType.FAILED);
    }

    public String getFailedReason() {
        return failedReason;
    }

    public void setFailedReason(final String failedReason) {
        this.failedReason = failedReason;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }
}