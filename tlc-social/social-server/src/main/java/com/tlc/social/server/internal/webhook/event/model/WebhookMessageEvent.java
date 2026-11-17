package com.tlc.social.server.internal.webhook.event.model;

import com.tlc.social.common.model.MessageEvent;

/**
 * Represents an abstract model for the webhook message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class WebhookMessageEvent implements MessageEvent {

    private final MessageEventType messageEventType;

    private Long senderId;
    private Long receiverId;
    private Long timestamp;

    protected WebhookMessageEvent(final MessageEventType messageEventType) {
        this.messageEventType = messageEventType;
    }

    public MessageEventType getMessageEventType() {
        return messageEventType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(final Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(final Long receiverId) {
        this.receiverId = receiverId;
    }

    /**
     * An enum which contains the webhook message event type
     */
    public enum MessageEventType {

        TEXT,
        READ,
        DELIVERY,
        FAILED,
        TEMPLATE_UPDATE,
        WA_ACCOUNT_UPDATE,
    }
}