package com.tlc.analytics.config.manager.view;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.fav.Favourite;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.ReportField;

/**
 * <p>
 * Provides abstract way to handle the view configuration
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public interface ViewConfigManager
{
    /**
     * Provides an instance of the {@link AnalyticsConfigManager} for {@link ViewDetails}.
     *
     * @return an {@link AnalyticsConfigManager} instance responsible for managing {@link ViewDetails}.
     */
    default AnalyticsConfigManager<ViewDetails> getViewDetailsManager()
    {
        return ViewDetailsManager.INSTANCE();
    }

    /**
     * Provides an instance of the {@link AnalyticsConfigManager} for {@link ReportField}.
     *
     * @return an {@link AnalyticsConfigManager} instance responsible for managing {@link ReportField}.
     */
    default AnalyticsConfigManager<ReportField> getReportFieldManager()
    {
        return ReportFieldManager.INSTANCE();
    }

    /**
     * Provides an instance of the {@link AnalyticsConfigManager} for {@link ChartView}.
     *
     * @return an {@link AnalyticsConfigManager} instance responsible for managing {@link ChartView}.
     */
    default AnalyticsConfigManager<ChartView> getChartViewManager()
    {
        return ChartViewManager.INSTANCE();
    }

    /**
     * Provides an instance of the {@link AnalyticsConfigManager} for {@link Favourite}.
     *
     * @return an {@link AnalyticsConfigManager} instance responsible for managing {@link Favourite}.
     */
    default AnalyticsConfigManager<Favourite> getFavouriteManager()
    {
        return FavouriteManager.INSTANCE();
    }

    /**
     * Provides an instance of the {@link AnalyticsConfigManager} for {@link KPIView}.
     *
     * @return an {@link AnalyticsConfigManager} instance responsible for managing {@link KPIView}.
     */
    default AnalyticsConfigManager<KPIView> getKPIManager()
    {
        return KPIViewManager.INSTANCE();
    }

}
