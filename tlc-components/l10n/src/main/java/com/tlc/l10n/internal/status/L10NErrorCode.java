package com.tlc.l10n.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the error codes related to L10N
 *
 * @author Kishore
 * @version 1.0
 */
public enum L10NErrorCode implements ErrorCodeProvider {

    INVALID_TIMEZONE(0x1),
    INVALID_CURRENCY(0x2),
    INVALID_COUNTRY(0x3),
    INVALID_TIMESTAMP(0x4),
    ;

    private final int localCode;

    L10NErrorCode(final int localCode) {
        this.localCode = L10NErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return localCode;
    }

    private static class L10NErrorCodeGroup implements ErrorCodeGroup {

        private static final L10NErrorCodeGroup GROUP = new L10NErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x60_0_0000;
        }
    }
}
