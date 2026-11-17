package com.tlc.crm.workflow.v2.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents workflow V2 error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum WorkflowV2ErrorCodes implements ErrorCodeProvider {

    WORKFLOW_NOT_FOUND(0x01),
    WORKFLOW_MARKED_DELETED(0x02),
    INVALID_NODE_TYPE(0x03),
    INVALID_VERSION_STATUS(0x04),
    INVALID_TRIGGER_SCOPE(0x05),
    INVALID_EDGE_TYPE(0x06),
    INVALID_EVENT_CONTEXT(0x07),
    INVALID_GRAPH(0x08),
    DRAFT_ONLY_EDIT(0x09),
    NODE_NOT_FOUND(0x10),
    COMPARATORS_NOT_FOUND(0x11),
    INVALID_TRIGGER_SOURCE(0x12),
    INVALID_TRIGGER_EVENT(0x13),
    INVALID_SPAN_CONFIGURATION(0x14),
    INVALID_EXECUTION_STATUS(0x15),
    VERSION_NOT_FOUND(0x16),
    INVALID_SPAN_UNIT(0x17),
    INVALID_SCHEDULE_TYPE(0x18),
    ;

    private final int code;

    WorkflowV2ErrorCodes(final int localCode) {
        this.code = WorkflowV2ErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class WorkflowV2ErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new WorkflowV2ErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x75_1_0001;
        }
    }
}
