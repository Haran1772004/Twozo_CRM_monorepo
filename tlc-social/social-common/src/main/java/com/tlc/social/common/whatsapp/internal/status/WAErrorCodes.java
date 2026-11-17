package com.tlc.social.common.whatsapp.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * An enum which contains the WhatsApp related error codes
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum WAErrorCodes implements ErrorCodeProvider {

    WA_BUSINESS_ACCOUNT_FETCH_FAILED(0x01),
    INVALID_BUSINESS_PHONE_NUMBER(0x02),
    WEBHOOK_SUBSCRIPTION_FAILED(0x03),
    BUSINESS_PHONE_NUMBER_REGISTRATION_FAILED(0x04),
    BUSINESS_PHONE_NUMBER_NOT_FOUND(0x05),
    MEDIA_RETRIEVAL_FAILED(0x06),
    UNSUPPORTED_TEMPLATE_LANGUAGE(0x07),
    FAILED_TO_FETCH_TEMPLATES(0x08),
    UNSUPPORTED_TEMPLATE_CATEGORY(0x09),
    UNSUPPORTED_TEMPLATE_PARAM_FORMAT(0x10),
    UNSUPPORTED_TEMPLATE_STATUS(0x11),
    UNSUPPORTED_TEMPLATE_COMPONENT_TYPE(0X12),
    UNSUPPORTED_TEMPLATE_HEADER_FORMAT(0x13),
    UNSUPPORTED_BUTTON_TYPE(0x14),
    UNSUPPORTED_TEMPLATE_SUB_CATEGORY(0x15),
    TEMPLATE_NOT_FOUND(0x16),
    WA_BUSINESS_ACCOUNT_NOT_FOUND(0x17),
    INVALID_SYNC_STATUS(0x18),
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
            return 0x55_0_0001;
        }
    }
}
