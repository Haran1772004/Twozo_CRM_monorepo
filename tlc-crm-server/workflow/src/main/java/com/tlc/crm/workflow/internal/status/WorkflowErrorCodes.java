package com.tlc.crm.workflow.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the workflow error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum WorkflowErrorCodes implements ErrorCodeProvider {

    COMPARATORS_NOT_FOUND(0x01),
    INVALID_TRIGGER_SOURCE(0x02),
    INVALID_TRIGGER_EVENT(0x03),
    INVALID_CONNECTOR(0x04),
    INVALID_TEMPLATE_CATEGORY(0x05),
    WORKFLOW_NOT_FOUND(0x06),
    INVALID_COMPARATOR_CATEGORY(0x07),
    TEMPLATE_NOT_FOUND(0x08),
    ACTION_SOURCE_NOT_FOUND(0x09),
    ACTION_FIELD_NOT_FOUND(0x10),
    FILTER_GROUP_NOT_FOUND(0x11),
    SORT_BY_NOT_FOUND(0x12),
    INVALID_TRIGGER_TYPE(0x13),
    INVALID_ACTION_TYPE(0x14),
    INVALID_DATA_INTERVAL_TYPE(0x15),
    INVALID_STATUS(0x16),
    INVALID_FILTER_TYPE(0x17),
    USER_RETRIEVAL_FAILED(0x18),
    WORKFLOW_ACTION_NOT_FOUND(0x19),
    TRIGGER_ACTION_NOT_FOUND(0x20),
    INVALID_WORKFLOW_EXECUTION_STATUS_FILTER(0x21),
    INVALID_WORKFLOW_EXECUTION_FILTER_TYPE(0x22),
    INVALID_WORKFLOW_EXECUTION_STATUS(0x23),
    INVALID_WORKFLOW_EXECUTION_ACTION_STATUS(0x24),
    INVALID_WORKFLOW_EXECUTION_CRITERIA_STATUS(0x25),
    HISTORY_NOT_FOUND(0x26),
    FAILED_TO_RESOLVE_WORKFLOW_DATA(0x27),
    FAILED_TO_RETRIEVE_WORKFLOW_HISTORY_LIST(0X28),
    WORKFLOW_MARKED_DELETED(0x29),
    FAILED_TO_RESOLVE_EMAIL_PLACEHOLDER(0x30),
    FAILED_TO_RESOLVE_WHATSAPP_PLACEHOLDER(0x31),
    ;

    private final int code;

    WorkflowErrorCodes(final int localCode) {
        this.code = WorkflowErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class WorkflowErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new WorkflowErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x75_0_0001;
        }
    }
}
