package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout;

import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.crm.core.api.analytics.converter.layout.CRMReportsLayout;

/**
 * Interface providing access to the CRM {@link DashboardLayout}
 *
 * @author jamesh
 * @version 1.0
 */
public interface CRMDashboardLayoutAPI
{
    default CRMReportsLayout<DashboardLayout> dashboardLayoutAPI(){ return CRMDashboardAPI.INSTANCE();}
}
