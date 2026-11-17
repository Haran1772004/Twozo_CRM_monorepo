package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.Map;

/**
 * A handler which listens to the Whatsapp message template names fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_TEMPLATE_NAMES)
public class WATemplateNamesFetchHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;

    public WATemplateNamesFetchHandler() {
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
        final Collection<Long> templateIds = (Collection<Long>) data.get(WAFields.TEMPLATE_IDS);
        final Map<Long, String> templateNames = waBusinessManager.getTemplateNames(templateIds);

        if (templateNames.isEmpty()) {
            throw ErrorCode.get(WAErrorCodes.TEMPLATE_NOT_FOUND, "i18n.social.whatsapp.template.not.found");
        }
        templateNames.forEach((key, value) -> response.put(String.valueOf(key), value));

        return response.getBytes();
    }
}