package com.tlc.social.common.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.common.internal.status.SocialErrorCodes;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A model which represents the social media channel
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class Channel implements SocialEntity {

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {SocialValidatorGroup.InboundMessageCreate.class}, message = "i18n.social.channel.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class}, message = "i18n.social.channel.name.required")
    private String name;

    @NotNull(groups = {Group.Create.class}, message = "i18n.social.channel.type.required")
    private ChannelType type;

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
     * An enum which contains all the channel type
     *
     * @author Maharaja S
     * @version 1.0
     */
    @AllArgsConstructor
    @Getter
    public enum ChannelType {

        FB_PAGE((short) 1),
        WHATSAPP_BUSINESS((short) 2),
        INSTAGRAM_PAGE((short) 3),
        ;

        private final short id;

        /**
         * <p>
         * Gets the {@link ChannelType} based on the provided id
         * </p>
         *
         * @param id Represents the id of the enum
         * @return the {@link ChannelType}
         */
        public static ChannelType get(final short id) {
            return switch (id) {
                case 1 -> FB_PAGE;
                case 2 -> WHATSAPP_BUSINESS;
                case 3 -> INSTAGRAM_PAGE;
                default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_CHANNEL_TYPE, "i18n.social.unsupported.channel");
            };
        }

        /**
         * <p>
         * Gets platform based on the channel id.
         * </p>
         *
         * @param channelTypeId Channel's id.
         * @return {@link Channel}.
         */
        public static Platform getPlatform(final short channelTypeId) {
            return switch (channelTypeId) {
                case 1 -> Platform.FACEBOOK;
                case 2 -> Platform.WHATSAPP;
                case 3 -> Platform.INSTAGRAM;
                default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_CHANNEL_TYPE, "i18n.social.unsupported.channel");
            };
        }
    }
}
