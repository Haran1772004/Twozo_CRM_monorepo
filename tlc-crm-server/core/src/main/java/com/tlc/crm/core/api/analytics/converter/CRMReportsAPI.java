package com.tlc.crm.core.api.analytics.converter;

import com.tlc.crm.core.api.analytics.converter.data.filter.CRMFilterDataAPI;
import com.tlc.crm.core.api.analytics.converter.data.underlying.CRMUnderlyingDataAPI;
import com.tlc.crm.core.api.analytics.converter.data.view.CRMViewDataAPI;
import com.tlc.crm.core.api.analytics.converter.general.meta.fields.CRMReportFieldsMetaAPI;
import com.tlc.crm.core.api.analytics.converter.general.meta.view.CRMViewTypeMetaAPI;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.CRMDashboardLayoutAPI;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.CRMViewLayoutAPI;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.share.DashboardShareManager;
import com.tlc.crm.core.api.analytics.converter.template.templatedetails.CRMTemplateDataAPI;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class to access the Dashboard, Chart, KPI, Template, Analytics Layout Meta, View Data.
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMReportsAPI implements CRMDashboardLayoutAPI,
                                      CRMViewLayoutAPI,
                                      CRMReportFieldsMetaAPI,
                                      CRMViewTypeMetaAPI,
                                      CRMFilterDataAPI,
                                      CRMViewDataAPI,
                                      CRMUnderlyingDataAPI,
                                      DashboardShareManager,
                                      CRMTemplateDataAPI
{
    private static class Instance
    {
        private static final CRMReportsAPI INSTANCE = new CRMReportsAPI();
    }

    public static CRMReportsAPI getInstance() {
        return Instance.INSTANCE;
    }
}
