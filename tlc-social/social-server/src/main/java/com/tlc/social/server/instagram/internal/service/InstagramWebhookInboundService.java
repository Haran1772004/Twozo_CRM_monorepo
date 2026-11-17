package com.tlc.social.server.instagram.internal.service;

import com.tlc.attachment.validator.Group;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.instagram.model.InstagramPageRecipient;
import com.tlc.social.common.instagram.status.InstagramErrorCodes;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.server.instagram.internal.converter.InstagramChatConverter;
import com.tlc.social.server.instagram.internal.model.InstagramMessage;
import com.tlc.social.server.instagram.internal.webhook.model.InstagramReadMessageEvent;
import com.tlc.social.server.instagram.internal.webhook.model.InstagramTextMessageEvent;
import com.tlc.social.server.instagram.manager.InstagramPageManager;
import com.tlc.social.server.internal.webhook.event.model.DeliveryMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.ReadMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.TextMessageEvent;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.service.WebhookInboundService;
import com.tlc.social.server.sql.resource.MSORECIPIENT;
import com.tlc.social.server.sql.resource.MSORECIPIENTPROFILE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collections;
import java.util.Objects;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * <p>
 * Pertains to outbound actions from instagram endpoint to our app.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramWebhookInboundService implements WebhookInboundService {

    private static final String INSTAGRAM_FILE_PATH = "social/instagram/page/%s";

    private final ChatManager chatManager;
    private final InstagramPageManager instagramPageManager;
    private final InstagramService instagramService;
    private final AttachmentService attachmentService;

    private InstagramWebhookInboundService() {
        chatManager = ChatManager.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        instagramService = InstagramService.getInstance();
        attachmentService = AttachmentService.getInstance();
    }

    private static final class Instance {
        private static final InstagramWebhookInboundService INSTAGRAM_WEBHOOK_INBOUND_SERVICE = new InstagramWebhookInboundService();
    }

    /**
     * <p>
     * Gets {@link InstagramWebhookInboundService}.
     * </p>
     *
     * @return {@link InstagramWebhookInboundService}.
     */
    public static InstagramWebhookInboundService getInstance() {
        return Instance.INSTAGRAM_WEBHOOK_INBOUND_SERVICE;
    }

    /**
     * <p>
     * Gets {@link AdminDataStore}.
     * </p>
     *
     * @return {@link AdminDataStore}.
     */
    private AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * {@inheritDoc}.
     *
     * @param textMessageEvent {@link TextMessageEvent}.
     * @return {@link JsonObject}.
     */
    @Override
    public JsonObject handleTextMessageEvent(final TextMessageEvent textMessageEvent) {
        final JsonObject response = Json.object();
        final InstagramTextMessageEvent instagramTextMessageEvent = (InstagramTextMessageEvent) textMessageEvent;
        final boolean isMessageEcho = instagramTextMessageEvent.isMessageEcho();
        final DataContainer targetContainer = DataContainer.create();
        final Long externalPageId;
        final Long pageScopedId;

        if (isMessageEcho) {
            externalPageId = instagramTextMessageEvent.getSenderId();
            pageScopedId = instagramTextMessageEvent.getReceiverId();
        } else {
            externalPageId = instagramTextMessageEvent.getReceiverId();
            pageScopedId = instagramTextMessageEvent.getSenderId();
        }
        final Optional<InstagramPage> optionalInstagramPage = instagramPageManager.getPage(externalPageId);

        if (optionalInstagramPage.isPresent()) {
            final InstagramPage page = optionalInstagramPage.get();
            final Table chatTable = chatManager.getChatTable();
            final Table recipientTable = chatManager.getRecipientTable();
            final Long pageId = page.id();
            final DataContainer chatDataContainer = instagramPageManager.getChatDataContainer(pageScopedId);
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
                final Table recipientProfileTable = chatManager.getRecipientProfileTable();
                final Row recipientProfileRow = chatDataContainer.getRow(recipientProfileTable, (Long) recipientRow.get(MSORECIPIENT.PROFILE_ID));

                profileInfo = new RecipientProfile();

                if (Objects.nonNull(recipientProfileRow)) {
                    profileInfo.setId(recipientProfileRow.getPKValue());
                    profileInfo.setName(recipientProfileRow.get(MSORECIPIENTPROFILE.NAME));
                }
            }
            final Chat chat = InstagramChatConverter.getInstance().convertTextMessageEventToChat(textMessageEvent, fetchAttachments(textMessageEvent));

            chat.setChannel(page.getChannel());
            chat.setProfile(profileInfo);

            if (Objects.nonNull(chatRow)) {
                chat.setId(chatRow.getPKValue());
            }
            final Collection<String> errors = SocialEntityValidator.validate(List.of(chat), SocialValidatorGroup.InboundMessageCreate.class, Group.Upload.class);

            if (errors.isEmpty()) {
                final Collection<Attachment> attachments = chat.getLastMessage().getAttachments();

                if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
                    attachmentService.upload(attachments, INSTAGRAM_FILE_PATH);
                }
                chatManager.loadChatIntoContainer(chat, chatDataContainer, targetContainer);

                if (isNewRecipient) {
                    validateAndLoadRecipient(pageScopedId, page, chat, chatDataContainer, targetContainer, response);

                    if (response.containsKey(FBFields.ERRORS)) {
                        return response;
                    }
                }
                chatManager.loadMessageIntoContainer(chat, chatDataContainer, targetContainer);
                final InstagramMessage lastMessage = (InstagramMessage) chat.getLastMessage();

                instagramPageManager.loadInstagramMessage(lastMessage, chatDataContainer, targetContainer);
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
            throw ErrorCode.get(InstagramErrorCodes.PAGE_NOT_FOUND, "i18n.social.instagram.page.not.found");
        }
        response.put(FBFields.EVENT, FBFields.TEXT_EVENT);

        return response;
    }

    /**
     * {@inheritDoc}
     *
     * @param deliveryMessageEvent {@link DeliveryMessageEvent}.
     */
    @Override
    public JsonObject handleDeliveryMessageEvent(final DeliveryMessageEvent deliveryMessageEvent) {
        throw ErrorCode.get(SocialErrorCodes.DELIVERY_MESSAGE_EVENT_NOT_SUPPORTED, "i18n.social.chat.not.found");
    }

    /**
     * {@inheritDoc}
     *
     * @param readMessageEvent {@link ReadMessageEvent}.
     */
    @Override
    public JsonObject handleReadMessageEvent(final ReadMessageEvent readMessageEvent) {
        final InstagramReadMessageEvent instagramReadMessageEvent = (InstagramReadMessageEvent) readMessageEvent;
        final String externalMessageId = instagramReadMessageEvent.getMessageId();
        final Long deliveredTime = instagramReadMessageEvent.getTimestamp();
        final Collection<Long> messageIds = instagramPageManager.getMessageIds(List.of(externalMessageId));

        if (!messageIds.isEmpty()) {
            chatManager.updateMessageStatus(messageIds, deliveredTime, Message.MessageState.READ, null);
        }

        return Json.emptyObject();
    }

    /**
     * <p>
     * Gets the recipient profile and loads into container.
     * </p>
     *
     * @param pageId          Represents the page id.
     * @param pageScopedId    Represents the page scoped id.
     * @param dataContainer   {@link DataContainer}.
     * @param targetContainer {@link DataContainer}.
     * @param response        Represents the json response.
     * @return {@link RecipientProfile}.
     */
    private RecipientProfile getAndLoadRecipientProfile(final Long pageId, final Long pageScopedId, final DataContainer dataContainer,
                                                        final DataContainer targetContainer, final JsonObject response) {
        final RecipientProfile profileInfo = instagramService.getRecipientInfo(pageId, pageScopedId);
        final Collection<String> errors = SocialEntityValidator.validate(List.of(profileInfo), com.tlc.validator.type.Group.Create.class, Group.Upload.class);

        if (errors.isEmpty()) {
            final Attachment profilePicture = profileInfo.getProfilePicture();

            if (Objects.nonNull(profilePicture)) {
                attachmentService.upload(List.of(profilePicture), String.format(INSTAGRAM_FILE_PATH, pageId));
            }
            instagramPageManager.loadRecipientProfile(profileInfo, dataContainer, targetContainer);
        } else {
            response.put(FBFields.ERRORS, errors);
        }

        return profileInfo;
    }

    /**
     * <p>
     * Validate and loads the {@link InstagramPageRecipient}.
     * </p>
     *
     * @param pageScopedId    Represents the page scoped id.
     * @param fbPage          {@link InstagramPage}.
     * @param chat            {@link Chat}.
     * @param dataContainer   {@link DataContainer}.
     * @param targetContainer {@link DataContainer}.
     * @param response        Represents the json response.
     */
    private void validateAndLoadRecipient(final Long pageScopedId, final InstagramPage fbPage, final Chat chat, final DataContainer dataContainer,
                                          final DataContainer targetContainer, final JsonObject response) {
        final InstagramPageRecipient instagramPageRecipient = new InstagramPageRecipient();

        instagramPageRecipient.setInstagramPage(fbPage);
        instagramPageRecipient.setPageScopedId(pageScopedId);
        instagramPageRecipient.setRecipientProfile(chat.getProfile());
        instagramPageRecipient.setChatId(chat.id());
        final Collection<String> errors = SocialEntityValidator.validate(List.of(instagramPageRecipient), SocialValidatorGroup.InboundMessageCreate.class);

        if (errors.isEmpty()) {
            instagramPageManager.loadRecipientIntoContainer(instagramPageRecipient, dataContainer, targetContainer);
        } else {
            response.put(FBFields.ERRORS, errors);
        }
    }

    /**
     * <p>
     * Fetches the collection of {@link Attachment} from the url.
     * </p>
     *
     * @param textMessageEvent Represents the {@link InstagramTextMessageEvent}.
     * @return the {@link Attachment}.
     */
    private Collection<Attachment> fetchAttachments(final TextMessageEvent textMessageEvent) {
        final Collection<InstagramTextMessageEvent.Attachment> textMessageEventAttachments = ((InstagramTextMessageEvent) textMessageEvent).getAttachments();

        if (Objects.nonNull(textMessageEventAttachments) && !textMessageEventAttachments.isEmpty()) {
            final Collection<Attachment> attachments = new ArrayList<>();

            textMessageEventAttachments.stream().map(attachment -> attachment.payload().url())
                    .forEach(url -> {
                        if (Objects.nonNull(url)) {
                            attachments.add(instagramService.fetchAttachment(url, Collections.emptyMap(), Collections.emptyMap()));
                        }
                    });

            return attachments;
        }

        return null;
    }
}
