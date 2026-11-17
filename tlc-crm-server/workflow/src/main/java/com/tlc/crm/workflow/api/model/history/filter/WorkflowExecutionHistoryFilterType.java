package com.tlc.crm.workflow.api.model.history.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * Represents the {@link WorkflowExecutionHistoryFilterType}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum WorkflowExecutionHistoryFilterType {

    WORKFLOWS((short) 1, "workflows"),
    DUE_DATE((short) 2, "dueDate"),
    STATUS((short) 3, "status");

    private final int id;
    private final String name;

    WorkflowExecutionHistoryFilterType(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the workflow execution history filter type for the given id
     * </p>
     *
     * @param id Refers the id of the workflow execution history filter type
     * @return The {@link WorkflowExecutionHistoryFilterType}
     */
    public static WorkflowExecutionHistoryFilterType get(final short id) {
        return switch (id) {
            case 1 -> WORKFLOWS;
            case 2 -> DUE_DATE;
            case 3 -> STATUS;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_WORKFLOW_EXECUTION_FILTER_TYPE, "i18n.workflow.execution.filter.type.invalid");
        };
    }
}
