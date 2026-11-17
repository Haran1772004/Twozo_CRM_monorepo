package com.tlc.analytics.config.action.api.view.layout;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the dashboard parent view configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/view-layout" )
public class DashboardParentViewConfigAction extends AnalyticsConfigAction<DashboardParentView>
{
    @Override
    protected @NonNull Class<DashboardParentView> getModelClass()
    {
        return DashboardParentView.class;
    }

    @Override
    public ConfigManager<DashboardParentView> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getDashboardParentViewManager();
    }
}
