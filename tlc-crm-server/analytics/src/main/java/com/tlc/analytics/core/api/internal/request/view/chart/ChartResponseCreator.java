package com.tlc.analytics.core.api.internal.request.view.chart;

import com.tlc.analytics.config.model.view.report.ChartView;

import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.internal.general.view.response.ChartResponse;
import com.tlc.analytics.core.api.query.ReportsQueryConverter;
import com.tlc.analytics.core.api.query.SelectQueryConverter;
import com.tlc.analytics.core.api.response.AbstractResponseCreator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;


/**
 * Creates chart responses.
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
class ChartResponseCreator extends AbstractResponseCreator<ChartView, ChartRequest, ViewResponse<ChartRequest>>
{
    private static class Instance
    {
        private static final ChartResponseCreator INSTANCE = new ChartResponseCreator();
    }

    public static ChartResponseCreator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param graphAxisList The list of graph axes to be included in the response.
     * @return The generated response object.
     */
    @Override
    protected @NonNull ViewResponse<ChartRequest> getResponse( final @NonNull List<GraphAxis<Object>> graphAxisList )
    {
        return ChartResponse.builder().graphAxes( graphAxisList ).build();
    }

    /**
     * {@inheritDoc}
     *
     * @return The select query converter instance.
     */
    @Override
    protected @NonNull SelectQueryConverter<ChartView, ChartRequest> getSelectQueryConverter()
    {
        return ReportsQueryConverter.INSTANCE()
                                    .getChartQueryConverter();
    }
}
