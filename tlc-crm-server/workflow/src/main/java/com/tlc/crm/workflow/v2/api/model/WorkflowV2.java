package com.tlc.crm.workflow.v2.api.model;

import com.tlc.crm.workflow.v2.validator.annotation.WorkflowV2Validator;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a workflow V2 definition.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
@WorkflowV2Validator(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.validation.failed")
public final class WorkflowV2 implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.workflow.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.ownerId.required")
    private Long ownerId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.name.required")
    @Size(max = 100, groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.name.size.exceeds")
    private String name;

    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.description.size.exceeds")
    private String description;

    @Valid
    private WorkflowVersion workflowVersion;

    @Valid
    private Map<String, WorkflowNode> nodes;

    private Long templateId;
    private boolean isActive;
    private boolean cancelPendingExecution;
    private boolean graphUpdate;
    private DateTime createdTime;
    private DateTime modifiedTime;
    private short triggerSourceId;
    private Map<Long, String> idToClientKey;

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

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public Map<String, WorkflowNode> getNodes() {
        return nodes;
    }

    public void setNodes(final Map<String, WorkflowNode> nodes) {
        this.nodes = nodes;
    }

    public Collection<WorkflowNode> getNodesAsCollection() {
        return Objects.isNull(nodes) ? new ArrayList<>() : new ArrayList<>(nodes.values());
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public boolean isCancelPendingExecution() {
        return cancelPendingExecution;
    }

    public void setCancelPendingExecution(final boolean cancelPendingExecution) {
        this.cancelPendingExecution = cancelPendingExecution;
    }

    public boolean isGraphUpdate() {
        return graphUpdate;
    }

    public void setGraphUpdate(final boolean graphUpdate) {
        this.graphUpdate = graphUpdate;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public WorkflowVersion getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(final WorkflowVersion workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public Map<Long, String> getIdToClientKey() {
        return idToClientKey;
    }

    public void setIdToClientKey(final Map<Long, String> idToClientKey) {
        this.idToClientKey = idToClientKey;
    }

    public short getTriggerSourceId() {
        return triggerSourceId;
    }

    public void setTriggerSourceId(final short triggerSourceId) {
        this.triggerSourceId = triggerSourceId;
    }

}