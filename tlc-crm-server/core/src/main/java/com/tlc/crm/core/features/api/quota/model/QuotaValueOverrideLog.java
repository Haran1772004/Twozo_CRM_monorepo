package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * A model that represents the quota forecast category override log details
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaValueOverrideLog implements TlcModel {

    @NotNull(groups = Group.Update.class, message = "i18n.quota.forecast.category.override.value.invalid")
    private String value;
    @NotNull(groups = Group.Update.class, message = "i18n.quota.invalid.org.id")
    @NotNull(groups = Group.Update.class, message = "i18n.quota.invalid.quota.id")
    private Long quotaId;
    @NotNull(groups = Group.Update.class, message = "i18n.quota.forecast.category.id.invalid")
    private Long forecastCategoryId;
    @NotNull(groups = Group.Update.class, message = "i18n.quota.invalid.created.user")
    private CrmUser overrideBy;

    private Long orgId;
    private Long addedAt;
    private Long id;
    private Long userId;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(final Long addedAt) {
        this.addedAt = addedAt;
    }

    public CrmUser getOverrideBy() {
        return overrideBy;
    }

    public void setOverrideBy(final CrmUser overrideBy) {
        this.overrideBy = overrideBy;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getQuotaId() {
        return quotaId;
    }

    public void setQuotaId(final Long quotaId) {
        this.quotaId = quotaId;
    }

    public Long getForecastCategoryId() {
        return forecastCategoryId;
    }

    public void setForecastCategoryId(final Long forecastCategoryId) {
        this.forecastCategoryId = forecastCategoryId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
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
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }

    public Long getUserId() {
        return userId;
    }
}
