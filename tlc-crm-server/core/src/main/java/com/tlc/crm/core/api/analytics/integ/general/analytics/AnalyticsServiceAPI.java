package com.tlc.crm.core.api.analytics.integ.general.analytics;

import com.tlc.crm.core.api.analytics.integ.general.analytics.fields.config.CRMViewFieldAPI;
import com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config.CRMDashboardLayout;
import com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config.CRMFav;
import com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config.CRMViewDetailsService;
import com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config.CRMViewLayout;
import com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config.CRMViewPermissionService;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Provides access to various analytics services in the CRM system
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class AnalyticsServiceAPI implements CRMDashboardLayout,
                                            CRMFav,
                                            CRMViewLayout,
                                            CRMViewFieldAPI,
                                            CRMViewDetailsService,
                                            CRMViewPermissionService
{
    private static class Instance
    {
        private static final AnalyticsServiceAPI INSTANCE = new AnalyticsServiceAPI();
    }

    public static AnalyticsServiceAPI INSTANCE()
    {
        return Instance.INSTANCE;
    }
}
