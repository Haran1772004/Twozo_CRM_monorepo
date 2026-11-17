package com.tlc.crm.messenger.api.conversation.model.converter;

import com.tlc.attachment.io.Extension;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;

import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivity;
import com.tlc.crm.messenger.api.conversation.model.CrmMessage;
import com.tlc.crm.messenger.api.conversation.model.MessageType;
import com.tlc.crm.messenger.converter.MessengerConverter;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.resolver.PlaceHolderValueResolver;
import com.tlc.file.common.models.Attachment;
import com.tlc.iam.common.models.User;
import com.tlc.social.common.model.Message;
import com.tlc.web.HttpUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Converts conversation related models.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ConversationConverter implements MessengerConverter<Message, CrmMessage> {

    private final PlaceHolderValueResolver placeHolderValueResolver;
    private final String attachmentRedirectionUrl;

    private ConversationConverter() {
        placeHolderValueResolver = PlaceHolderValueResolver.getInstance();
        attachmentRedirectionUrl = ConfHolder.getInstance().getProperties(CommonFields.CRM_COMMON_CONFIG_KEY).getConfig(CommonFields.FILE_PREVIEW_REDIRECT_URL);
    }

    private static final class Instance {
        private static final ConversationConverter CONVERSATION_CONVERTER = new ConversationConverter();
    }

    /**
     * <p>
     * Gets {@link ConversationConverter}.
     * </p>
     *
     * @return {@link ConversationConverter}.
     */
    public static ConversationConverter getInstance() {
        return Instance.CONVERSATION_CONVERTER;
    }

    /**
     * <p>
     * Converts message info to {@link JsonObject}.
     * </p>
     *
     * @param message    {@link Message}.
     * @param crmMessage {@link CrmMessage}.
     * @return message info to {@link JsonObject}.
     */
    @Override
    public JsonObject convert(final Message message, final CrmMessage crmMessage) {
        final JsonObject conversationJson = Json.object();
        final Message.MessageFlow messageFlow = message.getMessageFlow();
        final Collection<Attachment> attachments = message.getAttachments();
        final CrmMessage.OutboundMessageSource outboundMessageSource = crmMessage.getOutboundMessageSource();

        conversationJson.put(MessengerFields.ID, crmMessage.getCrmId());
        conversationJson.put(MessengerFields.TYPE, MessageType.MESSAGE.getName());
        conversationJson.put(MessengerFields.FLOW, messageFlow.getName());
        conversationJson.put(MessengerFields.TIME_STAMP, message.getSentTime());
        conversationJson.put(MessengerFields.MESSAGE, message.getContent());

        if (Message.MessageFlow.OUTBOUND == messageFlow) {
            conversationJson.put(MessengerFields.STATUS, message.getMessageState().getName());
            conversationJson.put(MessengerFields.FAILED_REASON, message.getFailedReason());
            conversationJson.put(MessengerFields.USER_ID, crmMessage.getUserId());
        }

        if (Objects.nonNull(outboundMessageSource)) {
            conversationJson.put(MessengerFields.OUTBOUND_MESSAGE_SOURCE, outboundMessageSource.getId());
        }

        if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
            final JsonArray attachmentArray = Json.array();

            for (final Attachment attachment : attachments) {
                final JsonObject attachmentJson = Json.object();
                final Long attachmentId = attachment.id();
                final String extension = attachment.getExtension();

                attachmentJson.put(MessengerFields.ID, attachmentId);
                attachmentJson.put(MessengerFields.FILE_NAME, attachment.getFileName());
                attachmentJson.put(MessengerFields.SIZE, attachment.getFileSize());
                attachmentJson.put(MessengerFields.EXTENSION, extension);
                attachmentJson.put(MessengerFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s", attachmentRedirectionUrl, attachmentId)));
                attachmentJson.put(MessengerFields.TYPE, Extension.getMediaType(extension));
                attachmentArray.put(attachmentJson);
            }
            conversationJson.put(MessengerFields.ATTACHMENTS, attachmentArray);
        }
        return conversationJson;
    }

    /**
     * <p>
     * Converts {@link ConversationActivity} to {@link JsonObject}.
     * </p>
     *
     * @param conversationActivity {@link ConversationActivity}.
     * @return {@link ConversationActivity} to {@link JsonObject}.
     */
    public JsonObject convertActivity(final ConversationActivity conversationActivity, final Map<Long, User> userMap) {
        final JsonObject activityJson = Json.object();

        activityJson.put(MessengerFields.ID, conversationActivity.getId());
        activityJson.put(MessengerFields.TYPE, MessageType.ACTIVITY.getName());
        activityJson.put(MessengerFields.TIME_STAMP, conversationActivity.getTimeStamp());
        activityJson.put(MessengerFields.MESSAGE, placeHolderValueResolver.resolve(conversationActivity.getConversationActivityTemplate(),
                conversationActivity.getPlaceHolderValue(), userMap));

        return activityJson;
    }
}