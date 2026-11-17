package com.tlc.exchange.rate.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error code to refer the exchange-rate related errors.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum ExchangeRateErrorCodes implements ErrorCodeProvider {

    UNSUPPORTED_SUBSCRIPTION_PLAN(0x01),
    INVALID_CURRENCY_SYMBOLS(0x02),
    INVALID_DATE(0x03),
    INVALID_EXCHANGE_RATE(0x04),
    INVALID_API_KEY(0x05),
    RESOURCE_NOT_FOUND(0x06),
    INVALID_BASE_CURRENCY(0x07),
    ACCESS_DENIED(0x08),
    API_USAGE_LIMIT_REACHED(0x09),
    INVALID_URL(0x10),
    INVALID_CURRENCY(0x11),
    EXCHANGE_RATE_SERVICE_NOT_INITIALIZED(0x12),
    TARGET_CURRENCY_ACCESS_DENIED(0x13),
    ;

    private final int code;

    ExchangeRateErrorCodes(final int code) {
        this.code = BaseCurrencyErrorCodesGroup.GROUP.getConvertedCode(code);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return the code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Gets the error code of base currency.
     * </p>
     *
     * @param errorCode Represents the error code
     * @return {@link ExchangeRateErrorCodes}
     */
    public static ExchangeRateErrorCodes getErrorCode(final int errorCode) {

        return switch (errorCode) {
            case 101 -> INVALID_API_KEY;
            case 104 -> API_USAGE_LIMIT_REACHED;
            case 105 -> UNSUPPORTED_SUBSCRIPTION_PLAN;
            case 201 -> INVALID_BASE_CURRENCY;
            case 202 -> INVALID_CURRENCY_SYMBOLS;
            case 302 -> INVALID_DATE;
            case 403 -> INVALID_EXCHANGE_RATE;
            case 404 -> RESOURCE_NOT_FOUND;
            default -> ACCESS_DENIED;
        };
    }

    /**
     * <p>
     * Represents the group of base currency error codes.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class BaseCurrencyErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new BaseCurrencyErrorCodesGroup();

        /**
         * <p>
         * Gets the value of prefix.
         * </p>
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x10_0_001;
        }
    }
}
