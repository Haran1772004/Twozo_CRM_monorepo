package com.tlc.notification.model.call;

/**
 * Represents a recipient in a Twilio WebRTC call.
 *
 * @param identity The unique identity of the WebRTC recipient (e.g., an agent or user or number).
 *
 * @author Sathish
 * @version 1.0
 */
public record Recipient(String identity, String aliasName) {

    public Recipient(final String identity) {
        this(identity, null);
    }
}
