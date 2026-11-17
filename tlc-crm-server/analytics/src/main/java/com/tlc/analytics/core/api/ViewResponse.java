package com.tlc.analytics.core.api;

import com.tlc.commons.json.JsonObject;
import lombok.NonNull;

import java.util.List;

/**
 * Interface for handling responses related to views and graphs.
 *
 * @param <V> The type of request used to generate the response.
 * @author Jamesh
 * @version 1.0
 */
public interface ViewResponse<V>
{
    /**
     * Generates a JSON response based on the provided request.
     *
     * @param request The request object containing parameters for generating the response.
     * @return A JsonObject representing the JSON response.
     * @throws NullPointerException if the request parameter is null.
     */
    @NonNull JsonObject getJsonResponse( final V request );

    /**
     * Retrieves the graph axis information based on the provided request.
     *
     * @param request The request object containing parameters to fetch the graph axis.
     * @return A list of GraphAxis objects representing the axes of the graph.
     * @throws NullPointerException if the request parameter is null.
     */
    @NonNull List<GraphAxis<Object>> getGraphAxis( final V request );
}
