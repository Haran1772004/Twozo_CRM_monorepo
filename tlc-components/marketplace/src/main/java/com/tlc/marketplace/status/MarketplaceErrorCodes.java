package com.tlc.marketplace.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the zapier error codes.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public enum MarketplaceErrorCodes implements ErrorCodeProvider {

    INVALID_APP_CATEGORY(0x01),
    INVALID_APP_PRICING(0x02),
    INVALID_APP_STATE(0x03),
    INVALID_RATING(0x04),
    ;

    private final int code;

    MarketplaceErrorCodes(final int localCode) {
        this.code = MarketPlaceErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return The code.
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class MarketPlaceErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new MarketPlaceErrorCodeGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x90_0_0002;
        }
    }
}