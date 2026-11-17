package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.share;

/**
 * Interface providing access to the CRM {@link DashboardVisibilityService}
 *
 * @author venkatesh Nambiyar
 * @version 1.0
 */
public interface DashboardShareManager
{
    default DashboardVisibilityService getDashboardVisibility()
    {
        return DashboardShareManagerImpl.INSTANCE();
    }
}
