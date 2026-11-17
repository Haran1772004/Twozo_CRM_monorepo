package com.tlc.analytics.core.api.internal.general.view.response;

import com.tlc.analytics.config.keys.view.filter.FilterFieldKeys;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.FilterDataRequest;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Represents a response containing filter data.
 *
 * @author KarthickV
 * @version 1.0
 */
@Getter
@SuperBuilder
public class FilterDataResponse implements ViewResponse<FilterDataRequest>
{
    @NonNull
    private final List<GraphAxis<Object>> graphAxes;

    /**
     * {@inheritDoc}
     *
     * @param request The request object containing parameters for generating the response.
     * @return A JsonObject representing the JSON response.
     */
    @Override
    public @NonNull JsonObject getJsonResponse( final @NonNull FilterDataRequest request )
    {
        final JsonObject dataJson = Json.object();
        final JsonArray rowArray = Json.array();

        for( final GraphAxis<Object> graphAxis : graphAxes )
        {
            rowArray.put( graphAxis.getValue() );
        }

        return dataJson.put( FilterFieldKeys.VALUES, rowArray );
    }

    /**
     * {@inheritDoc}
     *
     * @param request The request object containing parameters to fetch the graph axis.
     * @return A list of GraphAxis objects representing the axes of the graph.
     */
    @Override
    public @NonNull List<GraphAxis<Object>> getGraphAxis( final @NonNull FilterDataRequest request )
    {
        return graphAxes;
    }
}
