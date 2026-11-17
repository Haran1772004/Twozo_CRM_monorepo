package com.tlc.crm.workflow.api.model;

import com.tlc.crm.entity.models.type.Email;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import com.tlc.crm.workflow.validator.annotation.WorkflowValidator;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * A model that represents the {@link Workflow} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WorkflowValidator(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.validation.failed")
public final class Workflow implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.workflow.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.ownerId.required")
    private Long ownerId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.name.required")
    @Size(max = 100, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.name.size.exceeds")
    private String name;

    @Size(max = 255, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.description.size.exceeds")
    private String description;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.trigger.source.required")
    private TriggerSource triggerSource;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.trigger.event.required")
    private TriggerEvent triggerEvent;

    @Size(max = 2, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.criteria.set.size.exceeds")
    @Valid
    private Collection<WorkflowCriteriaSet> criteriaSets;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.action.required")
    @Size(max = 5, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.action.size.exceeds")
    @Valid
    private Collection<WorkflowAction> actions;

    private boolean isActive;
    private Long templateId;
    private DateTime createdTime;
    private DateTime modifiedTime;
    private int totalActions;
    private Collection<Long> userIds;

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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
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

    public TriggerSource getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(final TriggerSource triggerSource) {
        this.triggerSource = triggerSource;
    }

    public TriggerEvent getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(final TriggerEvent triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public void addCriteriaSet(final WorkflowCriteriaSet workflowCriteriaSet) {
        this.criteriaSets = Objects.isNull(this.criteriaSets) ? new ArrayList<>() : this.criteriaSets;

        this.criteriaSets.add(workflowCriteriaSet);
    }

    public Collection<WorkflowCriteriaSet> getCriteriaSets() {
        return criteriaSets;
    }

    public void setCriteriaSets(final Collection<WorkflowCriteriaSet> criteriaSets) {
        this.criteriaSets = criteriaSets;
    }

    public Collection<WorkflowAction> getActions() {
        return actions;
    }

    public void setActions(final Collection<WorkflowAction> actions) {
        this.actions = actions;
    }

    public void addAction(final WorkflowAction workflowAction) {
        this.actions = Objects.isNull(this.actions) ? new ArrayList<>() : this.actions;

        this.actions.add(workflowAction);
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

    public int getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(final int totalActions) {
        this.totalActions = totalActions;
    }

    public Collection<Long> getUserIds() {
        return this.userIds == null ? new HashSet<>() : this.userIds;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    public void addUserId(final Long userId) {
        this.userIds = this.userIds == null ? new HashSet<>() : this.userIds;

        this.userIds.add(userId);
    }
}
