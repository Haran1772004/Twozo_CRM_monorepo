package com.tlc.commons.webhook.receiver;

import com.tlc.commons.webhook.Webhook;

/**
 * Responsible for managing the lifecycle of webhook subscriptions with external providers.
 *
 * <p>
 * Allows for consistent handling of subscribe/unsubscribe
 * operations across different webhook services.
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public interface WebhookReceiver extends Webhook {

    /**
     * Initiates a subscription with the webhook provider.
     *
     * <p>
     * Register the callback URL with the
     * external service so that notifications can be received.
     * </p>
     */
    void subscribe();

    /**
     * Terminates an existing subscription with the webhook provider.
     *
     * <p>
     * Clean up any server-side or remote configurations
     * previously established during the subscription phase.
     * </p>
     */
    void unsubscribe();
}