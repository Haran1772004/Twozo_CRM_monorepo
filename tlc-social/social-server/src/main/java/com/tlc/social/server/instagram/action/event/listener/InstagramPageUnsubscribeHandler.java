package com.tlc.social.server.instagram.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.instagram.event.InstagramEvents;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.instagram.status.InstagramErrorCodes;
import com.tlc.social.server.instagram.internal.service.InstagramWebhookService;
import com.tlc.social.server.instagram.manager.InstagramPageManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.List;

/**
 * <p>
 * Handles Instagram pages unsubscribe action.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@EventListener(name = InstagramEvents.UNSUBSCRIBE_INSTAGRAM_PAGES)
public class InstagramPageUnsubscribeHandler implements EventConsumer<byte[]> {

    private final InstagramPageManager instagramPageManager;
    private final InstagramWebhookService instagramWebhookService;

    public InstagramPageUnsubscribeHandler() {
        instagramPageManager = InstagramPageManager.getInstance();
        instagramWebhookService = InstagramWebhookService.getInstance();
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

        if (instagramPageManager.exists(pageIds)) {
            for (final InstagramPage instagramPage : instagramPageManager.get(pageIds)) {
                instagramWebhookService.unsubscribe(instagramPage);
            }

            return response.put(FBFields.IS_PAGE_UNSUBSCRIBED, true).getBytes();
        } else {
            throw ErrorCode.get(InstagramErrorCodes.PAGE_NOT_FOUND, "i18n.social.instagram.page.not.found");
        }
    }
}
