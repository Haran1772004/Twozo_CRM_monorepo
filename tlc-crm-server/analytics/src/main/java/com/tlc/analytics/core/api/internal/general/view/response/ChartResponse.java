package com.tlc.analytics.core.api.internal.general.view.response;

import com.tlc.analytics.config.model.view.report.chart.GraphType;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.internal.general.view.response.graph.GraphResponse;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Represents a response containing chart data.
 *
 * @author Jamesh
 * @version 1.0
 */
@Getter
@SuperBuilder
public class ChartResponse implements ViewResponse<ChartRequest>
{
    @NonNull
    private final List<GraphAxis<Object>> graphAxes;

    /**
     * Represents a response containing chart data.
     */
    @NotNull
    @Override
    public @NonNull JsonObject getJsonResponse( final @NonNull ChartRequest request )
    {
        final GraphType graphType   = request.getView().getGraphType();

        switch ( graphType )
        {
            case PIE ->
            {
                return GraphResponse.create().getJson( request, graphAxes, true, true, null, null );
            }
            case BAR ->
            {
                return GraphResponse.create().getJson( request, graphAxes, true, false, null, null );
            }
            case LINE ->
            {
                return GraphResponse.create().getJson( request, graphAxes, false, false, null, null );
            }
        }

        return Json.object();
    }

    @Override
    public @NonNull List<GraphAxis<Object>> getGraphAxis( final @NonNull ChartRequest request )
    {
        return graphAxes;
    }
}
