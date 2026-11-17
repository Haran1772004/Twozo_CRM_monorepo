package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.common.whatsapp.converter.WABusinessPhoneNumberConverter;
import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * A handler which listens to the Whatsapp business phone numbers fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_BUSINESS_PHONE_NUMBERS)
public class WABusinessPhoneFetchHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;
    private final WABusinessPhoneNumberConverter waBusinessPhoneNumberConverter;

    public WABusinessPhoneFetchHandler() {
        waBusinessManager = WABusinessManager.getInstance();
        waBusinessPhoneNumberConverter = WABusinessPhoneNumberConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Represents the data to be consumed
     * @return the response as bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final JsonArray phoneArray = Json.array();
        final Collection<Long> phoneNumberIds = (Collection<Long>) data.get(WAFields.PHONE_NUMBER_IDS);
        final boolean includeTemplateCount = data.optBoolean(WAFields.INCLUDE_TEMPLATE_COUNT);

        if (!waBusinessManager.exists(phoneNumberIds)) {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.social.whatsapp.business.phone.not.found");
        }

        if (includeTemplateCount) {
            final String searchText = data.optString(WAFields.SEARCH_TEXT, null);
            final int start = data.getInt(WAFields.START);
            final int limit = data.getInt(WAFields.LIMIT);
            final Collection<Short> statusIds = getStatusIds(data);
            final long totalRecords = waBusinessManager.getBusinessPhoneNumberCount(phoneNumberIds, searchText);
            final Collection<WABusinessPhoneNumber> businessPhoneNumbers = NumericalConstant.ZERO < totalRecords
                    ? waBusinessManager.getBusinessPhoneNumbers(phoneNumberIds, searchText, start, limit) : Collections.emptyList();
            final Collection<Long> businessAccountIds = businessPhoneNumbers.stream().map(WABusinessPhoneNumber::getWaBusinessAccountId).toList();
            final Map<Long, Long> templateCountMap = businessAccountIds.isEmpty() ? Collections.emptyMap()
                    : waBusinessManager.getTemplateCount(businessAccountIds, statusIds);

            for (final WABusinessPhoneNumber waBusinessPhoneNumber : businessPhoneNumbers) {

                waBusinessPhoneNumber.setTemplateCount(templateCountMap.get(waBusinessPhoneNumber.getWaBusinessAccountId()));
                phoneArray.put(waBusinessPhoneNumberConverter.convert(waBusinessPhoneNumber));
            }
            response.put(WAFields.TOTAL_RECORDS, totalRecords);
        } else {

            for (final WABusinessPhoneNumber waBusinessPhoneNumber : waBusinessManager.getBusinessPhoneNumbers(phoneNumberIds)) {
                phoneArray.put(waBusinessPhoneNumberConverter.convert(waBusinessPhoneNumber));
            }
        }

        response.put(WAFields.BUSINESS_PHONE_NUMBERS, phoneArray);

        return response.getBytes();
    }

    /**
     * <p>
     * Gets the status ids from the json data
     * </p>
     *
     * @param data Represents the json data
     * @return the status ids
     */
    private Collection<Short> getStatusIds(final JsonObject data) {
        final JsonArray jsonArray = data.optJsonArray(WAFields.STATUS);

        if (Json.isEmpty(jsonArray)) {
            return null;
        }
        final Collection<Short> values = new HashSet<>(jsonArray.size());

        jsonArray.forEach(value -> values.add(((Number) value).shortValue()));

        return values;
    }
}
