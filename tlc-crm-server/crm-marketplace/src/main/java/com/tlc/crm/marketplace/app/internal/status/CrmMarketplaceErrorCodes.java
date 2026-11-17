package com.tlc.crm.marketplace.app.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the zapier error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum CrmMarketplaceErrorCodes implements ErrorCodeProvider {

    UNSUPPORTED_ACTION(0x01),
    FAILED_TO_FETCH_DATA(0x02),
    APP_NOT_FOUND(0x03),
    INVALID_APPLICATIONS_SCOPE(0x04),
    FAILED_TO_REGISTER_MARKETPLACE_HANDLERS(0x05),
    MARKETPLACE_APP_HANDLER_NOT_FOUND(0x06),
    INSTALLATION_FAILED(0x07),
    UNINSTALLATION_FAILED(0x08),
    ;

    private final int code;

    CrmMarketplaceErrorCodes(final int localCode) {
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
            return 0x90_0_0003;
        }
    }
}
