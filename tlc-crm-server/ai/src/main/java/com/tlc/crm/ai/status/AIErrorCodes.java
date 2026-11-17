package com.tlc.crm.ai.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Enum representing error codes for the AI module.
 *
 * @author Sathish
 * @version 1.0
 */
public enum AIErrorCodes implements ErrorCodeProvider {

    REQUEST_PROCESSING_FAILED(0x01),
    ;

    private final int code;

    AIErrorCodes(final int localCode) {
        this.code = AIErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * Private static class for grouping error codes under a specific prefix.
     */
    private static class AIErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new AIErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_051;
        }
    }
}
