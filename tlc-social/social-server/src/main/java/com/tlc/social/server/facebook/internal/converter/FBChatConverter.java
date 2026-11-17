package com.tlc.social.server.facebook.internal.converter;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.converter.ChatConverter;
import com.tlc.social.server.facebook.internal.model.FBMessage;
import com.tlc.social.server.facebook.internal.webhook.model.FBTextMessageEvent;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;

import java.util.Collection;
import java.util.Objects;

/**
 * A converter which converts facebook related model to chat
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBChatConverter extends ChatConverter {

    private FBChatConverter() {
    }

    private static final class InstanceHolder {
        private static final FBChatConverter FB_CHAT_CONVERTER = new FBChatConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link FBChatConverter}
     * </p>
     *
     * @return the instance of {@link FBChatConverter}
     */
    public static FBChatConverter getInstance() {
        return InstanceHolder.FB_CHAT_CONVERTER;
    }

    /**
     * <p>
     * Converts the {@link FBTextMessageEvent} into {@link Chat}
     * </p>
     *
     * @param textMessageEvent Represents the {@link FBTextMessageEvent}
     * @param attachments       Represents the collection of {@link Attachment}
     * @return the {@link Chat}
     */
    public Chat convertTextMessageEventToChat(final FBTextMessageEvent textMessageEvent, final Collection<Attachment> attachments) {
        final Chat chat = new Chat();
        final FBMessage lastMessage = new FBMessage();
        final boolean hasAttachments = Objects.nonNull(attachments) && !attachments.isEmpty();

        lastMessage.setContent(textMessageEvent.getText());
        lastMessage.setMessageState(Message.MessageState.SENT);
        lastMessage.setMessageFlow(textMessageEvent.isMessageEcho() ? Message.MessageFlow.OUTBOUND : Message.MessageFlow.INBOUND);
        lastMessage.setSentTime(textMessageEvent.getTimestamp());
        lastMessage.setExternalMessageId(textMessageEvent.getMessageId());

        if (hasAttachments) {
            lastMessage.setAttachments(attachments);
        }
        chat.setPlatform(Platform.FACEBOOK);
        chat.setLastMessage(lastMessage);
        chat.setUnreadCount(hasAttachments ? attachments.size() : NumericalConstant.ONE);

        return chat;
    }
}
