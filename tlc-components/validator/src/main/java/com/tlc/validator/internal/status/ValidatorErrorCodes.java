package com.tlc.validator.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of validator module
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum ValidatorErrorCodes implements ErrorCodeProvider {

    EXPRESSION_EVALUATION_FAILED(0x01),
    INVALID_CRITERIA_INPUT(0x02);

    private final int code;

    ValidatorErrorCodes(final int localCode) {
        this.code = ValidatorErrorCodes.CommonErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class CommonErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new ValidatorErrorCodes.CommonErrorCodesGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x30_0_0002;
        }
    }
}
