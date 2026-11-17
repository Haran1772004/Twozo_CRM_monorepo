package com.tlc.social.server.whatsapp.internal.webhook.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.common.whatsapp.fields.WAFields;

import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAFailedMessageEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * A converter which converts the WhatsApp webhook failed message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAFailedMessageEventConverter implements WebhookMessageEventConverter<WAFailedMessageEvent> {

    private WAFailedMessageEventConverter() {
    }

    /**
     * Holds the instance of {@link WAFailedMessageEventConverter}
     */
    private static final class InstanceHolder {
        private static final WAFailedMessageEventConverter WA_FAILED_MESSAGE_EVENT_CONVERTER = new WAFailedMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WAFailedMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link WAFailedMessageEventConverter}
     */
    public static WAFailedMessageEventConverter getInstance() {
        return InstanceHolder.WA_FAILED_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return {@link WAFailedMessageEvent}
     */
    @Override
    public WAFailedMessageEvent convert(final JsonObject data) {
        final WAFailedMessageEvent waFailedMessageEvent = new WAFailedMessageEvent();
        final Long externalPhoneNumberId = data.getJsonObject(WAFields.METADATA).getLong(WAFields.PHONE_NUMBER_ID);
        final JsonObject statusJson = data.getJsonArray(WAFields.STATUSES).getJsonObject(NumericalConstant.ZERO);

        waFailedMessageEvent.setReceiverId(externalPhoneNumberId);
        waFailedMessageEvent.setMessageId(statusJson.getString(WAFields.ID));
        waFailedMessageEvent.setSenderId(statusJson.getLong(WAFields.RECIPIENT_ID));
        waFailedMessageEvent.setTimestamp(Instant.ofEpochSecond(statusJson.getLong(WAFields.TIMESTAMP)).toEpochMilli());
        final JsonObject errorJson = statusJson.getJsonArray(WAFields.ERRORS).getJsonObject(NumericalConstant.ZERO);

        if (Objects.nonNull(errorJson)) {
            final JsonObject errorData = errorJson.getJsonObject(WAFields.ERROR_DATA);

            if (Objects.nonNull(errorData)) {
                waFailedMessageEvent.setFailedReason(errorData.getString(WAFields.DETAILS));
            }
        }

        return waFailedMessageEvent;
    }
}