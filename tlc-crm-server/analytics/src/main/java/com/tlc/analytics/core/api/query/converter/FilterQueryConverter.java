package com.tlc.analytics.core.api.query.converter;

import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.core.api.internal.DataViewRequest;
import com.tlc.analytics.core.api.ChartRequest;

import com.tlc.sql.common.api.dml.SelectQuery;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Implementation of {@link FilterQueryConverter} for converting a {@link DataViewRequest} into a {@link SelectQuery}.
 *
 * @param <V>       The type of report view associated with the request, e.g., {@link ChartView}.
 * @param <Request> The type of data view request, e.g., {@link ChartRequest}.
 * @author Jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FilterQueryConverter<V extends AnalyticsReport, Request extends DataViewRequest<V>>
      extends AbstractReportSelectQueryConverter<V, Request>
{
    private static class Instance
    {
        private static final FilterQueryConverter INSTANCE = new FilterQueryConverter();
    }

    public static FilterQueryConverter INSTANCE()
    {
        return Instance.INSTANCE;
    }
}
