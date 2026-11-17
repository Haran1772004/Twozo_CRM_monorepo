package com.tlc.analytics.core.api.internal.request.dynamic.filter;

import com.tlc.analytics.config.model.view.report.dynamic.filter.AnalyticsFilterData;
import com.tlc.analytics.core.api.FilterDataRequest;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.internal.general.view.response.FilterDataResponse;
import com.tlc.analytics.core.api.query.ReportsQueryConverter;
import com.tlc.analytics.core.api.query.SelectQueryConverter;
import com.tlc.analytics.core.api.response.AbstractResponseCreator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * Creates the filter data response.
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FilterDataResponseCreator extends AbstractResponseCreator<AnalyticsFilterData, FilterDataRequest, ViewResponse<FilterDataRequest>>
{

    private static class Instance
    {
        private static final FilterDataResponseCreator INSTANCE = new FilterDataResponseCreator();
    }

    public static FilterDataResponseCreator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param graphAxisList The list of graph axes to be included in the response.
     * @return The generated response object.
     */
    @NonNull
    @Override
    protected final ViewResponse<FilterDataRequest> getResponse( final @NonNull List<GraphAxis<Object>> graphAxisList )
    {
        return FilterDataResponse.builder()
                                 .graphAxes( graphAxisList )
                                 .build();
    }

    /**
     * {@inheritDoc}
     *
     * @return The select query converter instance.
     */
    @NonNull
    @Override
    protected final SelectQueryConverter<AnalyticsFilterData, FilterDataRequest> getSelectQueryConverter()
    {
        return ReportsQueryConverter.INSTANCE()
                                    .getFilterQueryConverter();
    }
}
