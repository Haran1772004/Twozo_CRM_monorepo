package com.tlc.social.common.model;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.commons.code.ErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

/**
 * A model which represents the chat and contains the chat details
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class Chat implements SocialEntity {

    private static final int DEFAULT_MSG_PREVIEW_LENGTH = 250;

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class}, message = "i18n.social.chat.id.required")
    private Long id;

    @Valid
    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.NewChatCreate.class}, message = "i18n.social.chat.message.required")
    private Message lastMessage;

    @Valid
    private RecipientProfile profile;

    @Valid
    private Channel channel;
    private int unreadCount;

    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class, SocialValidatorGroup.NewChatCreate.class}, message = "i18n.social.chat.platform.required")
    private Platform platform;

    private boolean isNewChat;

    public Message.MessageFlow getLastMessageType() {
        return lastMessage.getMessageFlow();
    }

    public Long getLastMessageTime() {
        return lastMessage.getSentTime();
    }

    /**
     * <p>
     * Gets the recent message preview
     * </p>
     *
     * @return the recent message preview
     */
    public String getRecentMessagePreview() {
        return getRecentMessagePreview(DEFAULT_MSG_PREVIEW_LENGTH);
    }

    /**
     * <p>
     * Gets the recent message preview from the provided preview length
     * </p>
     *
     * @return the recent message preview
     */
    public String getRecentMessagePreview(final int previewLength) {
        final String content = lastMessage.getContent();

        if (Objects.isNull(content)) {
            final List<Attachment> attachments = (List<Attachment>) lastMessage.getAttachments();

            return attachments.get(attachments.size() - NumericalConstant.ONE).getFileName();
        } else {
            final int contentLength = content.length();

            return previewLength < contentLength ? content.substring(0, previewLength) : content;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the id
     */
    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        throw ErrorCode.get(SocialErrorCodes.ORG_ID_NOT_SUPPORTED, "i18n.social.model.org.id.not.supported");
    }

    @Override
    public Object identity() {
        return null;
    }
}
