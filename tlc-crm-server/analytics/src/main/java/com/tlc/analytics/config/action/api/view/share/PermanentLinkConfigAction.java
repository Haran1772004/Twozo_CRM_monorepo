package com.tlc.analytics.config.action.api.view.share;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.share.PermanentLink;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the sharing permanent view link configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/permanent/link" )
public class PermanentLinkConfigAction extends AnalyticsConfigAction<PermanentLink>
{
    @Override
    protected @NonNull Class<PermanentLink> getModelClass()
    {
        return PermanentLink.class;
    }

    @Override
    public ConfigManager<PermanentLink> getConfigManager()
    {
        return  AnalyticsConfigManagerImpl.INSTANCE()
                                          .getPermanentLinkManager();
    }
}
