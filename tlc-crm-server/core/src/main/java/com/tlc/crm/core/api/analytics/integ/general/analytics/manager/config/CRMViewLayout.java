package com.tlc.crm.core.api.analytics.integ.general.analytics.manager.config;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;

/**
 * Retrieve configuration managers for {@link ChartView} and {@link KPIView} objects
 *
 * @author james
 * @version 1.0
 */
public interface CRMViewLayout
{
    /**
     * Retrieves the configuration manager for {@link ChartView} layouts.
     *
     * @return an instance of {@link AnalyticsConfigManager} for managing {@link ChartView} configurations.
     */
    default AnalyticsConfigManager<ChartView> getChartLayout()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getChartViewManager();
    }

    /**
     * Retrieves the configuration manager for {@link KPIView} layouts.
     *
     * @return an instance of {@link AnalyticsConfigManager} for managing {@link KPIView} configurations.
     */
    default AnalyticsConfigManager<KPIView> getKPILayout()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getKPIManager();
    }
}
