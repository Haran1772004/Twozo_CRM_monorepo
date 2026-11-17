package com.tlc.social.server.whatsapp.internal.webhook.model;

import com.tlc.social.server.internal.webhook.event.model.DeliveryMessageEvent;

/**
 * Represents a model for the WhatsApp webhook delivery message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WADeliveryMessageEvent extends DeliveryMessageEvent {

    private String messageId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }
}
