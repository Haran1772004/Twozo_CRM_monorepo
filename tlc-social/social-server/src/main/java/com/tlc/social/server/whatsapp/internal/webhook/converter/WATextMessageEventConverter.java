package com.tlc.social.server.whatsapp.internal.webhook.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.map.util.MapUrlProvider;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.whatsapp.internal.webhook.model.WATextMessageEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * A converter which converts the WhatsApp webhook text message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATextMessageEventConverter implements WebhookMessageEventConverter<WATextMessageEvent> {

    private WATextMessageEventConverter() {
    }

    /**
     * Holds the instance of {@link WATextMessageEventConverter}
     */
    private static final class InstanceHolder {
        private static final WATextMessageEventConverter TEXT_MESSAGE_EVENT_CONVERTER = new WATextMessageEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WATextMessageEventConverter}
     * </p>
     *
     * @return the instance of {@link WATextMessageEventConverter}
     */
    public static WATextMessageEventConverter getInstance() {
        return InstanceHolder.TEXT_MESSAGE_EVENT_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link WATextMessageEvent}
     */
    @Override
    public WATextMessageEvent convert(final JsonObject data) {
        final WATextMessageEvent textMessageEvent = new WATextMessageEvent();
        final JsonObject senderInfo = data.getJsonArray(WAFields.CONTACTS).getJsonObject(NumericalConstant.ZERO);
        final JsonObject messageJson = data.getJsonArray(WAFields.MESSAGES).getJsonObject(NumericalConstant.ZERO);

        if (Objects.nonNull(senderInfo)) {
            final Long waId = senderInfo.getLong(WAFields.WA_ID);
            final String profileName = senderInfo.getJsonObject(WAFields.PROFILE).getString(WAFields.NAME);

            textMessageEvent.setSenderProfileName(String.join("|", profileName, messageJson.getString(WAFields.FROM)));
            textMessageEvent.setSenderId(waId);
        }
        final Long externalPhoneNumberId = data.getJsonObject(WAFields.METADATA).getLong(WAFields.PHONE_NUMBER_ID);

        textMessageEvent.setReceiverId(externalPhoneNumberId);

        textMessageEvent.setMessageId(messageJson.getString(WAFields.ID));
        textMessageEvent.setTimestamp(Instant.ofEpochSecond(messageJson.getLong(WAFields.TIMESTAMP)).toEpochMilli());

        loadContentBasedOnType(textMessageEvent, messageJson);

        return textMessageEvent;
    }

    /**
     * <p>
     * Loads the content into {@link WATextMessageEvent} based on the type
     * </p>
     *
     * @param textMessageEvent Represents the {@link WATextMessageEvent}
     */
    private void loadContentBasedOnType(final WATextMessageEvent textMessageEvent, final JsonObject messageJson) {
        final String type = messageJson.getString(WAFields.TYPE);
        final WATextMessageEvent.ContentType contentType = WATextMessageEvent.ContentType.get(type);
        final JsonObject typeJson = messageJson.getJsonObject(type);

        switch (contentType) {
            case TEXT -> {
                final String content = typeJson.getString(WAFields.BODY);

                textMessageEvent.setText(content);
            }
            case IMAGE, AUDIO, DOCUMENT, VIDEO -> {
                final Long mediaId = typeJson.getLong(WAFields.ID);
                final String mimeType = typeJson.getString(WAFields.MIME_TYPE);

                textMessageEvent.setAttachment(new WATextMessageEvent.Attachment(mediaId, mimeType));
            }
            case LOCATION -> textMessageEvent.setText(MapUrlProvider.fetchUrl(typeJson.getString(WAFields.LATITUDE), typeJson.getString(WAFields.LONGITUDE)));
            case BUTTON -> textMessageEvent.setText(typeJson.getString(WAFields.PAYLOAD));
        }
    }
}