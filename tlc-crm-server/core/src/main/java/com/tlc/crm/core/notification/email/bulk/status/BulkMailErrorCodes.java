package com.tlc.crm.core.notification.email.bulk.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the error codes related to bulk mail
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum BulkMailErrorCodes implements ErrorCodeProvider {

    SOURCE_NOT_SUPPORTED(0x01),
    DRAFT_ACTION_FAILED(0x02),
    INVALID_BULK_MAIL_STATUS(0x03),
    BULK_MAIL_NOT_FOUND(0x04),
    CREATE_ACTION_FAILED(0x05),
    UPDATE_ACTION_FAILED(0x06),
    PERSONALIZATION_RESTRICTED(0x07),
    INVALID_METRICS_TYPE(0x08),
    BULK_MAIL_METRICS_NOT_FOUND(0x09),
    INVALID_FROM_ADDRESS(0x10),
    LIMIT_EXCEEDED(0x11),
    BULK_MAIL_DELETE_FAILED(0x12),
    ;

    private final int code;

    BulkMailErrorCodes(final int localCode) {
        this.code = BulkMailErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * Represents the {@link ErrorCodeGroup} of bulk mail
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class BulkMailErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new BulkMailErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0000;
        }
    }
}
