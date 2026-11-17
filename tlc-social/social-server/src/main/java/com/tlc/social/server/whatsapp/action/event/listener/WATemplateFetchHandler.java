package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.whatsapp.converter.WATemplateConverter;
import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Optional;

/**
 * A handler which listens to the Whatsapp message template fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_TEMPLATE)
public class WATemplateFetchHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;
    private final WATemplateConverter waTemplateConverter;

    public WATemplateFetchHandler() {
        waBusinessManager = WABusinessManager.getInstance();
        waTemplateConverter = WATemplateConverter.getInstance();
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
        final Long templateId = data.getLong(WAFields.TEMPLATE_ID);
        final Optional<WATemplate> optionalWATemplate = waBusinessManager.getTemplate(templateId);

         if (optionalWATemplate.isPresent()) {
             response.put(WAFields.TEMPLATE, waTemplateConverter.convert(optionalWATemplate.get()));
         } else {
             throw ErrorCode.get(WAErrorCodes.TEMPLATE_NOT_FOUND, "i18n.social.whatsapp.template.not.found");
         }

        return response.getBytes();
    }
}