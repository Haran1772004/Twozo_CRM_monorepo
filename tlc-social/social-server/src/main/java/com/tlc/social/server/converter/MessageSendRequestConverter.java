package com.tlc.social.server.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.server.model.MessageSendRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A converter which provides various methods to convert the data from/into {@link MessageSendRequest}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MessageSendRequestConverter implements MessageRequestConverter<MessageSendRequest> {

    private MessageSendRequestConverter() {
    }

    private static final class InstanceHolder {
        private static final MessageSendRequestConverter MESSAGE_SEND_REQUEST_CONVERTER = new MessageSendRequestConverter();
    }

    /**
     * <p>
     * Gets the instance of{@link MessageSendRequestConverter}
     * </p>
     *
     * @return the instance of{@link MessageSendRequestConverter}
     */
    public static MessageSendRequestConverter getInstance() {
        return InstanceHolder.MESSAGE_SEND_REQUEST_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the json data to be converted
     * @return the {@link MessageSendRequest}
     */
    @Override
    public MessageSendRequest convert(final JsonObject data) {
        final MessageSendRequest messageSendRequest = new MessageSendRequest();
        final Platform platform = Platform.get(data.getShort(ChatFields.PLATFORM));
        final boolean isChatIdExits = data.containsKey(ChatFields.CHAT_ID);

        if (isChatIdExits) {
            messageSendRequest.setChatId(data.getLong(ChatFields.CHAT_ID));
        } else {
            messageSendRequest.setNewChat(true);
        }
        messageSendRequest.setMessage(data.getNullableString(ChatFields.MESSAGE));
        messageSendRequest.setPlatform(platform);

        if (data.containsKey(ChatFields.ATTACHMENTS)) {
            final Collection<Attachment> attachments = new ArrayList<>();
            final JsonArray attachmentArray = data.getJsonArray(ChatFields.ATTACHMENTS);
            final int attachmentSize = attachmentArray.size();

            for (int index = 0; index < attachmentSize; index++) {
                final JsonObject attachmentJson = attachmentArray.getJsonObject(index);
                final Attachment attachment = new Attachment();

                attachment.setId(attachmentJson.getLong(ChatFields.ID));
                attachment.setFileName(attachmentJson.getString(ChatFields.NAME));
                attachments.add(attachment);
            }
            messageSendRequest.setAttachments(attachments);
        }

        if (Platform.WHATSAPP == platform && data.containsKey(ChatFields.TEMPLATE_ID)) {
            final MessageSendRequest.TemplateRequestData templateRequestData = new MessageSendRequest.TemplateRequestData(data.getLong(ChatFields.TEMPLATE_ID),
                    data.containsKey(ChatFields.PARAM_DATA) ? data.getJsonObject(ChatFields.PARAM_DATA) : Json.object());

            messageSendRequest.setTemplateRequestData(templateRequestData);

            if (!isChatIdExits) {
                final Collection<String> recipientPhoneNumbers;

                if (data.containsKey(ChatFields.RECIPIENT_PHONE_NUMBER)) {
                    recipientPhoneNumbers = List.of(data.getString(ChatFields.RECIPIENT_PHONE_NUMBER));
                } else {
                    recipientPhoneNumbers = (Collection<String>) data.get(ChatFields.RECIPIENT_PHONE_NUMBERS);
                }
                messageSendRequest.setRecipientPhoneNumbers(recipientPhoneNumbers);
                messageSendRequest.setSenderId(data.getLong(WAFields.BUSINESS_PHONE_NUMBER_ID));
            }
        }

        return messageSendRequest;
    }
}
