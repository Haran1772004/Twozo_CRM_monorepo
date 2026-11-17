package com.tlc.cdc.core.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Handles the cdc-core module error codes.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public enum CdcErrorCodes implements ErrorCodeProvider {

    INVALID_CONNECTOR_TYPE(0x01),
    CDC_HANDLER_ALREADY_EXISTS(0x02),
    ;

    private final int code;

    CdcErrorCodes(final int localCode) {
        this.code = CdcErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class CdcErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new CdcErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x45_0_0001;
        }
    }
}
