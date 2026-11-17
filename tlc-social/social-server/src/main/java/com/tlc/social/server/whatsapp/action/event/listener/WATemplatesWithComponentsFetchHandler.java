package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.common.whatsapp.converter.WATemplateConverter;
import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * A handler which listens to the WhatsApp message templates with preview fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_TEMPLATES_WITH_COMPONENTS)
public class WATemplatesWithComponentsFetchHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;
    private final WATemplateConverter waTemplateConverter;

    public WATemplatesWithComponentsFetchHandler() {
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
        final JsonArray templateArray = Json.array();
        final Long businessAccountId = data.getLong(WAFields.BUSINESS_ACCOUNT_ID);
        final int start = data.getInt(WAFields.START);
        final int limit = data.getInt(WAFields.LIMIT);
        final String searchText = data.optString(WAFields.SEARCH_TEXT, null);

        final Function<String, Collection<Short>> idExtractor = (key) -> {
            final JsonArray jsonArray = data.optJsonArray(key);

            if (Objects.isNull(jsonArray) || NumericalConstant.ZERO == jsonArray.size()) {
                return null;
            }
            final Collection<Short> values = new HashSet<>(jsonArray.size());

            jsonArray.forEach(value -> values.add(((Number) value).shortValue()));

            return values;
        };
        final Collection<Short> categoryIds = idExtractor.apply(WAFields.CATEGORY);
        final Collection<Short> statusIds = idExtractor.apply(WAFields.STATUS);
        final Collection<Short> languageIds = idExtractor.apply(WAFields.LANGUAGE);
        final long totalRecords = waBusinessManager.getTemplateCount(businessAccountId, searchText, categoryIds, statusIds, languageIds);

        if (NumericalConstant.ZERO < totalRecords) {

            if (data.optBoolean(WAFields.PREVIEW_NEEDED, false)) {
                final Collection<WATemplate> templatesWithComponents = waBusinessManager.getTemplatesWithComponents(businessAccountId,
                        start, limit, searchText, categoryIds, statusIds, languageIds);

                for (final WATemplate waTemplate : templatesWithComponents) {
                    templateArray.put(waTemplateConverter.convert(waTemplate));
                }
            } else {
                final Collection<WATemplate> templates = waBusinessManager.getTemplates(businessAccountId, start,
                        limit, searchText, categoryIds, statusIds, languageIds);

                for (final WATemplate waTemplate : templates) {
                    final JsonObject templateJson = Json.object();

                    templateJson.put(WAFields.ID, waTemplate.id());
                    templateJson.put(WAFields.NAME, waTemplate.getName());
                    templateJson.put(WAFields.LANGUAGE, waTemplate.getLanguage().getCode());
                    templateJson.put(WAFields.CATEGORY, waTemplate.getCategory().getId());
                    templateJson.put(WAFields.STATUS, waTemplate.getStatus().getId());

                    templateArray.put(templateJson);
                }
            }
        }
        response.put(WAFields.TOTAL_RECORDS, totalRecords);
        response.put(WAFields.TEMPLATES, templateArray);

        return response.getBytes();
    }
}