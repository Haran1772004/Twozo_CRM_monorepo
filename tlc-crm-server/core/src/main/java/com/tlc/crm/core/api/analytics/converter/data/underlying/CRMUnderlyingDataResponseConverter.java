package com.tlc.crm.core.api.analytics.converter.data.underlying;

import com.tlc.analytics.core.api.AnalyticsResponseProvider;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.UnderlyingDataRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * Provides different ways to convert the underlying data into json object
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class CRMUnderlyingDataResponseConverter
{
    private static class Instance
    {
        private static final CRMUnderlyingDataResponseConverter INSTANCE = new CRMUnderlyingDataResponseConverter();
    }

    public static CRMUnderlyingDataResponseConverter INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Retrieves the JSON response for the given underlying data request.
     *
     * @param underlyingDataRequest the request containing the underlying data parameters.
     * @return The list of {@link GraphAxis} representing the response from the analytics provider.
     */
    List<GraphAxis<Object>> getGraphAxisResponse(final @NonNull UnderlyingDataRequest underlyingDataRequest ) throws Exception
    {
        return AnalyticsResponseProvider.INSTANCE()
                                        .getUnderlyingDataResponseProvider()
                                        .getGraphAxisResponse( underlyingDataRequest );
    }

}
