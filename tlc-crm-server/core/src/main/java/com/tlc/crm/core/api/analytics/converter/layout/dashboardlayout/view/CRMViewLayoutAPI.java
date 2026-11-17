package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view;

import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;

import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.CRMViewConfigAPI;

/**
 * <p>
 * The interface defines methods for retrieving CRM view configurations for both chart and KPI views.
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
public interface CRMViewLayoutAPI
{
    /**
     * <p>
     * Gets the {@link ChartView} instance
     * </p>
     *
     * @return The {@link CRMChartView} instance
     */
    default CRMViewConfigAPI<ChartView> getChartConfigAPI()
    {
        return CRMChartView.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link KPIView} instance
     * </p>
     *
     * @return The {@link CRMKPIView} instance
     */
    default CRMViewConfigAPI<KPIView> getKPIConfigAPI()
    {
        return CRMKPIView.INSTANCE();
    }
}
