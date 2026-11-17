package com.tlc.analytics.config.action.api.view;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.fav.Favourite;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the favourite view configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/fav" )
public class FavouriteConfigAction extends AnalyticsConfigAction<Favourite>
{
    @Override
    protected @NonNull Class<Favourite> getModelClass() {
        return Favourite.class;
    }

    @Override
    public ConfigManager<Favourite> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getFavouriteManager();
    }
}
