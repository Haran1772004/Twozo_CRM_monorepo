package com.tlc.analytics.core.api.internal.request.dynamic.filter;

import com.tlc.analytics.config.model.view.report.dynamic.filter.AnalyticsFilterData;
import com.tlc.analytics.core.api.FilterDataRequest;
import com.tlc.analytics.core.api.response.ResponseProvider;

/**
 * Defines an API for providing a ResponseProvider that handles AnalyticsFilterData and FilterDataRequest
 *
 * @author jamesh
 * @version 1.0
 */
public interface FilterDataResponseProviderAPI
{
    default ResponseProvider<AnalyticsFilterData, FilterDataRequest> getFilterResponseProvider()
    {
        return FilterDataResponseProvider.INSTANCE();
    }
}
