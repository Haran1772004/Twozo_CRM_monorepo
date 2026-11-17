package com.tlc.crm.messenger.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * An enum which contains error codes related to facebook
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum CrmSocialErrorCodes implements ErrorCodeProvider {

    FAILED_TO_RETRIEVE_PAGES(0x01),
    WEBHOOK_UNSUBSCRIPTION_FAILED(0x02),
    INVALID_PAGE_STATE(0x03),
    PAGE_NOT_FOUND(0x04),
    PAGE_FETCH_FAILED(0x05),
    FAILED_TO_SEND_MESSAGE(0x06),
    WEBHOOK_VERIFICATION_FAILED(0x07),
    WEBHOOK_PAYLOAD_VALIDATION_FAILED(0x08),
    CHATS_FETCH_FAILED(0x09),
    ;

    private final int code;

    CrmSocialErrorCodes(final int localCode) {
        this.code = FBErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class FBErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new FBErrorCodeGroup();

        /**
         * {@inheritDoc}
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x91_0_0001;
        }
    }
}