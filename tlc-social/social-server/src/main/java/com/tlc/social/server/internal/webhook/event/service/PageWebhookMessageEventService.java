package com.tlc.social.server.internal.webhook.event.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.model.Platform;
import com.tlc.social.server.internal.webhook.event.service.impl.PageWebhookMessageEventServiceImpl;
import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;

/**
 * <p>
 *
 * </p>
 */
public interface PageWebhookMessageEventService {

    static PageWebhookMessageEventService getInstance() {
       return PageWebhookMessageEventServiceImpl.getInstance();
    }

    WebhookMessageEvent get(final JsonObject data, final Platform platform);
}