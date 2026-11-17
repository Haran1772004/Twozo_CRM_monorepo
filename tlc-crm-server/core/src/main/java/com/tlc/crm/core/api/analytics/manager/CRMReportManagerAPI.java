package com.tlc.crm.core.api.analytics.manager;

import com.tlc.crm.core.api.analytics.manager.config.field.FieldManagerAPI;
import com.tlc.crm.core.api.analytics.manager.config.function.FunctionService;
import com.tlc.crm.core.api.analytics.manager.config.module.ModuleManagerAPI;
import com.tlc.crm.core.api.analytics.manager.config.timePeriod.TimePeriodManagerAPI;
import com.tlc.crm.core.api.analytics.manager.config.widget.WidgetManagerAPI;
import com.tlc.crm.core.api.analytics.manager.data.underlying.CRMFieldConfigManagerAPI;

import com.tlc.crm.core.api.analytics.manager.share.DashboardShareManagerService;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Manages CRM Manager API
 * </p>
 *
 * @author James
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMReportManagerAPI implements FieldManagerAPI,
                                       ModuleManagerAPI,
        TimePeriodManagerAPI,
                                       WidgetManagerAPI,
                                       CRMRIntegrationManagerAPI,
                                       CRMFieldConfigManagerAPI,
                                       FunctionService,
                                       DashboardShareManagerService
{
    private static class Instance
    {
        private static final CRMReportManagerAPI INSTANCE = new CRMReportManagerAPI();
    }

    /**
     * Returns singleton instance of report manager API facade.
     *
     * @return The {@link CRMReportManagerAPI} instance.
     */
    public static CRMReportManagerAPI INSTANCE()
    {
        return Instance.INSTANCE;
    }
}
