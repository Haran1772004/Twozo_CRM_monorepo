package com.tlc.crm.auto.assignment.api.model;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.auto.assignment.validator.annotation.AutoAssignmentValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A model that represents the {@link AutoAssignmentRule} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@AutoAssignmentValidator(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.validation.failed")
public final class AutoAssignmentRule implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.auto.assignment.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.auto.assignment.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.auto.assignment.createdBy.required")
    private CrmUser createdBy;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.updatedBy.required")
    private CrmUser updatedBy;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.auto.assignment.name.required")
    @Size(max = 100, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.auto.assignment.name.size.exceeds")
    private String name;

    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.description.size.exceeds")
    private String description;

    @Size(max = 10, groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.criteria.set.size.exceeds")
    @Valid
    private Collection<AutoAssignmentCriteriaSet> criteriaSets;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.rule.required")
    @Valid
    private Collection<AssignmentRuleUser> assignmentRuleUsers;

    private boolean isActive;
    private Long templateId;
    private DateTime createdTime;
    private DateTime modifiedTime;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.module.required")
    private AssignmentModule assignmentModule;
    private short order;

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

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public void addCriteriaSet(final AutoAssignmentCriteriaSet autoAssignmentCriteriaSet) {
        this.criteriaSets = Objects.isNull(this.criteriaSets) ? new ArrayList<>() : this.criteriaSets;

        this.criteriaSets.add(autoAssignmentCriteriaSet);
    }

    public Collection<AutoAssignmentCriteriaSet> getCriteriaSets() {
        return criteriaSets;
    }

    public void setCriteriaSets(final Collection<AutoAssignmentCriteriaSet> criteriaSets) {
        this.criteriaSets = criteriaSets;
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

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Collection<AssignmentRuleUser> getAssignmentRuleUsers() {
        return assignmentRuleUsers;
    }

    public void setAssignmentRuleUsers(final Collection<AssignmentRuleUser> assignmentRuleUsers) {
        this.assignmentRuleUsers = assignmentRuleUsers;
    }

    public AssignmentModule getAssignmentModule() {
        return assignmentModule;
    }

    public void setAssignmentModule(final AssignmentModule assignmentModule) {
        this.assignmentModule = assignmentModule;
    }

    public void addAssignmentRuleUser(final AssignmentRuleUser assignmentRuleUser) {
        this.assignmentRuleUsers = Objects.isNull(this.assignmentRuleUsers) ? new ArrayList<>() : this.assignmentRuleUsers;

        this.assignmentRuleUsers.add(assignmentRuleUser);
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }
}
