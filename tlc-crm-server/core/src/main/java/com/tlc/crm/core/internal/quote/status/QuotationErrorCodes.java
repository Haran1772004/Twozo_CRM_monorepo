package com.tlc.crm.core.internal.quote.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides error codes for the quotation module.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum QuotationErrorCodes implements ErrorCodeProvider {

    QUOTATION_NOT_FOUND(0x01),
    QUOTATION_CREATE_FAILED(0x02),
    QUOTATION_UPDATE_FAILED(0x03),
    QUOTATION_DELETE_FAILED(0x04),
    QUOTATION_CLONE_FAILED(0x05),
    INVALID_QUOTATION_STATUS(0x06),
    QUOTATION_TEMPLATE_NOT_FOUND(0x07),
    QUOTATION_TEMPLATE_ACCESS_FAILED(0x08),
    QUOTATION_TEMPLATE_MAPPING_FAILED(0x09),
    INVALID_PLACEHOLDER(0x10),
    DEAL_ASSOCIATION_REQUIRED(0x11),
    INVALID_DISCOUNT_FORMAT(0x12),
    INVALID_SUB_TOTAL_FORMAT(0x13),
    INVALID_PAYMENT_TERM(0x14),
    INVALID_EFFECTIVE_DATE_VALUE_TYPE(0x15),
    QUOTATION_FILE_TOO_LARGE(0x16),
    PRIMARY_CONTACT_REQUIRED(0x17),
    ;

    private final int code;

    QuotationErrorCodes(final int localCode) {
        this.code = QuotationErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class QuotationErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new QuotationErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x31_0_0001;
        }
    }
}

