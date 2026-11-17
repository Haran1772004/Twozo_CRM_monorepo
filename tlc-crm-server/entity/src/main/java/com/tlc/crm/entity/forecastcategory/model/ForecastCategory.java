package com.tlc.crm.entity.forecastcategory.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.i18n.I18nKey;
import com.tlc.validator.TlcModel;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * A model that represents the forecast category
 *
 * @author kishore J
 * @version 1.0
 */
public class ForecastCategory implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.common.entity.invalid.id")
    private Long id;
    @NotNull(groups = {Group.Create.class}, message = "i18n.entity.forecast.category.name.required")
    @Size(max = 50, groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.forecast.category.name.invalid.length")
    private String name;
    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.forecast.category.description.invalid.length")
    private String description;
    private Long orgId;
    private boolean isEditable;
    private boolean isDeletable;
    private boolean isDefault;
    private boolean isBestCase;
    private CrmUser createdBy;
    private CrmUser updatedBy;
    private I18nKey displayName;

    public ForecastCategory() {
    }

    public ForecastCategory(final JsonObject configJson) {
        this.isEditable = configJson.getBoolean(ForecastCategoryFields.IS_EDITABLE);
        this.isDeletable = configJson.getBoolean(ForecastCategoryFields.IS_DELETABLE);
        this.isBestCase = configJson.optBoolean(ForecastCategoryFields.IS_BEST_CASE);
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEditable() {
        return isEditable;
    }

    public boolean isDeletable() {
        return isDeletable;
    }

    public boolean isBestCase() {
        return isBestCase;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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
        throw ErrorCode.get(EntityErrorCodes.UNSUPPORTED_ACTION);
    }

    public JsonObject buildConfigJson() {

        if (!(isEditable || isDeletable || isBestCase)) {
            return null;
        }
        final JsonObject configJson = Json.object();

        configJson.put(ForecastCategoryFields.IS_EDITABLE, isEditable);
        configJson.put(ForecastCategoryFields.IS_DELETABLE, isDeletable);
        configJson.put(ForecastCategoryFields.IS_BEST_CASE, isBestCase);
        return configJson;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isCommittedCategory() {
        return Objects.equals(name, DefaultForecastCategory.COMMITTED.getName());
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }
}
