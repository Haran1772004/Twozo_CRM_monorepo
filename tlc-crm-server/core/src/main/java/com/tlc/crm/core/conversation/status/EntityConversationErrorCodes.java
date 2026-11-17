package com.tlc.crm.core.conversation.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the error codes related to entity conversations
 *
 * @author Kavinila E
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum EntityConversationErrorCodes implements ErrorCodeProvider {

    USER_RETRIEVAL_FAILED(0x01),
    INVALID_CONVERSATION_TYPE(0x02),
    ENTITY_NOT_FOUND(0x03),
    UNSUPPORTED_OPERATION(0x04),
    ENTITY_CONVERSATION_NOT_FOUND(0x05),
    CHATS_FETCH_FAILED(0x06),
    ;

    private final int code;

    EntityConversationErrorCodes(final int localCode) {
        this.code = ConversationErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * Represents the {@link ErrorCodeGroup} of conversations
     *
     * @author kavinila E
     * @version 1.0
     */
    private static class ConversationErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ConversationErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0000;
        }
    }
}
