package com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.ViewDetails;

/**
 * Retrieve the configuration manager for {@link ViewDetails} objects
 *
 * @author james
 * @version 1.0
 */
public interface CRMViewDetailsService
{
    /**
     * Retrieves the configuration manager for {@link ViewDetails} objects.
     *
     * @return an instance of {@link AnalyticsConfigManager} for managing {@link ViewDetails} configurations.
     */
    default AnalyticsConfigManager<ViewDetails> getViewDetailsManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getViewDetailsManager();
    }
}
