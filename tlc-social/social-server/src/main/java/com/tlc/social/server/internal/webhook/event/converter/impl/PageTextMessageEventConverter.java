package com.tlc.social.server.internal.webhook.event.converter.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.model.PageTextMessageEvent;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A converter which converts the {@link PageTextMessageEvent}
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class PageTextMessageEventConverter<T extends PageTextMessageEvent> implements WebhookMessageEventConverter<T> {

    protected PageTextMessageEventConverter() {
    }

    /**
     * <p>
     * Init text message event.
     * </p>
     *
     * @return {@link PageTextMessageEvent}.
     */
    protected abstract T init();

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link PageTextMessageEvent}
     */
    @Override
    public T convert(final JsonObject data) {
        final T textMessageEvent = init();
        final JsonObject messageJson = data.getJsonObject(FBFields.MESSAGE);

        textMessageEvent.setSenderId(data.getJsonObject(FBFields.SENDER).getLong(FBFields.ID));
        textMessageEvent.setReceiverId(data.getJsonObject(FBFields.RECIPIENT).getLong(FBFields.ID));
        textMessageEvent.setTimestamp(data.getLong(FBFields.TIMESTAMP));
        textMessageEvent.setMessageEcho(messageJson.optBoolean(FBFields.IS_ECHO));
        textMessageEvent.setMessageId(messageJson.getString(FBFields.MID));

        if (messageJson.containsKey(FBFields.TEXT)) {
            textMessageEvent.setText(messageJson.getString(FBFields.TEXT));
        } else if (messageJson.containsKey(FBFields.ATTACHMENTS)) {
            final JsonArray attachmentArray = messageJson.getJsonArray(FBFields.ATTACHMENTS);
            final int attachmentSize = attachmentArray.size();
            final Collection<PageTextMessageEvent.Attachment> attachments = new ArrayList<>();

            for (int index = 0; index < attachmentSize; index++) {
                final JsonObject attachmentJson = attachmentArray.getJsonObject(index);
                final JsonObject payloadJson = attachmentJson.getJsonObject(FBFields.PAYLOAD);
                final PageTextMessageEvent.Attachment attachment = new PageTextMessageEvent.Attachment(attachmentJson.getString(FBFields.TYPE),
                        new PageTextMessageEvent.Payload(payloadJson.getString(FBFields.URL)));

                attachments.add(attachment);
            }
            textMessageEvent.setAttachments(attachments);
        } else if (messageJson.optBoolean(FBFields.IS_UNSUPPORTED)) {
            throw ErrorCode.get(SocialErrorCodes.RECEIVED_UNSUPPORTED_MESSAGE, "i18n.social.webhook.received.unsupported.message");
        }

        return textMessageEvent;
    }
}