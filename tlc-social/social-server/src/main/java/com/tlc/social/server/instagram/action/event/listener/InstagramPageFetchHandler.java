package com.tlc.social.server.instagram.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.instagram.converter.InstagramPageConverter;
import com.tlc.social.common.instagram.event.InstagramEvents;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.instagram.status.InstagramErrorCodes;
import com.tlc.social.server.instagram.manager.InstagramPageManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;

/**
 * <p>
 * Handles Instagram pages fetch action.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@EventListener(name = InstagramEvents.FETCH_INSTAGRAM_PAGES)
public class InstagramPageFetchHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response in byte array
     */
    @Override
    public byte[] consume(byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final JsonArray pageArray = Json.array();
        final InstagramPageManager instagramPageManager = InstagramPageManager.getInstance();
        final InstagramPageConverter instagramPageConverter = InstagramPageConverter.getInstance();
        final Collection<Long> pageIds = (Collection<Long>) data.get(FBFields.PAGE_IDS);

        if (!instagramPageManager.exists(pageIds)) {
            throw ErrorCode.get(InstagramErrorCodes.PAGE_NOT_FOUND, "i18n.social.instagram.page.not.found");
        }

        for (final InstagramPage instagramPage : instagramPageManager.get(pageIds)) {
            pageArray.put(instagramPageConverter.convert(instagramPage));
        }
        response.put(FBFields.PAGES, pageArray);

        return response.getBytes();
    }
}
