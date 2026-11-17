package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes specific to currency settings
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum CurrencyUnitErrorCodes implements ErrorCodeProvider {

    CURRENCY_NOT_FOUND(0x20),
    INVALID_CONVERSION_TYPE(0x21),
    INVALID_DECIMAL_POINT(0x22),
    BASE_CURRENCY_NOT_FOUND(0x23),
    CURRENCY_ID_ALREADY_EXISTS(0x24),
    UNSUPPORTED_CURRENCY(0x25),
    UNSUPPORTED_ACTION(0x26),
    BASE_CURRENCY_UPDATE_RESTRICTED(0x27),
    CURRENCY_DEACTIVATION_RESTRICTED(0x28),
    BASE_CURRENCY_DEACTIVATION_RESTRICTED(0x29),
    ;

    private final int code;

    CurrencyUnitErrorCodes(final int localCode) {
        this.code = CurrencyUnitErrorCodes.CurrencyErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Represents an error code group specific to currency-related operations.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see ErrorCodeGroup
     */
    private static class CurrencyErrorCodeGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new CurrencyUnitErrorCodes.CurrencyErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x80_0_0001;
        }
    }
}
