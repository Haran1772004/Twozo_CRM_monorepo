package com.tlc.crm.messenger.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Error codes related to messenger module.
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum MessengerErrorCodes implements ErrorCodeProvider {

    CHAT_NOT_FOUND(0x01),
    DEAL_NOT_FOUND(0x02),
    CHAT_LIST_RETRIEVAL_FAILED(0x03),
    CONTACT_NOT_FOUND(0x04),
    INVALID_CHAT_FILTER(0x05),
    INVALID_CONVERSATION_TYPE(0x06),
    CONVERSATION_NOT_FOUND(0x07),
    INVALID_MESSAGE_TYPE(0x08),
    FAILED_TO_SEND_READ_RECEIPT(0X09),
    USER_RETRIEVAL_FAILED(0x10),
    NO_ENTITY_FOUND_TO_LINK(0x11),
    ENTITY_CREATE_ACTION_FAILED(0x12),
    CHAT_CREATE_ACTION_FAILED(0x13),
    FAILED_TO_ASSIGN_USER_TO_CHAT(0X14),
    INVALID_OUTBOUND_MESSAGE_SOURCE(0x15),
    ;

    private final int code;

    MessengerErrorCodes(final int localCode) {
        this.code = MessengerErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class MessengerErrorCodeGroup implements ErrorCodeGroup {
        private static final MessengerErrorCodeGroup GROUP = new MessengerErrorCodeGroup();

        /**
         * {@inheritDoc}
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x90_0_0001;
        }
    }
}
