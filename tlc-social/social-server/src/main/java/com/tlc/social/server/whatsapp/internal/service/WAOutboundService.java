package com.tlc.social.server.whatsapp.internal.service;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.file.common.models.Attachment;

import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.common.whatsapp.model.WAPhoneRecipient;
import com.tlc.social.server.model.MessageSendRequest;
import com.tlc.social.server.service.SocialOutboundService;
import com.tlc.social.server.whatsapp.internal.model.WAMessage;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A service implementation of {@link SocialOutboundService} which provides WhatsApp related outbound services
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAOutboundService extends SocialOutboundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAOutboundService.class);

    private static final String NON_DIGIT_REGEX_PATTERN = "[^\\d]";

    private final WAService waService;
    private final WABusinessManager waBusinessManager;

    private WAOutboundService() {
        waService = WAService.getInstance();
        waBusinessManager = WABusinessManager.getInstance();
    }

    /**
     * Holds the instance of {@link WAOutboundService}
     */
    private static final class InstanceHolder {
        private static final WAOutboundService WA_OUTBOUND_SERVICE= new WAOutboundService();
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
     * <p>
     * Gets the instance of {@link WAOutboundService}
     * </p>
     *
     * @return the instance of {@link WAOutboundService}
     */
    public static WAOutboundService getInstance() {
        return InstanceHolder.WA_OUTBOUND_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param messageSendRequest Represents the {@link MessageSendRequest}
     * @return the {@link Message}
     */
    @Override
    public JsonObject send(final MessageSendRequest messageSendRequest) {

        if (!messageSendRequest.isNewChat() && Objects.nonNull(messageSendRequest.getChatId())) {
            return sendAndReturnResponse(messageSendRequest, null);
        } else {
            final Collection<String> recipientPhoneNumbers = messageSendRequest.getRecipientPhoneNumbers();
            final int recipientCount = recipientPhoneNumbers.size();
            final JsonArray responseArray = Json.array();

            for (final String recipientPhoneNumber : recipientPhoneNumbers) {
                final JsonObject sendResponse = sendAndReturnResponse(messageSendRequest, recipientPhoneNumber);

                if (NumericalConstant.ONE < recipientCount) {
                    responseArray.put(sendResponse);
                } else {
                    return sendResponse;
                }
            }

            return Json.object().put(ChatFields.LIST, responseArray);
        }
    }

    /**
     * <p>
     * Sends the message and return response
     * </p>
     *
     * @param messageSendRequest   Represents the {@link MessageSendRequest}
     * @param recipientPhoneNumber Represents the recipient phone number
     * @return the response
     */
    private JsonObject sendAndReturnResponse(final MessageSendRequest messageSendRequest, final String recipientPhoneNumber) {
        final JsonObject sendResponse = Json.object();
        final DataContainer targetContainer = DataContainer.create();
        final JsonArray messageArray = Json.array();
        final WAPhoneRecipient waPhoneRecipient;
        final Chat chat = new Chat();
        final DataContainer dataContainer;

        if (messageSendRequest.isNewChat() || Objects.isNull(messageSendRequest.getChatId())) {
            final Table chatTable = chatManager.getChatTable();
            final Long businessPhoneNumberId = messageSendRequest.getSenderId();
            final Long waExternalRecipientId = Long.parseLong(recipientPhoneNumber.replaceAll(NON_DIGIT_REGEX_PATTERN, ""));
            dataContainer = waBusinessManager.getChatDataContainer(businessPhoneNumberId, waExternalRecipientId);
            final Row chatRow = dataContainer.getRow(chatTable);
            final String businessDisplayPhoneNumber;

            if (Objects.isNull(chatRow)) {
                final Optional<WABusinessPhoneNumber> optionalWABusinessPhoneNumber = waBusinessManager.getBusinessPhoneNumber(businessPhoneNumberId);
                waPhoneRecipient = new WAPhoneRecipient();

                waPhoneRecipient.setExternalRecipientId(waExternalRecipientId);

                if (optionalWABusinessPhoneNumber.isPresent()) {
                    final WABusinessPhoneNumber businessPhoneNumber = optionalWABusinessPhoneNumber.get();
                    final RecipientProfile profileInfo = new RecipientProfile();

                    profileInfo.setName(recipientPhoneNumber);

                    waBusinessManager.loadRecipientProfile(profileInfo, dataContainer, targetContainer);
                    waPhoneRecipient.setWaBusinessPhoneNumber(businessPhoneNumber);
                    waPhoneRecipient.setRecipientProfile(profileInfo);
                    chat.setChannel(businessPhoneNumber.getChannel());
                    chat.setProfile(profileInfo);
                    messageSendRequest.setNewChat(true);
                    businessDisplayPhoneNumber = businessPhoneNumber.getDisplayPhoneNumber();
                } else {
                    businessDisplayPhoneNumber = null;
                }
            } else {
                final Long chatId = chatRow.getPKValue();

                chat.setId(chatId);
                waPhoneRecipient = waBusinessManager.getWARecipient(chatId);
                messageSendRequest.setNewChat(false);
                businessDisplayPhoneNumber = waPhoneRecipient.getWaBusinessPhoneNumber().getDisplayPhoneNumber();
            }
            sendResponse.put(ChatFields.CHANNEL_NAME, businessDisplayPhoneNumber);
        } else {
            final Long chatId = messageSendRequest.getChatId();
            dataContainer = chatManager.getChatContainer(chatId);
            waPhoneRecipient = waBusinessManager.getWARecipient(chatId);

            chat.setId(chatId);
        }
        chat.setPlatform(Platform.WHATSAPP);
        chat.setNewChat(messageSendRequest.isNewChat());

        if (messageSendRequest.hasMessage() || messageSendRequest.hasTemplate()) {
            setLastMessageToChat(chat, false, messageSendRequest.getMessage(), null, waPhoneRecipient, messageSendRequest.getTemplateRequestData());
            loadAndSendMessage(chat, dataContainer, targetContainer, sendResponse, messageArray, false);
        } else if (messageSendRequest.hasAttachments()) {
            for (final Attachment attachment : messageSendRequest.getAttachments()) {
                setLastMessageToChat(chat, true, null, attachment, waPhoneRecipient, null);
                loadAndSendMessage(chat, dataContainer, targetContainer, sendResponse, messageArray, true);

                if (sendResponse.containsKey(MessageFields.ERRORS)) {
                    break;
                }
            }
        }

        if (NumericalConstant.ZERO < messageArray.size()) {
            sendResponse.put(MessageFields.MESSAGES, messageArray);
            sendResponse.put(MessageFields.CHAT_ID, chat.id());
            sendResponse.put(ChatFields.RECIPIENT_PHONE_NUMBER, recipientPhoneNumber);
        }

        return sendResponse;
    }

    /**
     * <p>
     * Sets the {@link WAMessage} to the chat
     * </p>
     *
     * @param chat             Represents the {@link Chat}
     * @param hasAttachment    Represents true if the message hash attachments, false otherwise
     * @param content          Represents the message content
     * @param attachment       Represents the {@link Attachment}
     * @param waPhoneRecipient Represents the {@link WAPhoneRecipient}
     */
    private void setLastMessageToChat(final Chat chat, final boolean hasAttachment, final String content, final Attachment attachment, final WAPhoneRecipient waPhoneRecipient, final MessageSendRequest.TemplateRequestData templateRequestData) {
        final WAMessage waMessage = new WAMessage();

        waMessage.setWaRecipient(waPhoneRecipient);
        waMessage.setMessageFlow(Message.MessageFlow.OUTBOUND);
        waMessage.setMessageState(Message.MessageState.SENT);

        if (Objects.nonNull(templateRequestData)) {
            waMessage.setTemplateId(templateRequestData.templateId());
            waMessage.setParamData(templateRequestData.paramData());
        }

        if (!hasAttachment) {
            waMessage.setContent(content);
        } else {
            waMessage.setAttachments(List.of(attachment));
        }
        waMessage.setSentTime(Instant.now().toEpochMilli());

        chat.setLastMessage(waMessage);
    }

    /**
     * <p>
     * Loads the data into the container and sends message
     * </p>
     *
     * @param chat            Represents the chat id
     * @param dataContainer   Represents the data container
     * @param targetContainer Represents the target container
     * @param response        Represents the response
     * @param messageArray    Represents the message array
     * @param hasAttachment   Represents true if the message hash attachments, false otherwise
     */
    private void loadAndSendMessage(final Chat chat, final DataContainer dataContainer, final DataContainer targetContainer,
                                    final JsonObject response, final JsonArray messageArray, final boolean hasAttachment) {
        final Class<?>[] validatorGroup = new Class[NumericalConstant.TWO];
        final boolean isNewChat = chat.isNewChat();

        validatorGroup[0] = SocialValidatorGroup.WAOutboundMessageCreate.class;
        validatorGroup[1] = isNewChat ? SocialValidatorGroup.NewChatCreate.class : SocialValidatorGroup.OutboundMessageCreate.class;
        final Collection<String> errors = SocialEntityValidator.validate(List.of(chat), validatorGroup);

        if (errors.isEmpty()) {
            final JsonObject messageJson = Json.object();
            final WAMessage waMessage = (WAMessage) chat.getLastMessage();

            if (hasAttachment) {
                waMessage.setMessageState(Message.MessageState.SENDING);
            }
            chatManager.loadChatIntoContainer(chat, dataContainer, targetContainer);

            if (isNewChat) {
                final WAPhoneRecipient waRecipient = waMessage.getWaRecipient();

                waRecipient.setChatId(chat.id());
                waBusinessManager.loadRecipientIntoContainer(waRecipient, dataContainer, targetContainer);
            }

            if (hasAttachment) {
                chatManager.loadMessageIntoContainer(chat, dataContainer, targetContainer);
                adminDataStore().commitChanges(targetContainer);

                asyncSend(waMessage, dataContainer);
            } else {
                waService.sendMessage(waMessage);
                chatManager.loadMessageIntoContainer(chat, dataContainer, targetContainer);
                waBusinessManager.loadWAMessage(waMessage, dataContainer, targetContainer);

                adminDataStore().commitChanges(targetContainer);
            }

            messageJson.put(MessageFields.ID, waMessage.id());
            messageJson.put(MessageFields.SENT_TIME, waMessage.getSentTime());
            messageJson.put(MessageFields.LAST_MESSAGE, chat.getRecentMessagePreview());
            messageArray.put(messageJson);
        } else {
            response.put(MessageFields.ERRORS, errors);
        }
    }

    /**
     * <p>
     * Sends the message asynchronously for messages with attachments
     * </p>
     *
     * @param waMessage     Represents the message
     * @param dataContainer Represents the data container
     */
    private void asyncSend(final WAMessage waMessage, final DataContainer dataContainer) {
        socialEventHandler.handle(() -> {
            final Long messageId = waMessage.id();

            try {
                final DataContainer targetContainer = DataContainer.create();

                waService.sendMessage(waMessage);
                chatManager.updateMessageStatus(List.of(messageId), waMessage.getSentTime(), Message.MessageState.SENT, null);
                waBusinessManager.loadWAMessage(waMessage, dataContainer, targetContainer);

                adminDataStore().commitChanges(targetContainer);
            } catch (Throwable throwable) {
                LOGGER.error("Whatsapp: Failed to send message. Message id: {}", messageId, throwable);
                chatManager.updateMessageStatus(List.of(messageId), null, Message.MessageState.FAILED, MessageFields.UNKNOWN_ERROR);
            }
        });
    }
}