package com.tlc.crm.workflow.api.model.history.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * Represents the {@link WorkflowExecutionStatusFilter}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum WorkflowExecutionStatusFilter {

    ALL((short) -1),
    SUCCESS((short) 1),
    FAILURE((short) 2),
    PENDING((short) 3),
    STOPPED((short) 4);

    private final short id;

    WorkflowExecutionStatusFilter(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the status of the workflow execution filter
     * </p>
     *
     * @param id Refers the workflow execution status filter id
     * @return The {@link WorkflowExecutionStatusFilter}
     */
    public static WorkflowExecutionStatusFilter get(final short id) {
        return switch (id) {
            case -1 -> ALL;
            case 1 -> SUCCESS;
            case 2 -> FAILURE;
            case 3 -> PENDING;
            case 4 -> STOPPED;
            default ->
                    throw ErrorCode.get(WorkflowErrorCodes.INVALID_WORKFLOW_EXECUTION_STATUS_FILTER, "i18n.workflow.execution.status.filter.invalid");
        };
    }
}
