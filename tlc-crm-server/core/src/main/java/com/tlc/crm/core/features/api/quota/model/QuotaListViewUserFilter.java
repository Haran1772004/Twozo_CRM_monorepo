package com.tlc.crm.core.features.api.quota.model;

import java.util.Collection;

/**
 * Model that represents the {@link QuotaListViewUserFilter}
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaListViewUserFilter {

    private Long id;
    private Long userId;
    private Long orgId;
    private Collection<Long> selectedUserIds;
    private Collection<Long> selectedTeamIds;
    private QuotaMetric quotaMetric;
    private QuotaDateInterval quotaDateInterval;
    private ForecastType forecastType;
    private String listType;

    public Collection<Long> getSelectedTeamIds() {
        return selectedTeamIds;
    }

    public void setTeamIds(final Collection<Long> selectedTeamIds) {
        this.selectedTeamIds = selectedTeamIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Collection<Long> getSelectedUserIds() {
        return selectedUserIds;
    }

    public void setSelectedUserIds(final Collection<Long> selectedUserIds) {
        this.selectedUserIds = selectedUserIds;
    }

    public QuotaMetric getQuotaMetric() {
        return quotaMetric;
    }

    public void setQuotaMetric(final QuotaMetric quotaMetric) {
        this.quotaMetric = quotaMetric;
    }

    public QuotaDateInterval getQuotaDateInterval() {
        return quotaDateInterval;
    }

    public void setQuotaDateInterval(final QuotaDateInterval quotaDateInterval) {
        this.quotaDateInterval = quotaDateInterval;
    }

    public ForecastType getForecastType() {
        return forecastType;
    }

    public void setForecastType(final ForecastType forecastType) {
        this.forecastType = forecastType;
    }

    public String getListType() {
        return listType;
    }

    public void setListType(final String listType) {
        this.listType = listType;
    }
}
