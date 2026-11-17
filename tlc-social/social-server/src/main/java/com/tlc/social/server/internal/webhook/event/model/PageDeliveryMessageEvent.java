package com.tlc.social.server.internal.webhook.event.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * A model which represents the webhook message delivery event
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public abstract class PageDeliveryMessageEvent extends DeliveryMessageEvent {

    private Collection<String> messageIds;

    /**
     * Represents that all messages sent at or before the watermark have been delivered
     */
    private long watermark;

    protected PageDeliveryMessageEvent() {

    }
}