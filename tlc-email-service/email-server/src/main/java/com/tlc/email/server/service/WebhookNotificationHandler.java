package com.tlc.email.server.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.server.internal.webhook.receiver.impl.GoogleWebhookNotificationHandler;
import com.tlc.email.server.internal.webhook.receiver.impl.OutlookWebhookNotificationHandler;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;

/**
 * Handles incoming webhook notifications.
 *
 * @author kowsalya SP
 * @version 1.0
 */
public interface WebhookNotificationHandler {

    static WebhookNotificationHandler get(final EmailProvider emailProvider) {
        return switch (emailProvider) {
            case GOOGLE -> GoogleWebhookNotificationHandler.getInstance();
            case OUTLOOK -> OutlookWebhookNotificationHandler.getInstance();
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_PROVIDER);
        };
    }

    /**
     * Handles the webhook notification payload.
     *
     * @param payload the notification content
     */
    void handle(final JsonObject payload);
}
