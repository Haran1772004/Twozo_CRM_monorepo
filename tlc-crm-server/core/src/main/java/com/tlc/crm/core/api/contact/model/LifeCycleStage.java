package com.tlc.crm.core.api.contact.model;

import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the life cycle stage
 * </p>
 */
public class LifeCycleStage implements TlcModel {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.lifecycle.stage.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @AuditEntityName
    @AuditField(aliasName = "name", displayName = "i18n.contact.fields.stage")
    @Size(min = 3, max = 100, groups = {Create.class, Update.class}, message = "i18n.contact.stage.invalid.name")
    private String name;

    @Valid
    @Size(min = 1, max = 10, groups = {Create.class, Update.class}, message = "i18n.contact.stage.status.limit.invalid")
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.contact.status.invalid")
    private Collection<LifeCycleStatus> statuses;

    private Short order;

    private boolean isLastStage;

    private boolean isSystemStage;

    private boolean isActive;

    @Override
    public Long id() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Collection<LifeCycleStatus> getStatuses() {
        return statuses;
    }

    public void addStatus(final LifeCycleStatus status) {
        statuses = Objects.isNull(statuses) ? new ArrayList<>() : statuses;

        statuses.add(status);
    }

    public void setStatuses(final Collection<LifeCycleStatus> statuses) {
        this.statuses = statuses;
    }

    public Short getOrder() {
        return order;
    }

    public void setOrder(final Short order) {
        this.order = order;
    }

    public boolean isSystemStage() {
        return isSystemStage;
    }

    public void setSystemStage(final boolean systemStage) {
        isSystemStage = systemStage;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public boolean isLastStage() {
        return isLastStage;
    }

    public void setLastStage(final boolean lastStage) {
        isLastStage = lastStage;
    }

    @Override
    public Object identity() {
        return null;
    }
}
