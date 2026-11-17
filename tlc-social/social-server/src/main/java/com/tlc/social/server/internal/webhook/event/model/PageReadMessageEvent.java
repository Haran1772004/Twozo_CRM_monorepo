package com.tlc.social.server.internal.webhook.event.model;

/**
 * A model which represents the Facebook read message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class PageReadMessageEvent extends ReadMessageEvent {

    /**
     * Represents that all messages sent at or before the watermark have been read
     */
    private long watermark;

    public long getWatermark() {
        return watermark;
    }

    public void setWatermark(final long watermark) {
        this.watermark = watermark;
    }
}
