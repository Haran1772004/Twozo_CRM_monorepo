package com.tlc.crm.core.internal.webform.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Holds the form error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum FormErrorCodes implements ErrorCodeProvider {

    FORM_REQUIRED_FIELD_DATA_IS_MISSING(0x11),
    WEB_FORM_IS_INACTIVE(0x12),
    WEB_FORM_NOT_FOUND(0x13),
    ;

    private final int code;

    FormErrorCodes(int localCode) {
        this.code = FormErrorCodes.FormErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class FormErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new FormErrorCodes.FormErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x30_0_0005;
        }
    }
}
