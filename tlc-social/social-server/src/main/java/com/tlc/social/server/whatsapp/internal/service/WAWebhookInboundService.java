package com.tlc.social.server.whatsapp.internal.service;

import com.tlc.attachment.validator.Group;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.common.whatsapp.model.WAPhoneRecipient;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.internal.webhook.event.model.FailedMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.TextMessageEvent;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.internal.webhook.event.model.DeliveryMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.ReadMessageEvent;
import com.tlc.social.server.service.WebhookInboundService;
import com.tlc.social.server.sql.resource.MSORECIPIENT;
import com.tlc.social.server.sql.resource.MSORECIPIENTPROFILE;
import com.tlc.social.server.whatsapp.internal.webhook.converter.WAChatConverter;
import com.tlc.social.server.whatsapp.internal.model.WAMessage;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAAccountUpdateEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WADeliveryMessageEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAFailedMessageEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAReadMessageEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WATemplateStatusUpdateEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WATextMessageEvent;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides inbound services to handle the WhatsApp webhook events
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAWebhookInboundService implements WebhookInboundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAWebhookInboundService.class);
    private static final String WA_FILE_PATH = "social/whatsapp/business/%s";
    private static final int CHAT_BATCH_SIZE = 10;

    private final ChatManager chatManager;
    private final WABusinessManager waBusinessManager;
    private final WAService waService;
    private final WAChatConverter waChatConverter;
    private final AttachmentService attachmentService;

    private WAWebhookInboundService() {
        chatManager = ChatManager.getInstance();
        waBusinessManager = WABusinessManager.getInstance();
        waService = WAService.getInstance();
        attachmentService = AttachmentService.getInstance();
        waChatConverter = WAChatConverter.getInstance();
    }

    /**
     * Holds the instance of {@link WAWebhookInboundService}
     */
    private static final class InstanceHolder {
        private static final WAWebhookInboundService WA_WEBHOOK_INBOUND_SERVICE = new WAWebhookInboundService();
    }

    /**
     * <p>
     * Gets the instance of {@link WAWebhookInboundService}
     * </p>
     *
     * @return the instance of {@link WAWebhookInboundService}
     */
    public static WAWebhookInboundService getInstance() {
        return InstanceHolder.WA_WEBHOOK_INBOUND_SERVICE;
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
     * @param textMessageEvent Represents the {@link WATextMessageEvent}
     * @return the response
     */
    @Override
    public JsonObject handleTextMessageEvent(final TextMessageEvent textMessageEvent) {
        final WATextMessageEvent waTextMessageEvent = (WATextMessageEvent) textMessageEvent;
        final JsonObject response = Json.object();
        final DataContainer targetContainer = DataContainer.create();
        final Long externalBusinessPhoneNumberId = waTextMessageEvent.getReceiverId();
        final String recipientProfileName = waTextMessageEvent.getSenderProfileName();
        final Long recipientWAId = waTextMessageEvent.getSenderId();
        final Optional<WABusinessPhoneNumber> optionalWABusinessPhoneNumber = waBusinessManager.getWABusinessPhoneNumber(externalBusinessPhoneNumberId);

        if (optionalWABusinessPhoneNumber.isPresent()) {
            final WABusinessPhoneNumber waBusinessPhoneNumber = optionalWABusinessPhoneNumber.get();
            final Table chatTable = chatManager.getChatTable();
            final Table recipientTable = chatManager.getRecipientTable();
            final Table recipientProfileTable = chatManager.getRecipientProfileTable();
            final Long businessPhoneNumberId = waBusinessPhoneNumber.id();
            final DataContainer chatDataContainer = waBusinessManager.getChatDataContainer(businessPhoneNumberId, recipientWAId);
            final Row recipientRow = chatDataContainer.getRow(recipientTable);
            final Row chatRow = chatDataContainer.getRow(chatTable);
            final boolean isNewRecipient = Objects.isNull(recipientRow);
            final RecipientProfile profileInfo = new RecipientProfile();

            if (isNewRecipient) {
                profileInfo.setName(recipientProfileName);

                waBusinessManager.loadRecipientProfile(profileInfo, chatDataContainer, targetContainer);
            } else {
                final Row recipientProfileRow = chatDataContainer.getRow(recipientProfileTable, (Long) recipientRow.get(MSORECIPIENT.PROFILE_ID));

                if (Objects.nonNull(recipientProfileRow)) {
                    profileInfo.setId(recipientProfileRow.getPKValue());
                    profileInfo.setName(recipientProfileName);
                    recipientProfileRow.set(MSORECIPIENTPROFILE.NAME, recipientProfileName);

                    targetContainer.updateRow(recipientProfileRow);
                }
            }
            final Chat chat = waChatConverter.convertTextMessageEventToChat(waTextMessageEvent, fetchAttachment(waBusinessPhoneNumber.getWaBusinessAccountId(), waTextMessageEvent));

            chat.setChannel(waBusinessPhoneNumber.getChannel());
            chat.setProfile(profileInfo);

            if (Objects.nonNull(chatRow)) {
                chat.setId(chatRow.getPKValue());
            }
            final Collection<String> errors = SocialEntityValidator.validate(List.of(chat), SocialValidatorGroup.InboundMessageCreate.class, Group.Upload.class);

            if (errors.isEmpty()) {
                final Collection<Attachment> attachments = chat.getLastMessage().getAttachments();

                if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
                    attachmentService.upload(attachments, WA_FILE_PATH);
                }
                chatManager.loadChatIntoContainer(chat, chatDataContainer, targetContainer);

                if (isNewRecipient) {
                    validateAndLoadRecipient(recipientWAId, waBusinessPhoneNumber, chat, chatDataContainer, targetContainer, response);

                    if (response.containsKey(WAFields.ERRORS)) {
                        return response;
                    }
                }
                chatManager.loadMessageIntoContainer(chat, chatDataContainer, targetContainer);
                final WAMessage lastMessage = (WAMessage) chat.getLastMessage();

                waBusinessManager.loadWAMessage(lastMessage, chatDataContainer, targetContainer);
                adminDataStore().commitChanges(targetContainer);
                final JsonObject recipientJson = Json.object();

                recipientJson.put(MessageFields.ID, profileInfo.id());
                recipientJson.put(MessageFields.NAME, profileInfo.getName());
                response.put(MessageFields.RECIPIENT, recipientJson);
                response.put(MessageFields.CHAT_ID, chat.id());
                response.put(MessageFields.MESSAGE_ID, lastMessage.id());
                response.put(MessageFields.BUSINESS_PHONE_NUMBER_ID, businessPhoneNumberId);
                response.put(MessageFields.SENT_TIME, lastMessage.getSentTime());
                response.put(MessageFields.MESSAGE_PREVIEW, chat.getRecentMessagePreview());
                response.put(MessageFields.UNREAD_COUNT, chat.getUnreadCount());
            } else {
                response.put(MessageFields.ERRORS, errors);
            }

        } else {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.social.whatsapp.business.phone.not.found");
        }
        response.put(WAFields.EVENT, WAFields.TEXT_EVENT);

        return response;
    }

    /**
     * {@inheritDoc}
     *
     * @param deliveryMessageEvent Represents the {@link DeliveryMessageEvent}
     */
    @Override
    public JsonObject handleDeliveryMessageEvent(final DeliveryMessageEvent deliveryMessageEvent) {
        final JsonObject response = Json.object();
        final WADeliveryMessageEvent waDeliveryMessageEvent = (WADeliveryMessageEvent) deliveryMessageEvent;
        final String externalMessageId = waDeliveryMessageEvent.getMessageId();
        final Long deliveredTime = waDeliveryMessageEvent.getTimestamp();
        final Collection<Long> messageIds = waBusinessManager.getMessageIds(List.of(externalMessageId));

        if (!messageIds.isEmpty()) {
            chatManager.updateMessageStatus(messageIds, deliveredTime, Message.MessageState.DELIVERED, null);
            response.put(MessageFields.MESSAGE_IDS, messageIds);
            response.put(WAFields.EVENT, WAFields.DELIVERY_EVENT);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     *
     * @param readMessageEvent Represents the {@link ReadMessageEvent}
     */
    @Override
    public JsonObject handleReadMessageEvent(final ReadMessageEvent readMessageEvent) {
        final JsonObject response = Json.object();
        final WAReadMessageEvent waReadMessageEvent = (WAReadMessageEvent) readMessageEvent;
        final String externalMessageId = waReadMessageEvent.getMessageId();
        final Long deliveredTime = waReadMessageEvent.getTimestamp();
        final Collection<Long> messageIds = waBusinessManager.getMessageIds(List.of(externalMessageId));

        if (!messageIds.isEmpty()) {
            chatManager.updateMessageStatus(messageIds, deliveredTime, Message.MessageState.READ, null);
            response.put(MessageFields.MESSAGE_IDS, messageIds);
            response.put(WAFields.EVENT, WAFields.READ_EVENT);
        }

        return response;
    }

    /**
     * <p>
     * Handles the whatsapp webhook failed message event
     * </p>
     *
     * @param failedMessageEvent Represents the {@link FailedMessageEvent}
     */
    public JsonObject handleFailedMessageEvent(final FailedMessageEvent failedMessageEvent) {
        final JsonObject response = Json.object();
        final WAFailedMessageEvent waFailedMessageEvent = (WAFailedMessageEvent) failedMessageEvent;
        final String externalMessageId = waFailedMessageEvent.getMessageId();
        final String failedReason = waFailedMessageEvent.getFailedReason();
        final Collection<Long> messageIds = waBusinessManager.getMessageIds(List.of(externalMessageId));

        if (!messageIds.isEmpty()) {
            chatManager.updateMessageStatus(messageIds, null, Message.MessageState.FAILED, failedReason);
            response.put(MessageFields.MESSAGE_IDS, messageIds);
            response.put(WAFields.EVENT, WAFields.MESSAGE_FAILURE_EVENT);
        }

        return response;
    }

    /**
     * <p>
     * Handles the Whatsapp template status update event
     * </p>
     *
     * @param waTemplateStatusUpdateEvent Represents the {@link WATemplateStatusUpdateEvent}
     */
    public void handleTemplateStatusUpdateEvent(final WATemplateStatusUpdateEvent waTemplateStatusUpdateEvent) {
        final Optional<WABusinessAccount> optionalWABusinessAccount = waBusinessManager.getWABusinessAccount(waTemplateStatusUpdateEvent.getExternalWABusinessAccountId());

        if (optionalWABusinessAccount.isPresent()) {
            final Long externalTemplateId = waTemplateStatusUpdateEvent.getExternalTemplateId();
            final WATemplate.Status status = waTemplateStatusUpdateEvent.getStatus();
            final boolean isApproved = Objects.equals(WATemplate.Status.APPROVED, status);
            final WABusinessAccount waBusinessAccount = optionalWABusinessAccount.get();
            final Long waBusinessAccountId = waBusinessAccount.id();

            if (waBusinessManager.isTemplateExists(externalTemplateId, waBusinessAccountId)) {
                if (status == WATemplate.Status.APPROVED) {
                    final WATemplate waTemplate = waService.getTemplate(waBusinessAccount, externalTemplateId);
                    final Collection<String> errors = SocialEntityValidator.validate(List.of(waTemplate), com.tlc.validator.type.Group.Update.class);

                    if (errors.isEmpty()) {
                        waBusinessManager.updateTemplate(waBusinessAccount, waTemplate);
                    } else {
                        LOGGER.error("Whatsapp: Failed to update the template. Data validation failed. WABA ID:{}, External template Id:{}, Error:{}", waBusinessAccount.id(), externalTemplateId, errors.stream().findFirst().get());
                    }
                } else {
                    waBusinessManager.updateTemplateStatus(waBusinessAccountId, externalTemplateId, status);
                }
            } else if (isApproved) {
                final WATemplate waTemplate = waService.getTemplate(waBusinessAccount, externalTemplateId);
                final Collection<String> errors = SocialEntityValidator.validate(List.of(waTemplate), com.tlc.validator.type.Group.Create.class);

                if (errors.isEmpty()) {
                    waBusinessManager.storeTemplates(waBusinessAccount, List.of(waTemplate));
                } else {
                    LOGGER.error("Whatsapp: Failed to store template. Data validation failed. WABA ID:{}, External template Id:{}, Error:{}", waBusinessAccount.id(), externalTemplateId, errors.stream().findFirst().get());
                }
            }
        } else {
            throw ErrorCode.get(WAErrorCodes.WA_BUSINESS_ACCOUNT_NOT_FOUND, "i18n.social.whatsapp.business.account.not.found");
        }
    }

    /**
     * <p>
     * Handles the Whatsapp account update event
     * </p>
     *
     * @param waAccountUpdateEvent Represents the {@link WAAccountUpdateEvent}
     */
    public JsonObject handleAccountUpdateEvent(final WAAccountUpdateEvent waAccountUpdateEvent) {
        final JsonObject response = Json.object();
        final String displayPhoneNumber = waAccountUpdateEvent.getDisplayPhoneNumber();
        final Long externalWABAId = waAccountUpdateEvent.getExternalWABusinessAccountId();
        final Long deletedPhoneNumberId = waBusinessManager.deletePhoneNumber(externalWABAId, displayPhoneNumber);

        if (Objects.nonNull(deletedPhoneNumberId)) {
            deleteExistingMessages(deletedPhoneNumberId);
            response.put(WAFields.EVENT, WAFields.PHONE_NUMBER_DELETION_EVENT);
            response.put(WAFields.BUSINESS_PHONE_NUMBER_ID, deletedPhoneNumberId);

            return response;
        } else {
            LOGGER.error("Whatsapp: Failed to retrieve business phone number. External WABA Id:{}, Display phone number:{}", externalWABAId, displayPhoneNumber);
            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.social.whatsapp.business.phone.not.found");
        }
    }

    /**
     * <p>
     * Deletes the existing messages for the deleted phone number
     * </p>
     *
     * @param deletedPhoneNumberId Represents the whatsapp phone number id
     */
    private void deleteExistingMessages(final Long deletedPhoneNumberId) {
        final long totalChatCount = waBusinessManager.getChatCount(deletedPhoneNumberId);
        int start = 1;

        while (start <= totalChatCount) {
            waBusinessManager.markMessagesAsDeleted(waBusinessManager.getChatIds(deletedPhoneNumberId, start, CHAT_BATCH_SIZE));
            start += CHAT_BATCH_SIZE;
        }
    }

    /**
     * <p>
     * Validate and loads the {@link WAPhoneRecipient}
     * </p>
     *
     * @param recipientWAId         Represents the whatsapp id of the recipient
     * @param waBusinessPhoneNumber Represents the {@link WABusinessPhoneNumber}
     * @param chat                  Represents the {@link Chat}
     * @param dataContainer         Represents the data container
     * @param targetContainer       Represents the target container
     * @param response              Represents the json response
     */
    private void validateAndLoadRecipient(final Long recipientWAId, final WABusinessPhoneNumber waBusinessPhoneNumber, final Chat chat, final DataContainer dataContainer,
                                          final DataContainer targetContainer, final JsonObject response) {
        final WAPhoneRecipient waPhoneRecipient = new WAPhoneRecipient();

        waPhoneRecipient.setWaBusinessPhoneNumber(waBusinessPhoneNumber);
        waPhoneRecipient.setExternalRecipientId(recipientWAId);
        waPhoneRecipient.setRecipientProfile(chat.getProfile());
        waPhoneRecipient.setChatId(chat.id());
        final Collection<String> errors = SocialEntityValidator.validate(List.of(waPhoneRecipient), SocialValidatorGroup.InboundMessageCreate.class);

        if (errors.isEmpty()) {
            waBusinessManager.loadRecipientIntoContainer(waPhoneRecipient, dataContainer, targetContainer);
        } else {
            response.put(WAFields.ERRORS, errors);
        }
    }

    /**
     * <p>
     * Fetches the {@link Attachment} from the provided {@link WATextMessageEvent}
     * </p>
     *
     * @param textMessageEvent Represents the {@link WATextMessageEvent}
     * @return the {@link Attachment}
     */
    private Attachment fetchAttachment(final Long businessAccountId, final WATextMessageEvent textMessageEvent) {
        final WATextMessageEvent.Attachment textMessageEventAttachment = textMessageEvent.getAttachment();

        if (Objects.nonNull(textMessageEventAttachment)) {
            return waService.fetchAttachment(businessAccountId, textMessageEventAttachment.id(), textMessageEventAttachment.mimeType());
        }

        return null;
    }
}
