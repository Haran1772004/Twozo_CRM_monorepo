package com.tlc.crm.core.api.deal.model;

import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Model which represents the Pipeline
 *
 * @author Sundar
 * @version 1.0
 */
public class Pipeline implements TlcModel {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.pipeline.invalid.notnull.id")
    @Min(value = 1, message = "i18n.pipeline.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class, Delete.class}, message = "i18n.pipeline.invalid.notnull.orgId")
    @Min(value = 1, message = "i18n.pipeline.invalid.orgId")
    private Long orgId;

    @AuditEntityName
    @AuditField(aliasName = "pipeline", displayName = "i18n.deal.fields.pipeline.name")
    @SearchField(name = "name")
    @NotBlank(groups = {Create.class, Update.class}, message = "i18n.pipeline.invalid.name")
    private String name;

    @NotEmpty(groups = {Create.class, Update.class}, message = "i18n.pipeline.invalid.pipelineStages")
    private Collection<PipelineStage> pipelineStages;

    @Min(value = 1, message = "i18n.pipeline.invalid.rottenDays")
    private Short rottenDays;

    private Long totalStagesCount;

    private boolean isDefault;

    private boolean isSelected;

    private Short order;

    @Override
    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Collection<PipelineStage> getPipelineStages() {
        return pipelineStages;
    }

    public void setPipelineStages(Collection<PipelineStage> pipelineStages) {
        this.pipelineStages = pipelineStages;
    }

    public void addPipelineStages(PipelineStage pipelineStage) {
        this.pipelineStages = this.pipelineStages == null ? new ArrayList<>() : this.pipelineStages;

        this.pipelineStages.add(pipelineStage);
    }

    public Short getRottenDays() {
        return rottenDays;
    }

    public void setRottenDays(final Short rottenDays) {
        this.rottenDays = rottenDays;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Short getOrder() {
        return order;
    }

    public void setOrder(final Short order) {
        this.order = order;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(final boolean isSelected) {
        this.isSelected = isSelected;
    }

    public Long getTotalStagesCount() {
        return totalStagesCount;
    }

    public void setTotalStagesCount(final Long totalStagesCount) {
        this.totalStagesCount = totalStagesCount;
    }

    @Override
    public Object identity() {
        return null;
    }
}
