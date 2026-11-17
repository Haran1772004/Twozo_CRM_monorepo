package com.tlc.crm.plan.and.billing.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Enum representing error codes for the Plans & Billings module.
 *
 * @author Sathish
 * @version 1.0
 */
public enum SubscriptionErrorCodes implements ErrorCodeProvider {

    INVALID_SUBSCRIPTION_STATUS(0x01),
    PLAN_NOT_FOUND(0x02),
    INVALID_CONTRACT_STATE(0x03),
    SUBSCRIPTION_FAILED(0x04),
    SUBSCRIPTION_UPDATE_FAILED(0x05),
    USER_RETRIEVAL_FAILED(0x06),
    BILLING_CONTACT_NOT_EXISTS(0x07),
    INVALID_INVOICE_STATUS(0x08),
    INVALID_SORT_FIELD(0x09),
    BILLING_DETAILS_ALREADY_EXISTS(0x10),
    ORGANIZATION_RETRIEVAL_FAILED(0x11),
    CANCELLATION_DISCOUNT_ACTIVATION_FAILED(0x12),
    INVALID_CURRENT_PLAN(0x13),
    NO_PLAN_STATUS_FOUND(0x14),
    PLAN_ALREADY_EXPIRED(0x15),
    INVALID_BILLING_DETAILS(0x16),
    INVALID_PLAN_CANCELLATION_DATA(0x17),
    INVALID_PLAN(0x18),
    RETRIEVAL_LIMIT_EXCEEDED(0x19),
    INVALID_EMAIL(0x20),
    INVALID_PHONE(0x21),
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
            return 0x10_0_00001;
        }
    }
}
