package com.tlc.crm.core.features.api.quota.model;

import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;

import java.util.Objects;

/**
 * A model that represents the quota forecast category override progress details
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaValueOverrideProgress {

    private Long id;
    private Long value;
    private Long orgId;
    private Long overrideValue;
    private ForecastCategory forecastCategory;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getValue() {
        return Objects.isNull(value) ? 0L : value;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setValue(final Long value) {
        this.value = value;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(final Long overrideValue) {
        this.overrideValue = overrideValue;
    }

    public ForecastCategory getForecastCategory() {
        return forecastCategory;
    }

    public void setForecastCategory(final ForecastCategory forecastCategory) {
        this.forecastCategory = forecastCategory;
    }
}
