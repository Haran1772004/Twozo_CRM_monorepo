package com.tlc.crm.core.internal.marketinglist.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of crm marketing list
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum MarketingListErrorCodes implements ErrorCodeProvider {

    MARKETING_LIST_NOT_FOUND(0x01),
    CONTACT_NOT_FOUND(0x02),
    MARKETING_LIST_NAME_ALREADY_EXIST(0x03),
    FAILED_TO_DELETE_MARKETING_LIST(0x04),
    FAILED_TO_CREATE_MARKETING_LIST(0x05),
    ;

    private final int code;

    MarketingListErrorCodes(final int localCode) {
        this.code = MarketingListErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class MarketingListErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new MarketingListErrorCodeGroup();

        /**
         * {@inheritDoc}
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x28_0_0001;
        }
    }
}