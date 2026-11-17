package com.tlc.crm.core.api.analytics.converter.data.filter;

import com.tlc.analytics.core.api.AnalyticsResponseProvider;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.FilterDataRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * It processes filter data requests by converting them into a structured JSON response format containing graph axis data.
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMFilterDataConverter
{
    private static class Instance
    {
        private static final CRMFilterDataConverter INSTANCE = new CRMFilterDataConverter();
    }

    public static CRMFilterDataConverter INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Retrieves the JSON response for the given underlying data request.
     *
     * @param filterDataRequest the request containing the filter data parameters.
     * @return The list of {@link GraphAxis} representing the response from the analytics provider.
     */
    List<GraphAxis<Object>> getGraphAxisResponse(final @NonNull FilterDataRequest filterDataRequest ) throws Exception
    {
        return AnalyticsResponseProvider.INSTANCE()
                .getFilterResponseProvider()
                .getGraphAxisResponse( filterDataRequest );
    }

}
