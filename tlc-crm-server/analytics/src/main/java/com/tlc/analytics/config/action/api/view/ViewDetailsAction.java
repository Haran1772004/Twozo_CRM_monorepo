package com.tlc.analytics.config.action.api.view;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.ViewDetails;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the view details configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/view_details" )
public class ViewDetailsAction extends AnalyticsConfigAction<ViewDetails>
{
    @Override
    protected @NonNull Class<ViewDetails> getModelClass()
    {
        return ViewDetails.class;
    }

    @Override
    public ConfigManager<ViewDetails> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getViewDetailsManager();
    }
}
