package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.clone;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;

import com.tlc.pk.copy.CopyManager;
import com.tlc.pk.copy.CopyManagerFactory;
import com.tlc.pk.copy.EntityCopyConfig;

import com.tlc.sql.common.api.DataContainer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Implements the {@link AnalyticsCopyHandler} interface to manage the copying of {@link CRMDashboardDetails}
 * entities. This class handles the creation of a new entity instance, updating its fields as needed,
 * and persisting the copied entity. Uses a {@link CopyManager} for the actual copying logic.
 *
 * <p>This class follows the singleton design pattern to ensure a single instance is used.</p>
 *
 * @author KarthickV
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PROTECTED )
class AnalyticsCopyHandlerImpl implements AnalyticsCopyHandler
{
    private final CopyManager<EntityCopyConfig<Long, CRMDashboardDetails>> copyManager = CopyManagerFactory.getEntityCopyManager();

    /**
     * A private static class to hold the singleton instance of {@link AnalyticsCopyHandlerImpl}.
     * This ensures thread-safe, lazy initialization.
     */
    private static class Instance
    {
        private static final AnalyticsCopyHandlerImpl INSTANCE = new AnalyticsCopyHandlerImpl();
    }

    /**
     * Provides the singleton instance of {@link AnalyticsCopyHandlerImpl}.
     *
     * @return The singleton instance of {@link AnalyticsCopyHandlerImpl}.
     */
    public static AnalyticsCopyHandlerImpl INSTANCE()
    {
        return AnalyticsCopyHandlerImpl.Instance.INSTANCE;
    }

    /**
     * Clones a dashboard by delegating the operation to the {@link CopyManager}.
     *
     * @param dashboardCopyConfig The configuration for copying the dashboard, including source
     *                            and target details. Must not be null.
     * @param dataContainer    Holds the rows that will get committed
     * @throws Exception If any error occurs during the cloning process.
     */
    public void cloneDashboard( final @NonNull EntityCopyConfig<Long, CRMDashboardDetails> dashboardCopyConfig, final DataContainer dataContainer ) throws Exception
    {
        copyManager.copy( dashboardCopyConfig, dataContainer );
    }
}
