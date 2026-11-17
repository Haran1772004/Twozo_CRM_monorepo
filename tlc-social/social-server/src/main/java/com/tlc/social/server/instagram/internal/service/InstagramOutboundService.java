package com.tlc.social.server.instagram.internal.service;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.instagram.model.InstagramPageRecipient;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.server.instagram.internal.model.InstagramMessage;
import com.tlc.social.server.instagram.manager.InstagramPageManager;
import com.tlc.social.server.model.MessageSendRequest;
import com.tlc.social.server.service.SocialOutboundService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Pertains to outbound actions from app to instagram endpoint.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramOutboundService extends SocialOutboundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramOutboundService.class);

    private final InstagramService instagramService;
    private final InstagramPageManager instagramPageManager;

    private InstagramOutboundService() {
        instagramService = InstagramService.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
    }

    private static final class Instance {
        private static final InstagramOutboundService INSTAGRAM_OUTBOUND_SERVICE = new InstagramOutboundService();
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
     * Gets the instance of {@link InstagramOutboundService}
     * </p>
     *
     * @return the instance of {@link InstagramOutboundService}
     */
    public static InstagramOutboundService getInstance() {
        return Instance.INSTAGRAM_OUTBOUND_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param messageSendRequest Represents the {@link MessageSendRequest}
     * @return the {@link Message}
     */
    @Override
    public JsonObject send(final MessageSendRequest messageSendRequest) {
        final JsonObject response = Json.object();
        final Long chatId = messageSendRequest.getChatId();
        final DataContainer dataContainer = chatManager.getChatContainer(chatId);
        final InstagramPageRecipient instagramPageRecipient = instagramPageManager.getInstagramPageRecipient(chatId);
        final DataContainer targetContainer = DataContainer.create();
        final Chat chat = new Chat();
        final JsonArray messageArray = Json.array();

        chat.setId(chatId);
        chat.setPlatform(Platform.INSTAGRAM);

        if (messageSendRequest.hasMessage()) {
            setLastMessageToChat(chat, false, messageSendRequest.getMessage(), null, instagramPageRecipient);
            loadAndSendMessage(chat, dataContainer, targetContainer, response, messageArray, false);
        } else if (messageSendRequest.hasAttachments()) {
            for (final Attachment attachment : messageSendRequest.getAttachments()) {
                setLastMessageToChat(chat, true, null, attachment, instagramPageRecipient);
                loadAndSendMessage(chat, dataContainer, targetContainer, response, messageArray, true);

                if (response.containsKey(MessageFields.ERRORS)) {
                    break;
                }
            }
        }

        if (NumericalConstant.ZERO < messageArray.size()) {
            response.put(MessageFields.MESSAGES, messageArray);
        }

        return response;
    }

    /**
     * <p>
     * Sets the {@link InstagramMessage} to the chat
     * </p>
     *
     * @param chat                   {@link Chat}
     * @param hasAttachment          Represents true if the message hash attachments, false otherwise
     * @param content                message content
     * @param attachment             {@link Attachment}
     * @param instagramPageRecipient {@link InstagramPageRecipient}.
     */
    private void setLastMessageToChat(final Chat chat, final boolean hasAttachment, final String content,
                                      final Attachment attachment, final InstagramPageRecipient instagramPageRecipient) {
        final InstagramMessage instagramMessage = new InstagramMessage();

        instagramMessage.setInstagramPageRecipient(instagramPageRecipient);
        instagramMessage.setMessageFlow(Message.MessageFlow.OUTBOUND);
        instagramMessage.setMessageState(Message.MessageState.SENT);

        if (!hasAttachment) {
            instagramMessage.setContent(content);
        } else {
            instagramMessage.setAttachments(List.of(attachment));
        }
        instagramMessage.setSentTime(Instant.now().toEpochMilli());

        chat.setLastMessage(instagramMessage);
        chat.setUnreadCount(NumericalConstant.ONE);
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
    private void loadAndSendMessage(final Chat chat, final DataContainer dataContainer, final DataContainer targetContainer, final JsonObject response,
                                    final JsonArray messageArray, final boolean hasAttachment) {
        final Collection<String> errors = SocialEntityValidator.validate(List.of(chat), SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InstagramOutboundMessageCreate.class);

        if (errors.isEmpty()) {
            final JsonObject messageJson = Json.object();
            final InstagramMessage instagramMessage = (InstagramMessage) chat.getLastMessage();

            if (hasAttachment) {
                instagramMessage.setMessageState(Message.MessageState.SENDING);
            }
            chatManager.loadChatIntoContainer(chat, dataContainer, targetContainer);
            chatManager.loadMessageIntoContainer(chat, dataContainer, targetContainer);

            if (hasAttachment) {
                adminDataStore().commitChanges(targetContainer);
                asyncSend(instagramMessage, dataContainer);
            } else {
                instagramService.sendMessage(instagramMessage);
                instagramPageManager.loadInstagramMessage(instagramMessage, dataContainer, targetContainer);
                adminDataStore().commitChanges(targetContainer);
            }
            messageJson.put(MessageFields.ID, instagramMessage.id());
            messageJson.put(MessageFields.SENT_TIME, instagramMessage.getSentTime());
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
     * @param instagramMessage Represents the message
     * @param dataContainer    Represents the data container
     */
    private void asyncSend(final InstagramMessage instagramMessage, final DataContainer dataContainer) {
        socialEventHandler.handle(() -> {
            final Long messageId = instagramMessage.id();

            try {
                final DataContainer targetContainer = DataContainer.create();

                instagramService.sendMessage(instagramMessage);
                chatManager.updateMessageStatus(List.of(messageId), instagramMessage.getSentTime(), Message.MessageState.SENT, null);
                instagramPageManager.loadInstagramMessage(instagramMessage, dataContainer, targetContainer);

                adminDataStore().commitChanges(targetContainer);
            } catch (Throwable throwable) {
                LOGGER.error("Instagram: Failed to send message. Message id: {}", messageId, throwable);
                chatManager.updateMessageStatus(List.of(messageId), null, Message.MessageState.FAILED, MessageFields.UNKNOWN_ERROR);
            }
        });
    }
}
