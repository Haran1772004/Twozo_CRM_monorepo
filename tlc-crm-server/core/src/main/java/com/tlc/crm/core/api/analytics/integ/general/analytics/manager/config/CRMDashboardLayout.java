package com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;

/**
 * It responsible for managing the layout of {@link DashboardLayout}
 *
 * @author jamesh
 * @version 1.0
 */
public interface CRMDashboardLayout
{
    /**
     * Retrieves the configuration manager for {@link DashboardLayout} objects.
     *
     * @return an instance of `AnalyticsConfigManager` for managing `DashboardLayout` configurations.
     */
    default AnalyticsConfigManager<DashboardLayout> getDashboardLayout()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getDashboardLayoutManager();
    }

    /**
     * Retrieves the configuration manager for {@link DashboardParentView} objects.
     *
     * @return an instance of `AnalyticsConfigManager` for managing `DashboardParentView` configurations.
     */
    default AnalyticsConfigManager<DashboardParentView> getDashboardParentView()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getDashboardParentViewManager();
    }
}
