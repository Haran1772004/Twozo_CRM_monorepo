package com.tlc.social.server.instagram.internal.converter;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.converter.ChatConverter;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.server.instagram.internal.model.InstagramMessage;
import com.tlc.social.server.instagram.internal.webhook.model.InstagramTextMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.PageTextMessageEvent;
import com.tlc.social.server.internal.webhook.event.model.TextMessageEvent;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Converts instagram chats.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramChatConverter extends ChatConverter {

    private InstagramChatConverter() {
    }

    private static final class InstanceHolder {
        private static final InstagramChatConverter FB_CHAT_CONVERTER = new InstagramChatConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramChatConverter}
     * </p>
     *
     * @return the instance of {@link InstagramChatConverter}
     */
    public static InstagramChatConverter getInstance() {
        return InstanceHolder.FB_CHAT_CONVERTER;
    }

    /**
     * <p>
     * Converts the {@link PageTextMessageEvent} into {@link Chat}.
     * </p>
     *
     * @param textMessageEvent Represents the {@link PageTextMessageEvent}.
     * @param attachments      Represents the collection of {@link Attachment}.
     * @return the {@link Chat}.
     */
    public Chat convertTextMessageEventToChat(final TextMessageEvent textMessageEvent, final Collection<Attachment> attachments) {
        final Chat chat = new Chat();
        final InstagramMessage lastMessage = new InstagramMessage();
        final boolean hasAttachments = Objects.nonNull(attachments) && !attachments.isEmpty();
        final InstagramTextMessageEvent instagramTextMessageEvent = (InstagramTextMessageEvent) textMessageEvent;

        lastMessage.setContent(instagramTextMessageEvent.getText());
        lastMessage.setMessageState(Message.MessageState.SENT);
        lastMessage.setMessageFlow(instagramTextMessageEvent.isMessageEcho() ? Message.MessageFlow.OUTBOUND : Message.MessageFlow.INBOUND);
        lastMessage.setSentTime(instagramTextMessageEvent.getTimestamp());
        lastMessage.setExternalMessageId(instagramTextMessageEvent.getMessageId());

        if (hasAttachments) {
            lastMessage.setAttachments(attachments);
        }
        chat.setPlatform(Platform.INSTAGRAM);
        chat.setLastMessage(lastMessage);
        chat.setUnreadCount(hasAttachments ? attachments.size() : NumericalConstant.ONE);

        return chat;
    }
}
