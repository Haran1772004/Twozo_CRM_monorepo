package com.tlc.social.server.whatsapp.internal.webhook.converter;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.converter.ChatConverter;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.server.whatsapp.internal.model.WAMessage;
import com.tlc.social.server.whatsapp.internal.webhook.model.WATextMessageEvent;

import java.util.List;
import java.util.Objects;

/**
 * A converter which converts Whatsapp related model to chat
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAChatConverter extends ChatConverter {

    private WAChatConverter() {
    }

    private static final class InstanceHolder {
        private static final WAChatConverter WA_CHAT_CONVERTER = new WAChatConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WAChatConverter}
     * </p>
     *
     * @return the instance of {@link WAChatConverter}
     */
    public static WAChatConverter getInstance() {
        return InstanceHolder.WA_CHAT_CONVERTER;
    }

    /**
     * <p>
     * Converts the {@link WATextMessageEvent} into {@link Chat}
     * </p>
     *
     * @param textMessageEvent Represents the {@link WATextMessageEvent}
     * @param attachment       Represents the {@link Attachment}
     * @return the {@link Chat}
     */
    public Chat convertTextMessageEventToChat(final WATextMessageEvent textMessageEvent, final Attachment attachment) {
        final Chat chat = new Chat();
        final WAMessage lastMessage = new WAMessage();

        lastMessage.setContent(textMessageEvent.getText());
        lastMessage.setMessageState(Message.MessageState.SENT);
        lastMessage.setMessageFlow(Message.MessageFlow.INBOUND);
        lastMessage.setSentTime(textMessageEvent.getTimestamp());
        lastMessage.setExternalMessageId(textMessageEvent.getMessageId());

        if (Objects.nonNull(attachment)) {
            lastMessage.setAttachments(List.of(attachment));
        }
        chat.setPlatform(Platform.WHATSAPP);
        chat.setLastMessage(lastMessage);
        chat.setUnreadCount(NumericalConstant.ONE);

        return chat;
    }
}
