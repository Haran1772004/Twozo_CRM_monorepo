package com.tlc.social.server.whatsapp.internal.webhook.model;

import com.tlc.social.server.internal.webhook.event.model.ReadMessageEvent;

/**
 * Represents a model for the WhatsApp webhook read message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAReadMessageEvent extends ReadMessageEvent {

    private String messageId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }
}
