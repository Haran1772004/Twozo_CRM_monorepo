package com.tlc.crm.workflow.api.model.history;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * Holds the workflow {@link WorkflowExecutionStatus}
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum WorkflowExecutionStatus {

    IN_PROGRESS((short) 1, "i18n.workflow.execution.status.in.progress"),
    SUCCESS((short) 2, "i18n.workflow.execution.status.success"),
    FAILURE((short) 3, "i18n.workflow.execution.status.failure");

    private final short id;
    private final String name;

    WorkflowExecutionStatus(final short id, final String name) {
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
     * <p>
     * Gets the status of the history
     * </p>
     *
     * @param id Refers the workflow execution status id
     * @return The {@link WorkflowExecutionStatus}
     */
    public static WorkflowExecutionStatus get(final short id) {
        return switch (id) {
            case 1 -> IN_PROGRESS;
            case 2 -> SUCCESS;
            case 3 -> FAILURE;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_WORKFLOW_EXECUTION_STATUS, "i18n.workflow.execution.status.invalid");
        };
    }

    /**
     * <p>
     * Holds the {@link WorkflowActionExecutionStatus} of workflow
     * </p>
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    public enum WorkflowActionExecutionStatus {

        SUCCESS((short) 1, "i18n.workflow.execution.action.status.success"),
        FAILURE((short) 2, "i18n.workflow.execution.action.status.failure");

        private final short id;
        private final String name;

        WorkflowActionExecutionStatus(final short id, final String name) {
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
         * <p>
         * Gets the workflow execution action status for the given id
         * </p>
         *
         * @param id Refers the workflow execution action status id
         * @return The {@link WorkflowActionExecutionStatus}
         */
        public static WorkflowActionExecutionStatus get(final short id) {
            return switch (id) {
                case 1 -> SUCCESS;
                case 2 -> FAILURE;
                default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_WORKFLOW_EXECUTION_ACTION_STATUS, "i18n.workflow.execution.action.status.invalid");
            };
        }
    }

    /**
     * <p>
     * Holds the {@link WorkflowCriteriaExecutionStatus} of workflow
     * </p>
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    public enum WorkflowCriteriaExecutionStatus {

        PASSED((short) 1, "i18n.workflow.execution.criteria.status.success"),
        FAILED((short) 2, "i18n.workflow.execution.criteria.status.failed");

        private final short id;
        private final String name;

        WorkflowCriteriaExecutionStatus(final short id, final String name) {
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
         * <p>
         * Gets the criteria status for the given id
         * </p>
         *
         * @param id Refers the criteria status id
         * @return The {@link WorkflowCriteriaExecutionStatus}
         */
        public static WorkflowCriteriaExecutionStatus get(final short id) {
            return switch (id) {
                case 1 -> PASSED;
                case 2 -> FAILED;
                default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_WORKFLOW_EXECUTION_CRITERIA_STATUS, "i18n.workflow.execution.criteria.status.invalid");
            };
        }
    }
}