package com.tlc.crm.workflow.v2.api.model;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.workflow.api.model.TriggerSource;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * Runtime execution of a published workflow V2 version.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2Execution implements TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private Long versionId;
    private Long workflowId;
    private String workflowName;
    private ExecutionStatus status;
    private TriggerSource triggerSource;
    private DateTime startedTime;
    private DateTime completedTime;
    private CrmUser crmUser;
    private int totalActions;
    private Collection<WorkflowV2ExecutionNode> executionNodes;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(final Long versionId) {
        this.versionId = versionId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(final Long workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(final String workflowName) {
        this.workflowName = workflowName;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(final ExecutionStatus status) {
        this.status = status;
    }

    public TriggerSource getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(final TriggerSource triggerSource) {
        this.triggerSource = triggerSource;
    }

    public DateTime getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(final DateTime startedTime) {
        this.startedTime = startedTime;
    }

    public DateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(final DateTime completedTime) {
        this.completedTime = completedTime;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    public int getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(final int totalActions) {
        this.totalActions = totalActions;
    }

    public Collection<WorkflowV2ExecutionNode> getExecutionNodes() {
        return executionNodes;
    }

    public void setExecutionNodes(final Collection<WorkflowV2ExecutionNode> executionNodes) {
        this.executionNodes = executionNodes;
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
