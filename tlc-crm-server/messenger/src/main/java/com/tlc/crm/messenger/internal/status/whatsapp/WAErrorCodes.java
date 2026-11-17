package com.tlc.crm.messenger.internal.status.whatsapp;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * An enum which contains error codes related to WhatsApp business
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum WAErrorCodes implements ErrorCodeProvider {

    INVALID_PHONE_NUMBER_STATE(0x01),
    FAILED_TO_FETCH_WHATSAPP_BUSINESS_ACCOUNT(0x02),
    UNSUPPORTED_EMBEDDED_SIGNUP(0x03),
    BUSINESS_PHONE_NUMBER_IS_EMPTY(0x04),
    USER_ABANDONED_THE_EMBEDDED_SIGN_UP_FLOW(0x05),
    EMBEDDED_SIGN_UP_FAILED(0x05),
    WEBHOOK_VERIFICATION_FAILED(0x06),
    WEBHOOK_PAYLOAD_VALIDATION_FAILED(0x07),
    INVALID_PHONE_NUMBER(0x08),
    PRIMARY_PHONE_NOT_FOUND(0x09),
    TEMPLATE_LIST_RETRIEVAL_FAILED(0x10),
    FAILED_TO_FETCH_BUSINESS_PHONE_NUMBERS(0x11),
    TEMPLATE_RETRIEVAL_FAILED(0x12),
    BUSINESS_PHONE_NUMBER_NOT_FOUND(0x13),
    BUSINESS_ACCOUNT_NOT_FOUND(0x14),
    FAILED_TO_CHECK_TEMPLATE_SYNC_STATUS(0x15),
    TEMPLATE_LANGUAGE_RETRIEVAL_FAILED(0x16),
    TEMPLATE_SYNC_INITIATION_FAILED(0x17),
    INVALID_WA_CAMPAIGN_TEMPLATE_STATUS(0x18),
    TEMPLATE_COUNT_RETRIEVAL_FAILED(0x20),
    ;

    private final int code;

    WAErrorCodes(final int localCode) {
        this.code = WAErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class WAErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new WAErrorCodes.WAErrorCodeGroup();

        /**
         * {@inheritDoc}
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x92_0_0001;
        }
    }
}