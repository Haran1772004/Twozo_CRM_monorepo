package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.HashSet;

/**
 * A handler which listens to the WhatsApp template count fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_TEMPLATE_COUNT)
public class WATemplateCountEventHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;

    public WATemplateCountEventHandler() {
        waBusinessManager = WABusinessManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Represents the data to be consumed
     * @return the response in bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final Long businessAccountId = data.getLong(WAFields.BUSINESS_ACCOUNT_ID);
        final JsonArray statusArray = data.getJsonArray(WAFields.STATUS);
        final Collection<Short> statusIds = new HashSet<>(statusArray.size());

        statusArray.forEach(statusId -> statusIds.add(((Number) statusId).shortValue()));

        if (!waBusinessManager.isBusinessAccountExists(businessAccountId)) {
            throw ErrorCode.get(WAErrorCodes.WA_BUSINESS_ACCOUNT_NOT_FOUND, "i18n.social.whatsapp.business.account.not.found");
        }

        response.put(WAFields.TEMPLATE_COUNT, waBusinessManager.getTemplateCount(businessAccountId, null, null, statusIds, null));

        return response.getBytes();
    }
}