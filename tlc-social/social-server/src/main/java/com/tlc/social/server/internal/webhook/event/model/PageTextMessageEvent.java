package com.tlc.social.server.internal.webhook.event.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * A model which represents the fb webhook text message event
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public abstract class PageTextMessageEvent extends TextMessageEvent {

    private boolean isMessageEcho;
    private String messageId;
    private String text;
    private Collection<Attachment> attachments;

    protected PageTextMessageEvent() {
    }

    /**
     * A record which encapsulates the attachment details
     *
     * @author Maharaja S
     * @version 1.0
     */
    public record Attachment(String type, Payload payload) {
    }

    /**
     * A record which encapsulates the attachment payload details
     *
     * @author Maharaja S
     * @version 1.0
     */
    public record Payload(String url) {
    }
}