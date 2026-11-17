package com.tlc.analytics.core.api;

import com.tlc.analytics.core.api.internal.request.dynamic.filter.FilterDataResponseProviderAPI;
import com.tlc.analytics.core.api.internal.request.dynamic.underlying.UnderlyingDataResponseProviderAPI;
import com.tlc.analytics.core.api.internal.request.view.chart.ChartResponseProviderAPI;
import com.tlc.analytics.core.api.internal.request.view.kpi.KPIResponseProviderAPI;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * API for providing responses related to Analytics.
 * Implements {@link ChartResponseProviderAPI}.
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
@Accessors( fluent = true )
public class AnalyticsResponseProvider implements ChartResponseProviderAPI,
                                                  KPIResponseProviderAPI,
                                                  FilterDataResponseProviderAPI,
                                                  UnderlyingDataResponseProviderAPI
{
    /**
     * Singleton instance of {@link AnalyticsResponseProvider}
     */
    private static class Instance
    {
        private static final AnalyticsResponseProvider INSTANCE = new AnalyticsResponseProvider();
    }

    public static AnalyticsResponseProvider INSTANCE()
    {
        return Instance.INSTANCE;
    }
}
