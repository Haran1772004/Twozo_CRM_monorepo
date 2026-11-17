package com.tlc.analytics.config.action.api.view.underlying;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsUnderlyingData;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the underlying data configuration related web actions
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/underlying" )
public class UnderlyingConfigAction extends AnalyticsConfigAction<AnalyticsUnderlyingData>
{
    @Override
    protected @NonNull Class<AnalyticsUnderlyingData> getModelClass()
    {
        return AnalyticsUnderlyingData.class;
    }

    @Override
    public ConfigManager<AnalyticsUnderlyingData> getConfigManager()
    {
        return null;
    }
}
