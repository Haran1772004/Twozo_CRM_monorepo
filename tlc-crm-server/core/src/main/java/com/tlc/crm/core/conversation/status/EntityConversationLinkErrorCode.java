package com.tlc.crm.core.conversation.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Which contains entity email linking related error code
 *
 * @author Kishore J
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum EntityConversationLinkErrorCode implements ErrorCodeProvider {

    INVALID_SOURCE(0x01),
    THREAD_ALREADY_MAPPED_WITH_ANOTHER_DEAL(0x02),
    EMAIL_ADDRESS_ALREADY_EXISTS(0x03),
    DUPLICATE_MOBILE_NUMBER(0x04),
    DUPLICATE_EMAIL_ADDRESS(0x05),
    SALES_OWNER_NOT_FOUND(0x06),
    TERRITORY_NOT_FOUND(0x07),
    PRODUCT_NOT_FOUND(0x08),
    VARIATION_NOT_FOUND(0x09),
    CRM_USER_NOT_FOUND(0x10),
    CREATE_ACTION_FAILED(0x11),
    THREAD_NOT_FOUND(0x12),
    DEAL_NOT_FOUND(0x13),
    CONTACT_NOT_FOUND(0x14),
    COMPANY_NOT_FOUND(0x15),
    LIFE_CYCLE_STAGE_NOT_FOUND(0x16),
    INVALID_PIPELINE_OR_STAGE(0x17),
    EMAILS_LIMIT_EXCEEDED(0x18),
    LIFE_CYCLE_STATUS_NOT_FOUND(0x19),
    UNLINK_CONVERSATION_ACTION_RESTRICTED(0x20),
    ;

    private final int code;

    EntityConversationLinkErrorCode(final int localCode) {
        this.code = EmailLinkedErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class EmailLinkedErrorCodeGroup implements ErrorCodeGroup {
        private static final EmailLinkedErrorCodeGroup GROUP = new EmailLinkedErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x22_0_0001;
        }
    }
}
