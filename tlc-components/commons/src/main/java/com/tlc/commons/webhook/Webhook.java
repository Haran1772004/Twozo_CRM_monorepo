package com.tlc.commons.webhook;

/**
 * Represents a webhook, an HTTP callback used to receive real-time event notifications from external systems.
 * Typically models messages from third-party services and can be extended with event data.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public interface Webhook {

    /**
     * Returns the webhook notification URL for sending/receiving events.
     *
     * <p>
     * The URL acts as the endpoint and is typically registered with
     * the provider during the subscription process. It may be static or
     * generated dynamically based on runtime context.
     * </p>
     *
     * @return the callback URL for sending/receiving webhook notifications
     */
    String getNotificationUrl();
}