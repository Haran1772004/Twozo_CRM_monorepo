package com.tlc.analytics.config.action.api.view.share;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.share.ViewPermission;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the view permission configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/view-permission" )
public class ViewPermissionConfigAction extends AnalyticsConfigAction<ViewPermission>
{
    @Override
    protected @NonNull Class<ViewPermission> getModelClass()
    {
        return ViewPermission.class;
    }

    @Override
    public ConfigManager<ViewPermission> getConfigManager()
    {
        return  AnalyticsConfigManagerImpl.INSTANCE()
                                          .getViewPermissionManager();
    }
}
