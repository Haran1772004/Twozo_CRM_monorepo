package com.tlc.social.server.facebook.internal.service;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.facebook.model.FBPageRecipient;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.server.facebook.internal.model.FBMessage;
import com.tlc.social.server.facebook.manager.FBPageManager;
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
 * A service implementation of {@link SocialOutboundService} which provides facebook related outbound services
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBOutboundService extends SocialOutboundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBOutboundService.class);

    private final FBService fbService;
    private final FBPageManager fbPageManager;

    private FBOutboundService() {
        fbService = FBService.getInstance();
        fbPageManager = FBPageManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final FBOutboundService FB_OUTBOUND_SERVICE = new FBOutboundService();
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
     * Gets the instance of {@link FBOutboundService}
     * </p>
     *
     * @return the instance of {@link FBOutboundService}
     */
    public static FBOutboundService getInstance() {
        return InstanceHolder.FB_OUTBOUND_SERVICE;
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
        final FBPageRecipient fbPageRecipient = fbPageManager.getFBPageRecipient(chatId);
        final DataContainer targetContainer = DataContainer.create();
        final Chat chat = new Chat();
        final JsonArray messageArray = Json.array();

        chat.setId(chatId);
        chat.setPlatform(Platform.FACEBOOK);

        if (messageSendRequest.hasMessage()) {
            setLastMessageToChat(chat, false, messageSendRequest.getMessage(), null, fbPageRecipient);
            loadAndSendMessage(chat, dataContainer, targetContainer, response, messageArray, false);
        } else if (messageSendRequest.hasAttachments()) {
            for (final Attachment attachment : messageSendRequest.getAttachments()) {
                setLastMessageToChat(chat, true, null, attachment, fbPageRecipient);
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
     * Sets the {@link FBMessage} to the chat
     * </p>
     *
     * @param chat            Represents the {@link Chat}
     * @param hasAttachment   Represents true if the message hash attachments, false otherwise
     * @param content         Represents the message content
     * @param attachment      Represents the {@link Attachment}
     * @param fbPageRecipient Represents the {@link FBPageRecipient}
     */
    private void setLastMessageToChat(final Chat chat, final boolean hasAttachment, final String content, final Attachment attachment, final FBPageRecipient fbPageRecipient) {
        final FBMessage fbMessage = new FBMessage();

        fbMessage.setFbPageRecipient(fbPageRecipient);
        fbMessage.setMessageFlow(Message.MessageFlow.OUTBOUND);
        fbMessage.setMessageState(Message.MessageState.SENT);

        if (!hasAttachment) {
            fbMessage.setContent(content);
        } else {
            fbMessage.setAttachments(List.of(attachment));
        }
        fbMessage.setSentTime(Instant.now().toEpochMilli());

        chat.setLastMessage(fbMessage);
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
    private void loadAndSendMessage(final Chat chat, final DataContainer dataContainer, final DataContainer targetContainer,
                                    final JsonObject response, final JsonArray messageArray, final boolean hasAttachment) {
        final Collection<String> errors = SocialEntityValidator.validate(List.of(chat), SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.FBOutboundMessageCreate.class);

        if (errors.isEmpty()) {
            final JsonObject messageJson = Json.object();
            final FBMessage fbMessage = (FBMessage) chat.getLastMessage();

            if (hasAttachment) {
                fbMessage.setMessageState(Message.MessageState.SENDING);
            }
            chatManager.loadChatIntoContainer(chat, dataContainer, targetContainer);
            chatManager.loadMessageIntoContainer(chat, dataContainer, targetContainer);

            if (hasAttachment) {
                adminDataStore().commitChanges(targetContainer);
                asyncSend(fbMessage, dataContainer);
            } else {
                fbService.sendMessage(fbMessage);
                fbPageManager.loadFBMessage(fbMessage, dataContainer, targetContainer);
                adminDataStore().commitChanges(targetContainer);
            }
            messageJson.put(MessageFields.ID, fbMessage.id());
            messageJson.put(MessageFields.SENT_TIME, fbMessage.getSentTime());
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
     * @param fbMessage Represents the message
     * @param dataContainer    Represents the data container
     */
    private void asyncSend(final FBMessage fbMessage, final DataContainer dataContainer) {
        socialEventHandler.handle(() -> {
            final Long messageId = fbMessage.id();

            try {
                final DataContainer targetContainer = DataContainer.create();

                fbService.sendMessage(fbMessage);
                chatManager.updateMessageStatus(List.of(messageId), fbMessage.getSentTime(), Message.MessageState.SENT, null);
                fbPageManager.loadFBMessage(fbMessage, dataContainer, targetContainer);

                adminDataStore().commitChanges(targetContainer);
            } catch (Throwable throwable) {
                LOGGER.error("Facebook: Failed to send message. Message id: {}", messageId, throwable);
                chatManager.updateMessageStatus(List.of(messageId), null, Message.MessageState.FAILED, MessageFields.UNKNOWN_ERROR);
            }
        });
    }
}
