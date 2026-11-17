package com.tlc.crm.workflow.api.model.history;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionStatusFilter;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * Holds the reasons for action execution failure.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum WorkflowActionExecutionFailureCause {
    INVALID_DATA((short) 1, "i18n.workflow.execution.required.data.invalid"),
    UNIDENTIFIED_ERROR((short) 2, "i18n.workflow.execution.unidentified.error.encountered"),
    DB_OPERATION_FAILED((short) 3, "i18n.workflow.execution.database.error.encountered"),
    USER_EMAIL_NOT_SYNCED((short) 4, "i18n.workflow.execution.email.sender.address.not.synced"),
    EMAIL_RECIPIENTS_UNSUBSCRIBED((short) 5, "i18n.workflow.execution.email.recipients.unsubscribed"),
    FAILED_TO_RESOLVE_EMAIL_PLACEHOLDER((short) 6, "i18n.workflow.execution.email.placeholder.resolver.failed"),
    WHATSAPP_PHONE_NUMBER_INACTIVE((short) 7, "i18n.workflow.execution.whatsapp.phone.number.inactive"),
    WHATSAPP_INVALID_RECIPIENT_PHONE_NUMBER((short) 8, "i18n.workflow.execution.whatsapp.invalid.recipient.number"),
    FAILED_TO_RESOLVE_CONTACT_PLACEHOLDER((short) 9, "i18n.workflow.execution.contact.placeholder.resolve.failed"),
    ;

    private final short id;
    private final String reason;

    WorkflowActionExecutionFailureCause(final short id, final String reason) {
        this.id = id;
        this.reason = reason;
    }

    /**
     * Gets the id.
     *
     * @return id.
     */
    public short getId() {
        return id;
    }

    /**
     * Gets the cause for the failure.
     *
     * @return The reason.
     */
    public String getFailureCause() {
        return reason;
    }

    /**
     * <p>
     * Gets the {@link WorkflowActionExecutionFailureCause} for the given id
     * </p>
     *
     * @param id Refers the {@link WorkflowExecutionStatusFilter} id
     * @return The {@link WorkflowExecutionStatusFilter}
     */
    public static WorkflowActionExecutionFailureCause get(final short id) {
        return switch (id) {
            case 1 -> INVALID_DATA;
            case 2 -> UNIDENTIFIED_ERROR;
            case 3 -> DB_OPERATION_FAILED;
            case 4 -> USER_EMAIL_NOT_SYNCED;
            case 5 -> EMAIL_RECIPIENTS_UNSUBSCRIBED;
            case 6 -> FAILED_TO_RESOLVE_EMAIL_PLACEHOLDER;
            case 7 -> WHATSAPP_PHONE_NUMBER_INACTIVE;
            case 8 -> WHATSAPP_INVALID_RECIPIENT_PHONE_NUMBER;
            case 9 -> FAILED_TO_RESOLVE_CONTACT_PLACEHOLDER;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_WORKFLOW_EXECUTION_STATUS_FILTER, "i18n.workflow.execution.status.filter.invalid");
        };
    }
}
