package com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.fav.Favourite;

/**
 * Retrieve the configuration manager for {@link Favourite} objects.
 */
public interface CRMFav
{
    /**
     * Retrieves the configuration manager for {@link Favourite} objects.
     *
     * @return an instance of {@link AnalyticsConfigManager} for managing {@link Favourite} configurations.
     */
    default AnalyticsConfigManager<Favourite> getFav()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getFavouriteManager();
    }
}
