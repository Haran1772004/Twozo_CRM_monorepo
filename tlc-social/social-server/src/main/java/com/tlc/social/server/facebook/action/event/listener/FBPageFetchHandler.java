package com.tlc.social.server.facebook.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.facebook.converter.FBPageConverter;
import com.tlc.social.common.facebook.event.FBEvents;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.internal.status.FBErrorCodes;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.server.facebook.manager.FBPageManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;

/**
 * A handler which listens to the facebook page fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = FBEvents.FETCH_FB_PAGES)
public class FBPageFetchHandler implements EventConsumer<byte[]> {

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
        final FBPageManager fbPageManager = FBPageManager.getInstance();
        final FBPageConverter fbPageConverter = FBPageConverter.getInstance();
        final Collection<Long> pageIds = (Collection<Long>) data.get(FBFields.PAGE_IDS);

        if (!fbPageManager.exists(pageIds)) {
            throw ErrorCode.get(FBErrorCodes.PAGE_NOT_FOUND, "i18n.social.facebook.page.not.found");
        }

        for (final FBPage fbPage : fbPageManager.get(pageIds)) {
            pageArray.put(fbPageConverter.convert(fbPage));
        }
        response.put(FBFields.PAGES, pageArray);

        return response.getBytes();
    }
}
