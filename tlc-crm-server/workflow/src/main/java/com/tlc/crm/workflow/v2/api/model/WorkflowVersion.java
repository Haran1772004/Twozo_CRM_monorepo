package com.tlc.crm.workflow.v2.api.model;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * Represents a workflow version.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public class WorkflowVersion implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.initial.node.required")
    private short initialNodeId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.version.status.required")
    private VersionStatus versionStatus;

    private Long workflowId;
    private int versionNumber;
    private DateTime publishedTime;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public short getInitialNodeId() {
        return initialNodeId;
    }

    public void setInitialNodeId(final short initialNodeId) {
        this.initialNodeId = initialNodeId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(final Long workflowId) {
        this.workflowId = workflowId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(final int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public VersionStatus getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(final VersionStatus versionStatus) {
        this.versionStatus = versionStatus;
    }

    public DateTime getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(final DateTime publishedTime) {
        this.publishedTime = publishedTime;
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
