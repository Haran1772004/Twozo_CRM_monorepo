package com.tlc.social.common.facebook.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * An enum which contains the facebook related error codes
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum FBErrorCodes implements ErrorCodeProvider {

    PAGES_FETCH_FAILED(0x01),
    WEBHOOK_PAGE_SUBSCRIPTION_FAILED(0x02),
    WEBHOOK_PAGE_UN_SUBSCRIPTION_FAILED(0x03),
    RECIPIENT_PROFILE_RETRIEVAL_FAILED(0x04),
    PAGE_NOT_FOUND(0x05),
    ;

    private final int code;

    FBErrorCodes(final int localCode) {
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
            return 0x85_0_0001;
        }
    }
}
