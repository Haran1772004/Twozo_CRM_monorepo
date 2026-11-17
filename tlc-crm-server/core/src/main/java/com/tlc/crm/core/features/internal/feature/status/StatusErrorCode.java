package com.tlc.crm.core.features.internal.feature.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Contains the features related error codes
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum StatusErrorCode implements ErrorCodeProvider {

    CONTACT_NOT_FOUND(0x14),
    DEAL_NOT_FOUND(0x15),
    COMPANY_NOT_FOUND(0x21);

    private final int code;

    StatusErrorCode(final int localCode) {
        this.code = StatusErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class StatusErrorCodeGroup implements ErrorCodeGroup {
        private static final StatusErrorCodeGroup GROUP = new StatusErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}
