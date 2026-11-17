package com.tlc.social.server.facebook.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.facebook.event.FBEvents;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.internal.status.FBErrorCodes;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.server.facebook.internal.service.FBWebhookService;
import com.tlc.social.server.facebook.manager.FBPageManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.List;

/**
 * A handler which listens to the fb page webhook unsubscribe event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = FBEvents.UNSUBSCRIBE_FB_PAGES)
public class FBPageUnsubscribeHandler implements EventConsumer<byte[]> {
    private final FBPageManager fbPageManager;
    private final FBWebhookService fbWebhookService;

    public FBPageUnsubscribeHandler() {
        fbPageManager = FBPageManager.getInstance();
        fbWebhookService = FBWebhookService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Represents the bytes to be consumed
     * @return the response in bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final Long pageId = data.getLong(FBFields.PAGE_ID);
        final List<Long> pageIds = List.of(pageId);
        final JsonObject response = Json.object();

        if (!fbPageManager.exists(pageIds)) {
            throw ErrorCode.get(FBErrorCodes.PAGE_NOT_FOUND, "i18n.social.facebook.page.not.found");
        }

        for (final FBPage fbPage : fbPageManager.get(pageIds)) {
            fbWebhookService.unsubscribe(fbPage);
        }

        return response.put(FBFields.IS_PAGE_UNSUBSCRIBED, true).getBytes();
    }
}
