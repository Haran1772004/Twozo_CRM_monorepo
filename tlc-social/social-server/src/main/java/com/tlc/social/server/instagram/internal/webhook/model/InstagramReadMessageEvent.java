package com.tlc.social.server.instagram.internal.webhook.model;

import com.tlc.social.server.internal.webhook.event.model.PageReadMessageEvent;

/**
 * <p>
 * Read message event related for Instagram.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramReadMessageEvent extends PageReadMessageEvent {

    private String messageId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }
}