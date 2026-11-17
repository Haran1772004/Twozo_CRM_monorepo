package com.tlc.crm.core.internal.invoice.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides error codes for the invoice module.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum InvoiceErrorCodes implements ErrorCodeProvider {

    INVOICE_NOT_FOUND(0x01),
    INVOICE_CREATE_FAILED(0X02),
    INVALID_ENTITY_ID(0X03),
    INVOICE_TEMPLATE_NOT_FOUND(0x04),
    INVOICE_UPDATE_FAILED(0X05),
    INVOICE_UNSUPPORTED_ACTION(0X06),
    CONTACT_EMAIL_NOT_FOUND(0X07),
    INVOICE_FILE_TOO_LARGE(0x08),
    ;

    private final int code;

    InvoiceErrorCodes(final int localCode) {
        this.code = InvoiceErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Provides the error code group for the invoice module.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class InvoiceErrorCodeGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new InvoiceErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x32_0_0002;
        }
    }
}