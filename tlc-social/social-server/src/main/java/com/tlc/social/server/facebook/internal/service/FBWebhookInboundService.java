package com.tlc.social.server.facebook.internal.service;

import com.tlc.attachment.validator.Group;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.file.common.models.Attachment;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.internal.status.FBErrorCodes;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.common.facebook.model.FBPageRecipient;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.server.facebook.internal.webhook.model.FBDeliveryMessageEvent;
import com.tlc.social.server.facebook.internal.webhook.model.FBReadMessageEvent;
import com.tlc.social.server.facebook.internal.webhook.model.FBTextMessageEvent;
import com.tlc.social.server.facebook.internal.model.FBMessage;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.server.facebook.internal.converter.FBChatConverter;
import com.tlc.social.server.facebook.manager.FBPageManager;
import com.tlc.social.server.internal.webhook.event.model.DeliveryMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.ReadMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.TextMessageEvent;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.service.WebhookInboundService;
import com.tlc.social.server.sql.resource.MSORECIPIENT;

import com.tlc.file.client.service.AttachmentService;

import com.tlc.social.server.sql.resource.MSORECIPIENTPROFILE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides inbound services to handle the facebook webhook events
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBWebhookInboundService implements WebhookInboundService {

    private static final String FB_FILE_PATH = "social/facebook/page/%s";

    private final ChatManager chatManager;
    private final FBPageManager fbPageManager;
    private final FBService fbService;
    private AttachmentService attachmentService;

    private FBWebhookInboundService() {
        chatManager = ChatManager.getInstance();
        fbPageManager = FBPageManager.getInstance();
        fbService = FBService.getInstance();
        attachmentService = AttachmentService.getInstance();
    }

    private static final class InstanceHolder {
        private static final FBWebhookInboundService FB_WEBHOOK_INBOUND_SERVICE = new FBWebhookInboundService();
    }

    /**
     * <p>
     * Gets the instance of {@link FBWebhookInboundService}
     * </p>
     *
     * @return the instance of {@link FBWebhookInboundService}
     */
    public static FBWebhookInboundService getInstance() {
        return InstanceHolder.FB_WEBHOOK_INBOUND_SERVICE;
    }

    /**
     * <p>
     * Gets the {@link AdminDataStore}
     * </p>
     *
     * @return the {@link AdminDataStore}
     */
    private AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * {@inheritDoc}
     *
     * @param textMessageEvent Represents the {@link TextMessageEvent}
     * @return the response
     */
    @Override
    public JsonObject handleTextMessageEvent(final TextMessageEvent textMessageEvent) {
        final FBTextMessageEvent fbTextMessageEvent = (FBTextMessageEvent) textMessageEvent;
        final JsonObject response = Json.object();
        final boolean isMessageEcho = fbTextMessageEvent.isMessageEcho();
        final DataContainer targetContainer = DataContainer.create();
        final Long externalPageId;
        final Long pageScopedId;

        if (isMessageEcho) {
            externalPageId = fbTextMessageEvent.getSenderId();
            pageScopedId = fbTextMessageEvent.getReceiverId();
        } else {
            externalPageId = fbTextMessageEvent.getReceiverId();
            pageScopedId = fbTextMessageEvent.getSenderId();
        }
        final Optional<FBPage> optionalFBPage = fbPageManager.getPage(externalPageId);

        if (optionalFBPage.isPresent()) {
            final FBPage page = optionalFBPage.get();
            final Table chatTable = chatManager.getChatTable();
            final Table recipientTable = chatManager.getRecipientTable();
            final Table recipientProfileTable = chatManager.getRecipientProfileTable();
            final Long pageId = page.id();
            final DataContainer chatDataContainer = fbPageManager.getChatDataContainer(pageScopedId);
            final Row recipientRow = chatDataContainer.getRow(recipientTable);
            final Row chatRow = chatDataContainer.getRow(chatTable);
            final RecipientProfile profileInfo;
            final boolean isNewRecipient = Objects.isNull(recipientRow);

            if (isNewRecipient) {
                profileInfo = getAndLoadRecipientProfile(pageId, pageScopedId, chatDataContainer, targetContainer, response);

                if (response.containsKey(FBFields.ERRORS)) {
                    return response;
                }
            } else {
                final Row recipientProfileRow = chatDataContainer.getRow(recipientProfileTable, (Long) recipientRow.get(MSORECIPIENT.PROFILE_ID));
                profileInfo = new RecipientProfile();

                if (Objects.nonNull(recipientProfileRow)) {
                    profileInfo.setId(recipientProfileRow.getPKValue());
                    profileInfo.setName(recipientProfileRow.get(MSORECIPIENTPROFILE.NAME));
                }
            }
            final Chat chat = FBChatConverter.getInstance().convertTextMessageEventToChat(fbTextMessageEvent, fetchAttachments(fbTextMessageEvent));

            chat.setChannel(page.getChannel());
            chat.setProfile(profileInfo);

            if (Objects.nonNull(chatRow)) {
                chat.setId(chatRow.getPKValue());
            }
            final Collection<String> errors = SocialEntityValidator.validate(List.of(chat), SocialValidatorGroup.InboundMessageCreate.class, Group.Upload.class);

            if (errors.isEmpty()) {
                final Collection<Attachment> attachments = chat.getLastMessage().getAttachments();

                if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
                    attachmentService.upload(attachments, FB_FILE_PATH);
                }
                chatManager.loadChatIntoContainer(chat, chatDataContainer, targetContainer);

                if (isNewRecipient) {
                    validateAndLoadRecipient(pageScopedId, page, chat, chatDataContainer, targetContainer, response);

                    if (response.containsKey(FBFields.ERRORS)) {
                        return response;
                    }
                }
                chatManager.loadMessageIntoContainer(chat, chatDataContainer, targetContainer);
                final FBMessage lastMessage = (FBMessage) chat.getLastMessage();

                fbPageManager.loadFBMessage(lastMessage, chatDataContainer, targetContainer);
                adminDataStore().commitChanges(targetContainer);
                final JsonObject recipientJson = Json.object();

                recipientJson.put(MessageFields.ID, profileInfo.id());
                recipientJson.put(MessageFields.NAME, profileInfo.getName());
                response.put(MessageFields.RECIPIENT, recipientJson);
                response.put(MessageFields.CHAT_ID, chat.id());
                response.put(MessageFields.MESSAGE_ID, lastMessage.id());
                response.put(MessageFields.PAGE_ID, pageId);
                response.put(MessageFields.IS_MESSAGE_ECHO, isMessageEcho);
                response.put(MessageFields.SENT_TIME, lastMessage.getSentTime());
                response.put(MessageFields.MESSAGE_PREVIEW, chat.getRecentMessagePreview());
                response.put(MessageFields.UNREAD_COUNT, chat.getUnreadCount());
            } else {
                response.put(MessageFields.ERRORS, errors);
            }
        } else {
            throw ErrorCode.get(FBErrorCodes.PAGE_NOT_FOUND, "i18n.social.facebook.page.not.found");
        }
        response.put(FBFields.EVENT, FBFields.TEXT_EVENT);

        return response;
    }

    /**
     * {@inheritDoc}
     *
     * @param deliveryMessageEvent Represents the {@link DeliveryMessageEvent}
     */
    @Override
    public JsonObject handleDeliveryMessageEvent(final DeliveryMessageEvent deliveryMessageEvent) {
        final FBDeliveryMessageEvent fbDeliveryMessageEvent = (FBDeliveryMessageEvent) deliveryMessageEvent;
        final Long deliveredTime = fbDeliveryMessageEvent.getWatermark();
        final Collection<String> externalMessageIds = fbDeliveryMessageEvent.getMessageIds();
        final Collection<Long> messageIds = fbPageManager.getMessageIds(externalMessageIds);

        if (!messageIds.isEmpty()) {
            chatManager.updateMessageStatus(messageIds, deliveredTime, Message.MessageState.DELIVERED, null);
        }

        return Json.emptyObject();
    }

    /**
     * {@inheritDoc}
     *
     * @param readMessageEvent Represents the {@link ReadMessageEvent}
     */
    @Override
    public JsonObject handleReadMessageEvent(final ReadMessageEvent readMessageEvent) {
        final FBReadMessageEvent fbReadMessageEvent = (FBReadMessageEvent) readMessageEvent;
        final Long pageScopedId = fbReadMessageEvent.getSenderId();
        final Long watermark = fbReadMessageEvent.getWatermark();
        final Optional<Long> optionalChatId = fbPageManager.getChatId(pageScopedId);

        if (optionalChatId.isPresent()) {
            final Long chatId = optionalChatId.get();
            final Collection<Long> messageIds = fbPageManager.fetchUnReadOutboundMessageIds(chatId, watermark);

            if (!messageIds.isEmpty()) {
                chatManager.updateMessageStatus(messageIds, watermark, Message.MessageState.READ, null);
            }
        } else {
            throw ErrorCode.get(SocialErrorCodes.CHAT_NOT_FOUND, "i18n.social.chat.not.found");
        }

        return Json.emptyObject();
    }

    /**
     * <p>
     * Gets the recipient profile and loads into container
     * </p>
     *
     * @param pageId          Represents the page id
     * @param pageScopedId    Represents the page scoped id
     * @param dataContainer   Represents the data container
     * @param targetContainer Represents the target container
     * @param response        Represents the json response
     * @return the {@link RecipientProfile}
     */
    private RecipientProfile getAndLoadRecipientProfile(final Long pageId, final Long pageScopedId, final DataContainer dataContainer,
                                                        final DataContainer targetContainer, final JsonObject response) {
        final RecipientProfile profileInfo = fbService.getRecipientInfo(pageId, pageScopedId);
        final Collection<String> errors = SocialEntityValidator.validate(List.of(profileInfo), com.tlc.validator.type.Group.Create.class, Group.Upload.class);

        if (errors.isEmpty()) {
            attachmentService.upload(List.of(profileInfo.getProfilePicture()), String.format(FB_FILE_PATH, pageId));
            fbPageManager.loadRecipientProfile(profileInfo, dataContainer, targetContainer);
        } else {
            response.put(FBFields.ERRORS, errors);
        }

        return profileInfo;
    }

    /**
     * <p>
     * Validate and loads the {@link FBPageRecipient}
     * </p>
     *
     * @param pageScopedId    Represents the page scoped id
     * @param fbPage          Represents the {@link FBPage}
     * @param chat            Represents the {@link Chat}
     * @param dataContainer   Represents the data container
     * @param targetContainer Represents the target container
     * @param response        Represents the json response
     */
    private void validateAndLoadRecipient(final Long pageScopedId, final FBPage fbPage, final Chat chat, final DataContainer dataContainer,
                                          final DataContainer targetContainer, final JsonObject response) {
        final FBPageRecipient fbPageRecipient = new FBPageRecipient();

        fbPageRecipient.setFbPage(fbPage);
        fbPageRecipient.setPageScopedId(pageScopedId);
        fbPageRecipient.setRecipientProfile(chat.getProfile());
        fbPageRecipient.setChatId(chat.id());
        final Collection<String> errors = SocialEntityValidator.validate(List.of(fbPageRecipient), SocialValidatorGroup.InboundMessageCreate.class);

        if (errors.isEmpty()) {
            fbPageManager.loadRecipientIntoContainer(fbPageRecipient, dataContainer, targetContainer);
        } else {
            response.put(FBFields.ERRORS, errors);
        }
    }

    /**
     * <p>
     * Fetches the collection of {@link Attachment} from the url
     * </p>
     *
     * @param textMessageEvent Represents the {@link FBTextMessageEvent}
     * @return the {@link Attachment}
     */
    private Collection<Attachment> fetchAttachments(final FBTextMessageEvent textMessageEvent) {
        final Collection<FBTextMessageEvent.Attachment> textMessageEventAttachments = textMessageEvent.getAttachments();

        if (Objects.nonNull(textMessageEventAttachments) && !textMessageEventAttachments.isEmpty()) {
            final Collection<Attachment> attachments = new ArrayList<>();

            textMessageEventAttachments.stream().map(attachment -> attachment.payload().url())
                    .forEach(url -> {
                        if (Objects.nonNull(url)) {
                            attachments.add(fbService.fetchAttachment(url, Collections.emptyMap(), Collections.emptyMap()));
                        }
                    });

            return attachments;
        }

        return null;
    }
}
