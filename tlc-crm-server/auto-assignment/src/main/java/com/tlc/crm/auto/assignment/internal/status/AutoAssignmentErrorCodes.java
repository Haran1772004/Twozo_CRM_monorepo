package com.tlc.crm.auto.assignment.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the auto-assignment error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum AutoAssignmentErrorCodes implements ErrorCodeProvider {

    COMPARATORS_NOT_FOUND(0x01),
    INVALID_MODULE(0x02),
    INVALID_CONNECTOR(0x03),
    AUTO_ASSIGNMENT_NOT_FOUND(0x04),
    TEMPLATE_NOT_FOUND(0x05),
    INVALID_STATUS(0x06),
    USER_RETRIEVAL_FAILED(0x07),
    FAILED_TO_RETRIEVE_DATA(0x08),
    INVALID_SOURCE(0x09),
    ;

    private final int code;

    AutoAssignmentErrorCodes(final int localCode) {
        this.code = AutoAssignmentErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return the code
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class AutoAssignmentErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new AutoAssignmentErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x75_0_0001;
        }
    }
}
