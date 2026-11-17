package com.tlc.analytics.core.api.internal.request.view.chart;

import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.creator.ViewResponseCreator;
import com.tlc.analytics.core.api.internal.ReportResponseProvider;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.commons.json.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Provides chart responses.
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
class ChartResponseProvider extends ReportResponseProvider<ChartView, ChartRequest, ViewResponse<ChartRequest>>
{
    private static class Instance
    {
        private static final ChartResponseProvider INSTANCE = new ChartResponseProvider();
    }

    public static ChartResponseProvider INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     *{@inheritDoc}
     *
     * @return The view response creator. Must not be null.
     */
    @Override
    protected @NonNull ViewResponseCreator<ChartView, ChartRequest, ViewResponse<ChartRequest>> getResponseCreator()
    {
        return ChartResponseCreator.INSTANCE();
    }

    /**
     * {@inheritDoc}
     *
     * @param response The view response to generate the JSON object from. Must not be null.
     * @param request  The view request for which to generate the cache key.
     * @return The generated response JSON object. Must not be null.
     */
    @Override
    protected @NonNull JsonObject generateResponse( final ViewResponse<ChartRequest> response,
                                                    final ChartRequest request  )
    {
        return response.getJsonResponse( request );
    }

    /**
     * {@inheritDoc}
     *
     * @param response The view response to generate the JSON object from.
     * @param request  The view request for which to generate the cache key.
     * @return List of {@link GraphAxis} objects representing the generated axes for the graph
     */
    @Override
    protected @NonNull List<GraphAxis<Object>> generateGraphAxis( final ViewResponse<ChartRequest> response,
                                                                  final ChartRequest request )
    {
        return response.getGraphAxis( request );
    }

    /**
     * {@inheritDoc}
     *
     * @param chartRequest The view request for which to generate the cache key. Must not be null.
     * @return The generated cache key. Must not be null.
     */
    @Override
    protected @NonNull String generateCacheKey( final ChartRequest chartRequest )
    {
        return "";
    }
}
