package com.tlc.social.common.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.models.Attachment;

import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A converter which provides various ways to convert the data from/into {@link Message}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MessageConverter implements SocialEntityConverter<Message> {

    private MessageConverter() {
    }

    private static final class InstanceHolder {
        private static final MessageConverter MESSAGE_CONVERTER = new MessageConverter();
    }

    public static MessageConverter getInstance() {
        return InstanceHolder.MESSAGE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Represents the json data to be converted
     * @return the {@link Message}
     */
    @Override
    public Message convert(final JsonObject jsonObject) {
        final Message message = new Message();

        if (jsonObject.containsKey(MessageFields.CONTENT)) {
            message.setContent(jsonObject.getString(MessageFields.CONTENT));
        }

        if (jsonObject.containsKey(MessageFields.ATTACHMENT_IDS)) {
            final Collection<Attachment> attachments = new ArrayList<>();
            final Collection<Long> attachmentIds = (Collection<Long>) jsonObject.get(MessageFields.ATTACHMENT_IDS);

            for (final Long attachmentId : attachmentIds) {
                final Attachment attachment = new Attachment();

                attachment.setId(attachmentId);
                attachments.add(attachment);
            }
            message.setAttachments(attachments);
        }
        message.setSentTime(jsonObject.getLong(MessageFields.SENT_TIME));

        if (jsonObject.containsKey(MessageFields.DELIVERED_TIME)) {
            message.setDeliveredTime(jsonObject.getLong(MessageFields.DELIVERED_TIME));
        }

        if (jsonObject.containsKey(MessageFields.READ_TIME)) {
            message.setReadTime(jsonObject.getLong(MessageFields.READ_TIME));
        }
        message.setFailedReason(jsonObject.getNullableString(MessageFields.FAILED_REASON));
        message.setMessageFlow(Message.MessageFlow.get(jsonObject.getString(MessageFields.MESSAGE_FLOW)));
        message.setMessageState(Message.MessageState.get(jsonObject.getString(MessageFields.MESSAGE_STATE)));
        message.setId(jsonObject.getLong(MessageFields.ID));

        return message;
    }

    /**
     * {@inheritDoc}
     *
     * @param message Represents the {@link Message}
     * @return the json data
     */
    public JsonObject convert(final Message message) {
        final JsonObject messageJson = Json.object();
        final Collection<Attachment> attachments = message.getAttachments();

        messageJson.put(MessageFields.ID, message.id());
        messageJson.put(MessageFields.MESSAGE_FLOW, message.getMessageFlow().name());
        messageJson.put(MessageFields.MESSAGE_STATE, message.getMessageState().name());
        messageJson.put(MessageFields.CONTENT, message.getContent());
        messageJson.put(MessageFields.DELIVERED_TIME, message.getDeliveredTime());
        messageJson.put(MessageFields.READ_TIME, message.getReadTime());
        messageJson.put(MessageFields.SENT_TIME, message.getSentTime());
        messageJson.put(MessageFields.FAILED_REASON, message.getFailedReason());

        if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
            messageJson.put(MessageFields.ATTACHMENT_IDS, attachments.stream().map(Attachment::id).collect(Collectors.toUnmodifiableSet()));
        }

        return messageJson;
    }
}
