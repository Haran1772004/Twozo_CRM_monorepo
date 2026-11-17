package com.tlc.crm.core.internal.deal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 *     Holds the forecast related error codes.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ForecastErrorCodes implements ErrorCodeProvider {

    DEAL_NOT_FOUND(0x21),
    PIPELINE_STAGE_NOT_FOUND(0x22),
    PIPELINE_NOT_FOUND(0x23),
    INVALID_FORECAST_INTERVAL(0x23),
    INVALID_FORECAST_FIELD_STORE_TYPE(0x24),
    ;

    private final int code;

    ForecastErrorCodes(int localCode) {
        this.code = localCode;
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

    private static class ForecastErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ForecastErrorCodes.ForecastErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}