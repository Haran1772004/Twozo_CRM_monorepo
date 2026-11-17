package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Per-node runtime status for a workflow V2 execution.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum NodeExecutionStatus {

    IN_PROGRESS((short) 1, "i18n.workflow.node.execution.status.inprogress"),
    COMPLETED((short) 2, "i18n.workflow.node.execution.status.completed"),
    FAILED((short) 3, "i18n.workflow.node.execution.status.failed"),
    PENDING((short) 4, "i18n.workflow.node.execution.status.pending"),
    EXPIRED((short) 5, "i18n.workflow.node.execution.status.expired"),
    ;

    private final short id;
    private final String name;

    NodeExecutionStatus(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the node execution status for the given ID.
     *
     * @param id The ID.
     * @return The node execution status.
     */
    public static NodeExecutionStatus get(final short id) {
        return switch (id) {
            case 1 -> IN_PROGRESS;
            case 2 -> COMPLETED;
            case 3 -> FAILED;
            case 4 -> PENDING;
            case 5 -> EXPIRED;
            default ->
                    throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_EXECUTION_STATUS, "i18n.workflow.invalid.execution.status");
        };
    }
}
