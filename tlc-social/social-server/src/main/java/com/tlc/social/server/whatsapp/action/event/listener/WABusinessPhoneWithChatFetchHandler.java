package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.whatsapp.converter.WABusinessPhoneNumberConverter;
import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.Map;

/**
 * A handler which listens to the Whatsapp business phone numbers with chat fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_BUSINESS_PHONE_NUMBERS_WITH_CHAT)
public class WABusinessPhoneWithChatFetchHandler implements EventConsumer<byte[]> {

    private static final String NON_DIGIT_REGEX_PATTERN = "[^\\d]";

    private final WABusinessManager waBusinessManager;
    private final WABusinessPhoneNumberConverter waBusinessPhoneNumberConverter;

    public WABusinessPhoneWithChatFetchHandler() {
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
        final Collection<Long> businessPhoneNumberIds = (Collection<Long>) data.get(WAFields.BUSINESS_PHONE_NUMBER_IDS);
        final Long recipientPhoneNumber = Long.parseLong(data.getString(ChatFields.RECIPIENT_PHONE_NUMBER).replaceAll(NON_DIGIT_REGEX_PATTERN, ""));

        if (!waBusinessManager.exists(businessPhoneNumberIds)) {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.social.whatsapp.business.phone.not.found");
        }
        final Map<Long, Long> chatIdMap = waBusinessManager.getChatIdMap(businessPhoneNumberIds, recipientPhoneNumber);

        for (final WABusinessPhoneNumber waBusinessPhoneNumber : waBusinessManager.getBusinessPhoneNumbers(businessPhoneNumberIds)) {
            final JsonObject businessPhoneJson = waBusinessPhoneNumberConverter.convert(waBusinessPhoneNumber);

            businessPhoneJson.put(ChatFields.CHAT_ID, chatIdMap.get(waBusinessPhoneNumber.id()));
            phoneArray.put(businessPhoneJson);
        }
        response.put(WAFields.BUSINESS_PHONE_NUMBERS, phoneArray);

        return response.getBytes();
    }
}
