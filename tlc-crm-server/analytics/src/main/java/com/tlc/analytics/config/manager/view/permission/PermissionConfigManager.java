package com.tlc.analytics.config.manager.view.permission;

import com.tlc.analytics.config.model.view.share.PermanentLink;
import com.tlc.analytics.config.model.view.share.ViewPermission;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;

/**
 * <p>
 * Provides abstract way to handle the permission config api
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public interface PermissionConfigManager
{
    /**
     * <p>
     * Gets the {@link PermanentLink} instance
     * </p>
     *
     * @return The {@link PermanentLink}
     */
    default AnalyticsConfigManager<PermanentLink> getPermanentLinkManager()
    {
        return PermanentLinkManager.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link ViewPermission} instance
     * </p>
     *
     * @return The {@link ViewPermission}
     */
    default AnalyticsConfigManager<ViewPermission> getViewPermissionManager()
    {
        return ViewPermissionManager.INSTANCE();
    }
}
