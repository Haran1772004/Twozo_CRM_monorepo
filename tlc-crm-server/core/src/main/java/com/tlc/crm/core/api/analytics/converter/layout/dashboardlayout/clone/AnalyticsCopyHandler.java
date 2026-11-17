package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.clone;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;

import com.tlc.pk.copy.EntityCopyConfig;

import com.tlc.sql.common.api.DataContainer;

import lombok.NonNull;

/**
 * An interface defining the contract for analytics cloning operations.
 */
public interface AnalyticsCopyHandler
{
    /**
     * Clones a dashboard based on the provided copy configuration.
     *
     * @param dashboardCopyConfig The configuration for copying the dashboard, including
     *                            the source and target details. Must not be null.
     * @param dataContainer    Holds the rows that will get committed
     * @throws Exception If an error occurs during the cloning process.
     */
    void cloneDashboard( final @NonNull EntityCopyConfig<Long, CRMDashboardDetails> dashboardCopyConfig, final DataContainer dataContainer ) throws Exception;

    /**
     * Provides a singleton instance of the AnalyticsCloner implementation.
     *
     * @return The singleton instance of AnalyticsCloner.
     */
    static AnalyticsCopyHandler getInstance()
    {
        return AnalyticsCopyHandlerImpl.INSTANCE();
    }
}
