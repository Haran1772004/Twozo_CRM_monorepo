package com.tlc.analytics.config.action.api.view;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.Action;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Manages the chart view configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/chart_view" )
public class ChartViewAction extends AnalyticsConfigAction<ChartView>
{
    @Override
    protected @NonNull Class<ChartView> getModelClass()
    {
        return ChartView.class;
    }

    @Override
    public ConfigManager<ChartView> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getChartViewManager();
    }

    @Override
    protected boolean isValidDetails( final @NonNull Long orgId,
                                      final @NonNull Long userId,
                                      final @NonNull Action action,
                                      final @NonNull Collection<ChartView> models )
    {
        return true;
    }
}
