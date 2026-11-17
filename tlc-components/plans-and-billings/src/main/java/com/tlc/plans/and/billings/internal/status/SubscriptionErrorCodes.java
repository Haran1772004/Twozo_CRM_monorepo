package com.tlc.plans.and.billings.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Enum representing error codes for the Plans & Billings module.
 *
 * @author Sathish
 * @version 1.0
 */
public enum SubscriptionErrorCodes implements ErrorCodeProvider {

    INVALID_BILLING_CYCLE(0x01),
    INVALID_PLAN_TYPE(0x02),
    INVALID_PRICING_BASIS(0x03),
    DEFAULT_TRIAL_PLAN_NOT_FOUND(0x04),
    INVALID_DISCOUNT_TYPE(0x05),
    DISCOUNT_NOT_FOUND(0x06),
    INVALID_TIME_PERIOD(0x07),
    ;

    private final int code;

    SubscriptionErrorCodes(final int localCode) {
        this.code = SubscriptionErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * Private static class for grouping error codes under a specific prefix.
     */
    private static class SubscriptionErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new SubscriptionErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_00005;
        }
    }
}
