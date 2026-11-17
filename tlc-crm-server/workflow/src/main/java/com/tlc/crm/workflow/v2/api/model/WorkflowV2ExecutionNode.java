package com.tlc.crm.workflow.v2.api.model;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;

/**
 * Per-node execution row for workflow V2.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2ExecutionNode implements TlcModel {

    private Long id;
    private Long orgId;
    private Long executionId;
    private Long nodeId;
    private NodeExecutionStatus status;
    private DateTime startedTime;
    private DateTime completedTime;
    private Short failureReason;
    private NodeType nodeType;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(final Long executionId) {
        this.executionId = executionId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Long nodeId) {
        this.nodeId = nodeId;
    }

    public NodeExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(final NodeExecutionStatus status) {
        this.status = status;
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

    public Short getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(final Short failureReason) {
        this.failureReason = failureReason;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(final NodeType nodeType) {
        this.nodeType = nodeType;
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
