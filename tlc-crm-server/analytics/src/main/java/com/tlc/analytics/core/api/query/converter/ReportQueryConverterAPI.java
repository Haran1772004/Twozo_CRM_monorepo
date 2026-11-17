package com.tlc.analytics.core.api.query.converter;

import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.dynamic.filter.AnalyticsFilterData;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsUnderlyingData;
import com.tlc.analytics.core.api.internal.DataViewRequest;
import com.tlc.analytics.core.api.FilterDataRequest;
import com.tlc.analytics.core.api.UnderlyingDataRequest;
import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.KPIRequest;
import com.tlc.analytics.core.api.query.SelectQueryConverter;

import com.tlc.sql.common.api.dml.SelectQuery;

/**
 * Implementation of {@link ReportQueryConverterAPI} for converting a {@link DataViewRequest} into a {@link SelectQuery}.

 * @author Jamesh
 * @version 1.0
 */
public interface ReportQueryConverterAPI
{
    /**
     * <p>
     * Gets the {@link ChartQueryConverter} instance
     * </p>
     *
     * @return The {@link ChartQueryConverter} instance
     */
    default SelectQueryConverter<ChartView, ChartRequest> getChartQueryConverter()
    {
        return ChartQueryConverter.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link KPIQueryConverter} instance
     * </p>
     *
     * @return The {@link KPIQueryConverter} instance
     */
    default SelectQueryConverter<KPIView, KPIRequest> getKPIQueryConverter()
    {
        return KPIQueryConverter.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link FilterQueryConverter} instance
     * </p>
     *
     * @return The {@link FilterQueryConverter} instance
     */
    default SelectQueryConverter<AnalyticsFilterData, FilterDataRequest> getFilterQueryConverter()
    {
        return FilterQueryConverter.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link UnderlyingDataQueryConverter} instance
     * </p>
     *
     * @return The {@link UnderlyingDataQueryConverter} instance
     */
    default SelectQueryConverter<AnalyticsUnderlyingData, UnderlyingDataRequest> getUnderlyingQueryConverter()
    {
        return UnderlyingDataQueryConverter.INSTANCE();
    }
}
