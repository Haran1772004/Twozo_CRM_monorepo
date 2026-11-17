package com.tlc.social.common.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * An enum which represents the common social error codes
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum SocialErrorCodes implements ErrorCodeProvider {

    UNSUPPORTED_SOCIAL_PLATFORM(0x01),
    INVALID_MESSAGE_STATE(0x02),
    INVALID_MESSAGE_FLOW(0x03),
    INVALID_LAST_MESSAGE_TYPE(0x04),
    UNSUPPORTED_CHANNEL_TYPE(0X05),
    MESSAGES_RETRIEVAL_FAILED(0X06),
    CHATS_RETRIEVAL_FAILED(0X07),
    CHAT_NOT_FOUND(0x08),
    MESSAGE_OR_ATTACHMENT_IS_REQUIRED(0x09),
    MESSAGE_SEND_ACTION_FAILED(0x10),
    READ_RECEIPT_SEND_ACTION_FAILED(0x11),
    ORG_ID_NOT_SUPPORTED(0x12),
    INVALID_CHAT_FILTER(0x13),
    RECIPIENT_PROFILE_NOT_FOUND(0X14),
    ATTACHMENT_FETCH_FAILED(0x15),
    UNSUPPORTED_WEBHOOK_EVENT(0x16),
    DELIVERY_MESSAGE_EVENT_NOT_SUPPORTED(0X17),
    CHANNEL_NOT_FOUND(0X18),
    UNSUPPORTED_EXTENSION(0x19),
    RECEIVED_UNSUPPORTED_MESSAGE(0X20);

    private final int code;

    SocialErrorCodes(final int localCode) {
        this.code = SocialErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return the code
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class SocialErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new SocialErrorCodeGroup();

        /**
         * {@inheritDoc}
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x95_0_0001;
        }
    }
}
