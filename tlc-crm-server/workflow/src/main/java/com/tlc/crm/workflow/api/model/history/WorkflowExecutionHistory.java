package com.tlc.crm.workflow.api.model.history;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Represents the {@link WorkflowExecutionHistory} attributes
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowExecutionHistory implements TlcModel {

    private Long id;
    private Long orgId;
    private Long workflowId;
    private DateTime executedTime;
    private DateTime conditionExecutedTime;
    private WorkflowExecutionStatus executionStatus;
    private Workflow workflow;
    private CrmUser crmUser;
    private WorkflowCriteriaExecutionStatus criteriaExecutionStatus;
    private Collection<WorkflowActionExecutionHistory> actionExecutionHistories;
    private JsonObject workflowState;

    public WorkflowExecutionHistory() {
    }

    public WorkflowExecutionHistory(final Long orgId, final Long workflowId) {
        this.orgId = orgId;
        this.workflowId = workflowId;
    }

    public JsonObject getWorkflowState() {
        return workflowState;
    }

    public void setWorkflowState(final JsonObject workflowState) {
        this.workflowState = workflowState;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public DateTime getExecutedTime() {
        return executedTime;
    }

    public DateTime getConditionExecutedTime() {
        return conditionExecutedTime;
    }

    public WorkflowCriteriaExecutionStatus getCriteriaExecutionStatus() {
        return criteriaExecutionStatus;
    }

    public Collection<WorkflowActionExecutionHistory> getActionExecutionHistories() {
        return actionExecutionHistories;
    }

    public WorkflowExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setExecutedTime(final DateTime executedTime) {
        this.executedTime = executedTime;
    }

    public void setConditionExecutedTime(final DateTime conditionExecutedTime) {
        this.conditionExecutedTime = conditionExecutedTime;
    }

    public void setCriteriaExecutionStatus(final WorkflowCriteriaExecutionStatus criteriaExecutionStatus) {
        this.criteriaExecutionStatus = criteriaExecutionStatus;
    }

    public void setActionExecutionHistories(final Collection<WorkflowActionExecutionHistory> actionExecutionHistories) {
        this.actionExecutionHistories = actionExecutionHistories;
    }

    public void setExecutionStatus(final WorkflowExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    public void setWorkflow(final Workflow workflow) {
        this.workflow = workflow;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}