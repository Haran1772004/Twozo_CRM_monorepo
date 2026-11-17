package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents workflow V2 execution runtime status.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public enum ExecutionStatus {

    IN_PROGRESS((short) 1, "i18n.workflow.execution.status.in.progress"),
    SUCCESS((short) 2, "i18n.workflow.execution.status.success"),
    STOPPED((short) 3, "i18n.workflow.execution.status.stopped"),
    PENDING((short) 4, "i18n.workflow.execution.status.pending"),
    FAILED((short) 5, "i18n.workflow.execution.status.failure"),
    ;

    private final short id;
    private final String displayName;

    ExecutionStatus(final short id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public short getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the execution status for the given ID.
     *
     * @param id The ID.
     * @return The execution status.
     */
    public static ExecutionStatus get(final short id) {
        return switch (id) {
            case 1 -> IN_PROGRESS;
            case 2 -> SUCCESS;
            case 3 -> STOPPED;
            case 4 -> PENDING;
            case 5 -> FAILED;
            default ->
                    throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_EXECUTION_STATUS, "i18n.workflow.invalid.execution.status");
        };
    }
}
