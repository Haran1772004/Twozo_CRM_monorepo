package com.tlc.social.common.instagram.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Pertains to instagram error codes.
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum InstagramErrorCodes implements ErrorCodeProvider {

    PAGES_FETCH_FAILED(0x01),
    WEBHOOK_PAGE_SUBSCRIPTION_FAILED(0x02),
    WEBHOOK_PAGE_UN_SUBSCRIPTION_FAILED(0x03),
    RECIPIENT_PROFILE_RETRIEVAL_FAILED(0x04),
    ATTACHMENT_FETCH_FAILED(0x05),
    PAGE_NOT_FOUND(0x06),
    ;

    private final int code;

    InstagramErrorCodes(final int localCode) {
        this.code = InstagramErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class InstagramErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new InstagramErrorCodeGroup();

        /**
         * {@inheritDoc}
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x85_0_0001;
        }
    }
}
