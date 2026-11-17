package com.tlc.analytics.config.action.api.view.layout;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the dashboard layout configuration related web actions
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/dashboard-layout" )
public class DashboardLayoutConfigAction extends AnalyticsConfigAction<DashboardLayout>
{
    @Override
    protected @NonNull Class<DashboardLayout> getModelClass()
    {
        return DashboardLayout.class;
    }

    @Override
    public ConfigManager<DashboardLayout> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getDashboardLayoutManager();
    }
}
