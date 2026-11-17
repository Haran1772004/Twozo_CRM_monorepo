package com.tlc.analytics.core.api.internal.request.dynamic.underlying;

import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsUnderlyingData;
import com.tlc.analytics.core.api.UnderlyingDataRequest;
import com.tlc.analytics.core.api.response.ResponseProvider;

/**
 * <p>
 * Provides the abstract way to manage the response provider for the underlying data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface UnderlyingDataResponseProviderAPI
{
    /**
     * Gets the instance of {@link UnderlyingResponseProvider}.
     *
     * @return {@link ResponseProvider}.
     */
    default ResponseProvider<AnalyticsUnderlyingData, UnderlyingDataRequest> getUnderlyingDataResponseProvider()
    {
        return UnderlyingResponseProvider.INSTANCE();
    }
}
