package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.trash.CRMDashboardTrashManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMReportsViewType;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutPosition;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.trash.CRMDashboardTrash;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMFavourite;

/**
 * <p>
 * The interface defines methods for view config manager for dashboard layout
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
public interface CRMViewLayoutManagerAPI
{
    /**
     * <p>
     * Gets the {@link CRMViewConfigManager} instance
     * </p>
     *
     * @return The {@link CRMDashboardManager} instance
     */
    default CRMViewConfigManager<CRMDashboardDetails> getCRMDashboardManager()
    {
        return CRMDashboardManager.INSTANCE();
    }

    default CRMViewConfigManager<CRMChildViewDetails> getCRMChildViewManager()
    {
        return CRMChildViewManager.INSTANCE();
    }

    default CRMViewConfigManager<CRMANAViewMapper> getCRMANAViewManager()
    {
        return CRMANAViewManager.INSTANCE();
    }

    default CRMViewConfigManager<CRMReportsViewType> getCRMReportsViewTypeManager()
    {
        return CRMReportViewTypeManager.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link CRMViewConfigManager} instance
     * </p>
     *
     * @return The {@link CRMFavouriteManager} instance
     */
    default CRMViewConfigManager<CRMFavourite> getCRMFavouriteManager()
    {
        return CRMFavouriteManager.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link CRMDashboardTrash} manager instance
     * </p>
     *
     * @return The {@link CRMDashboardTrashManager}
     */
    default CRMViewConfigManager<CRMDashboardTrash> getCRMDashboardTrashManager()
    {
        return CRMDashboardTrashManager.getInstance();
    }

    default CRMViewConfigManager<DashboardLayoutPosition> getDashboardLayoutManager() {
        return DashboardLayoutPositionManager.INSTANCE();
    }
}
