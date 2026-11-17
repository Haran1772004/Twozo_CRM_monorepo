package com.tlc.payment.gateway.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Enum representing error codes for the Payment Gateway module.
 *
 * @author Sathish
 * @version 1.0
 */
public enum PaymentGatewayErrorCodes implements ErrorCodeProvider {

    PAYER_NOT_FOUND(0x01),
    PAYER_ALREADY_EXISTS(0x02),
    TRANSACTION_ID_MISSING(0x03),
    INVALID_URL(0x04),
    INVALID_PAYMENT_STATUS(0x05),
    ;

    private final int code;

    PaymentGatewayErrorCodes(final int localCode) {
        this.code = PaymentGatewayErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * Private static class for grouping error codes under a specific prefix.
     */
    private static class PaymentGatewayErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new PaymentGatewayErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_005;
        }
    }
}
