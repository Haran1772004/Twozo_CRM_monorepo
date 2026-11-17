package com.tlc.validator;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Provides the error codes of an validation
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public enum ValidationErrorCodes implements ErrorCodeProvider {

    DATA_VALIDATION_FAILED(0x01),
    ;

    private final int code;

    ValidationErrorCodes(final int code) {
        this.code = ValidationErrorCodeGroup.GROUP.getConvertedCode(code);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class ValidationErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ValidationErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}