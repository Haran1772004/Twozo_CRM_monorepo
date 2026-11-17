package com.tlc.crm.core.api.deal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

/**
 * Model which represents the Pipeline Stage
 *
 * @author Sundar
 * @version 1.0
 */
public class PipelineStage implements TlcModel {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.pipelineStage.invalid.notnull.id")
    @Min(value = 1, groups = {Delete.class, Update.class}, message = "i18n.pipelineStage.invalid.id")
    private Long id;

    @AuditEntityName
    @AuditField(aliasName = "stage", displayName = "i18n.deal.fields.pipeline.stage.name")
    @SearchField(name = "name")
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.pipelineStage.invalid.name")
    @NotBlank(groups = {Create.class, Update.class}, message = "i18n.pipelineStage.invalid.name")
    @Size(max = 250, groups = {Create.class, Update.class}, message = "i18n.pipelineStage.name.limit.exceeded")
    private String name;

    @NotNull(groups = {Create.class, Update.class, Delete.class}, message = "i18n.pipelineStage.invalid.notnull.orgId")
    @Min(value = 1, groups = {Create.class, Update.class, Delete.class}, message = "i18n.pipelineStage.invalid.orgId")
    private Long orgId;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.pipelineStage.invalid.notnull.pipelineId")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "i18n.pipelineStage.invalid.pipelineId")
    private Long pipelineId;

    private Short order;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.pipelineStage.invalid.notnull.probability")
    @Range(min = 0, max = 100, groups = {Create.class, Update.class}, message = "i18n.pipelineStage.invalid.probability")
    private Double probability;

    private boolean isDefault;

    @Override
    public Long id() {
        return id;
    }

    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(Long pipelineId) {
        this.pipelineId = pipelineId;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public Short getOrder() {
        return order;
    }

    public void setOrder(Short order) {
        this.order = order;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * <p>
     * Represents the stage types
     * </p>
     */
    public enum StageCategory {

        NEW((short) 1),
        WON((short) 2),
        LOST((short) 3);

        private final short id;

        StageCategory(final short id) {
            this.id = id;
        }

        /**
         * <p>
         * Gets the stage type id
         * </p>
         *
         * @return the type id
         */
        public short getId() {
            return this.id;
        }

        /**
         * Gets the {@link StageCategory} by id
         *
         * @param id represents the id of the stage type
         * @return {@link StageCategory}
         */
        public static StageCategory valueOf(final short id) {
            return switch (id) {
                case 1 -> NEW;
                case 2 -> WON;
                case 3 -> LOST;
                default -> throw ErrorCode.get(CoreErrorCodes.INVALID_STAGE_CATEGORY);
            };
        }
    }
}