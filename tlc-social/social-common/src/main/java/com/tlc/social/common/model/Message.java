package com.tlc.social.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.file.common.models.Attachment;

import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.validator.group.SocialValidatorGroup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * A model which contains the message details
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class Message implements SocialEntity {

    @Getter(AccessLevel.NONE)
    private Long id;

    @Size(max = 2000, groups = {SocialValidatorGroup.FBOutboundMessageCreate.class}, message = "i18n.social.facebook.message.invalid.length")
    @Size(max = 4096, groups = {SocialValidatorGroup.WAOutboundMessageCreate.class}, message = "i18n.social.whatsapp.message.invalid.length")
    @Size(max = 1000, groups = {SocialValidatorGroup.InstagramOutboundMessageCreate.class}, message = "i18n.social.instagram.message.invalid.length")
    private String content;

    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class, SocialValidatorGroup.NewChatCreate.class}, message = "i18n.social.message.state.required")
    private MessageState messageState;

    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class, SocialValidatorGroup.NewChatCreate.class}, message = "i18n.social.message.flow.required")
    private MessageFlow messageFlow;

    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class, SocialValidatorGroup.NewChatCreate.class}, message = "i18n.social.message.sent.time.required")
    private Long sentTime;
    private Long deliveredTime;
    private Long readTime;
    private String failedReason;

    @Valid
    private Collection<Attachment> attachments;

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

    /**
     * An enum which contains all the message state
     *
     * @author Maharaja S
     * @version 1.0
     */
    @AllArgsConstructor
    @Getter
    public enum MessageState {

        SENDING((short) 1, "sending"),
        SENT((short) 2, "sent"),
        DELIVERED((short) 3, "delivered"),
        READ((short) 4, "read"),
        FAILED((short) 5, "failed");

        private final short id;
        private final String name;

        /**
         * <p>
         * Gets the {@link MessageState} based on the provided id
         * </p>
         *
         * @param id Represents the enum id
         * @return the {@link MessageState}
         */
        public static MessageState get(final short id) {
            return switch (id) {
                case 1 -> SENDING;
                case 2 -> SENT;
                case 3 -> DELIVERED;
                case 4 -> READ;
                case 5 -> FAILED;
                default -> throw ErrorCode.get(SocialErrorCodes.INVALID_MESSAGE_STATE, "i18n.social.message.invalid.state");
            };
        }

        /**
         * <p>
         * Gets the {@link MessageState} based on the provided name
         * </p>
         *
         * @param name Represents the enum name
         * @return the {@link MessageState}
         */
        public static MessageState get(final String name) {
            return switch (name.toLowerCase()) {
                case "sending" -> SENDING;
                case "sent" -> SENT;
                case "delivered" -> DELIVERED;
                case "read" -> READ;
                case "failed" -> FAILED;
                default -> throw ErrorCode.get(SocialErrorCodes.INVALID_MESSAGE_STATE, "i18n.social.message.invalid.state");
            };
        }
    }

    /**
     * An enum which contains the message flow
     *
     * @author Maharaja S
     * @version 1.0
     */
    @AllArgsConstructor
    @Getter
    public enum MessageFlow {

        INBOUND((short) 1, "inbound"),
        OUTBOUND((short) 2, "outbound");

        private final short id;
        private final String name;

        /**
         * <p>
         * Gets the {@link MessageFlow} based on the provided id
         * </p>
         *
         * @param id Represents the enum id
         * @return the {@link MessageFlow}
         */
        public static MessageFlow get(final short id) {
            return switch (id) {
                case 1 -> INBOUND;
                case 2 -> OUTBOUND;
                default -> throw ErrorCode.get(SocialErrorCodes.INVALID_MESSAGE_FLOW, "i18n.social.message.invalid.flow");
            };
        }

        /**
         * <p>
         * Gets the {@link MessageFlow} based on the provided name
         * </p>
         *
         * @param name Represents the enum name
         * @return the {@link MessageFlow}
         */
        public static MessageFlow get(final String name) {
            return switch (name.toLowerCase()) {
                case "inbound" -> INBOUND;
                case "outbound" -> OUTBOUND;
                default -> throw ErrorCode.get(SocialErrorCodes.INVALID_MESSAGE_FLOW, "i18n.social.message.invalid.flow");
            };
        }
    }
}
