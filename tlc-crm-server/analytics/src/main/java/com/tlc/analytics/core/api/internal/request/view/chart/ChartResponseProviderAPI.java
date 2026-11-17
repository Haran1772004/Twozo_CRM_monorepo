package com.tlc.analytics.core.api.internal.request.view.chart;

import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.response.ResponseProvider;

/**
 * Represents an API for retrieving chart response providers.
 *
 * @author Aswath
 * @version 1.0
 */
public interface ChartResponseProviderAPI
{
    /**
     * Retrieves the chart response provider.
     *
     * @return The chart response provider.
     */
    default ResponseProvider<ChartView, ChartRequest> getChartResponseProvider()
    {
        return ChartResponseProvider.INSTANCE();
    }
}
