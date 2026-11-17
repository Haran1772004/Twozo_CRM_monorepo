package com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.share.ViewPermission;

/**
 * Retrieve the configuration manager for {@link ViewPermission} objects
 */
public interface CRMViewPermissionService
{
    /**
     * Retrieves the configuration manager for {@link ViewPermission}.
     *
     * @return an instance of {@link AnalyticsConfigManager} for managing {@link ViewPermission} configurations.
     */
    default AnalyticsConfigManager<ViewPermission> getViewPermissionManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getViewPermissionManager();
    }
}
