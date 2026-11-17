package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A handler which listens to the Whatsapp message templates fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_TEMPLATES)
public class WATemplatesFetchHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;

    public WATemplatesFetchHandler() {
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
        final JsonArray templateArray = Json.array();
        final JsonObject response = Json.object();
        final Long businessAccountId = data.getLong(WAFields.BUSINESS_ACCOUNT_ID);
        final int start = data.getInt(WAFields.START);
        final int limit = data.getInt(WAFields.LIMIT);
        final String searchText = data.optString(WAFields.SEARCH_TEXT, null);
        final long totalRecords = waBusinessManager.getTotalTemplateCount(businessAccountId, searchText);

        if (NumericalConstant.ZERO < totalRecords) {
            for (final WATemplate waTemplate : waBusinessManager.getTemplates(businessAccountId, start, limit, searchText)) {
                final JsonObject templateJson = Json.object();

                templateJson.put(WAFields.ID, waTemplate.id());
                templateJson.put(WAFields.LANGUAGE, waTemplate.getLanguage().getCode());
                templateJson.put(WAFields.NAME, waTemplate.getName());

                templateArray.put(templateJson);
            }
        }
        response.put(WAFields.TOTAL_RECORDS, totalRecords);
        response.put(WAFields.TEMPLATES, templateArray);

        return response.getBytes();
    }
}