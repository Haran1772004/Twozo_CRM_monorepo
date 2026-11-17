package com.tlc.crm.workflow.api.model.history;

import com.tlc.i18n.I18nKey;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowActionExecutionStatus;

/**
 * <p>
 * Represents the {@link WorkflowActionExecutionHistory} details
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowActionExecutionHistory {

    private long orgId;
    private Long executionHistoryId;
    private Long actionId;
    private Long executedTime;
    private WorkflowActionExecutionFailureCause failureCause;
    private I18nKey actionName;
    private WorkflowActionExecutionStatus actionExecutionStatus;

    public long getOrgId() {
        return orgId;
    }

    public Long getExecutionHistoryId() {
        return executionHistoryId;
    }

    public Long getActionId() {
        return actionId;
    }

    public Long getExecutedTime() {
        return executedTime;
    }

    public WorkflowActionExecutionFailureCause getFailureCause() {
        return failureCause;
    }

    public WorkflowActionExecutionStatus getActionExecutionStatus() {
        return actionExecutionStatus;
    }

    public I18nKey getActionName() {
        return actionName;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setExecutionHistoryId(final Long executionHistoryId) {
        this.executionHistoryId = executionHistoryId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }

    public void setExecutedTime(final Long executedTime) {
        this.executedTime = executedTime;
    }

    public void setFailureCause(final WorkflowActionExecutionFailureCause failureCause) {
        this.failureCause = failureCause;
    }

    public void setActionExecutionStatus(final WorkflowActionExecutionStatus actionExecutionStatus) {
        this.actionExecutionStatus = actionExecutionStatus;
    }

    public void setActionName(final I18nKey actionName) {
        this.actionName = actionName;
    }
}