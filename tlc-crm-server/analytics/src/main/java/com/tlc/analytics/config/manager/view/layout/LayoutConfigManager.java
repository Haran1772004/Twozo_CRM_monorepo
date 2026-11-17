package com.tlc.analytics.config.manager.view.layout;

import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;

/**
 * <p>
 * Provides abstract way to handle the layout config api
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public interface LayoutConfigManager
{
    /**
     * <p>
     * Gets the {@link DashboardParentView} instance
     * </p>
     *
     * @return The {@link DashboardParentView}
     */
    default AnalyticsConfigManager<DashboardParentView> getDashboardParentViewManager()
    {
        return DashboardParentViewManager.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link DashboardLayout} instance
     * </p>
     *
     * @return The {@link DashboardLayout}
     */
    default AnalyticsConfigManager<DashboardLayout> getDashboardLayoutManager()
    {
        return DashboardLayoutManager.INSTANCE();
    }
}
