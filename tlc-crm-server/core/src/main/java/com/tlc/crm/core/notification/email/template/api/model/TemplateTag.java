package com.tlc.crm.core.notification.email.template.api.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

import com.tlc.crm.email.notification.email.EmailNotificationModel;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>
 * A model represents the {@link TemplateTag}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TemplateTag implements EmailNotificationModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    private DateTime lastUsedTime;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.email.template.tag.invalid.name")
    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.template.tag.name.max.length")
    private String name;

    private DateTime updatedTime;

    private DateTime createdTime;

    private Long ownerId;

    private CrmUser createdBy;

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setLastUsedTime(final DateTime lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public DateTime getLastUsedTime() {
        return lastUsedTime;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
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
