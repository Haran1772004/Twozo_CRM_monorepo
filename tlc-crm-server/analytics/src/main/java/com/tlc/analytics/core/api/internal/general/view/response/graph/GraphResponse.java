package com.tlc.analytics.core.api.internal.general.view.response.graph;

import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.BiFunction;

/**
 * <p>
 * Generates a JSON representation of a graph response based on the given request and graph axis
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@Accessors(fluent = true)
@NoArgsConstructor( staticName = "create", access = AccessLevel.PUBLIC )
public class GraphResponse
{

    /**
     * Constructs a JSON object for the graph response by populating metadata and graph data.
     *
     * @param request            The ChartRequest containing the organization ID, view, and drill-down information.
     * @param graphAxis          List of GraphAxis objects representing the axes of the graph.
     * @param hasDefaultLegender Boolean indicating if a default legend is included.
     * @param hasDefaultSize     Boolean indicating if a default size is applied.
     * @param xTransformFunction Function to transform X-axis values.
     * @param yTransformFunction Function to transform Y-axis values.
     * @return A JsonObject containing the complete graph response, including metadata and data.
     * @throws NullPointerException if any of the required parameters are null.
     */
    public JsonObject getJson( final @NonNull ChartRequest request, final @NonNull List<GraphAxis<Object>> graphAxis,
                               final boolean hasDefaultLegender, final boolean hasDefaultSize,
                               final BiFunction<String, Object, Object> xTransformFunction,
                               final BiFunction<String, Object, Object> yTransformFunction )
    {
        final JsonObject jsonObject = Json.object();

        GraphMetaJson.create()
                     .setMetaJson( request.getOrgId(), request.getView()
                                                              .getGraphType(),
                                   graphAxis, request.isDrillDownApplied(), null, jsonObject );

        GraphDataJson.create()
                     .setDataJson( graphAxis, jsonObject );

        return jsonObject;
    }

}
