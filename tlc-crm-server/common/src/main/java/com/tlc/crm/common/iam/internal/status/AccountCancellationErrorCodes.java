package com.tlc.crm.common.iam.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of account cancellation
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public enum AccountCancellationErrorCodes implements ErrorCodeProvider {

    ORG_CANCELLATION_FORBIDDEN(0x01),
    ORG_CANCELLATION_REQUEST_EXISTS(0x02),
    ORG_CANCELLATION_REQUEST_NOT_FOUND(0x03),
    RETENTION_TIME_EXPIRED(0x04),
    CANCELLATION_REASON_NOT_FOUND(0x05),
    ALTERNATE_PRODUCT_NOT_FOUND(0x06),
    RECONSIDERATION_NOT_FOUND(0x07),
    ;

    private final int code;

    AccountCancellationErrorCodes(final int code) {
        this.code = AccountCancellationErrorCodes.AccountCancellationErrorCodeGroup.GROUP.getConvertedCode(code);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class AccountCancellationErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new AccountCancellationErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x15_0_0001;
        }
    }
}