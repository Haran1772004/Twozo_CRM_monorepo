package com.tlc.analytics.core.api.internal.general.view.response.graph;

import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * Represents a class for handling and setting graph data in JSON format
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( staticName = "create", access = AccessLevel.MODULE )
class GraphDataJson
{
    private final JsonArray dataJson = Json.array();

    /**
     * <p>
     * Sets the graph data JSON array based on the provided graph axes and graph JSON object.
     * </p>
     *
     * @param graphAxis the list of graph axes
     * @param graphJson the graph JSON object
     */
    public void setDataJson( final @NonNull List<GraphAxis<Object>> graphAxis, final JsonObject graphJson )
    {
        for( final GraphAxis<Object> graphAxiss : graphAxis )
        {
            final JsonArray jsonArray = Json.array();
            final JsonObject rowJson = Json.object();
            final List<AxisValue<Object>> axisValues = graphAxiss.getAxisValues();

            for( final AxisValue<Object> axisValue : axisValues )
            {
                jsonArray.put( axisValue.getValue() );
            }
            rowJson.put( graphAxiss.getReportField()
                                   .getAxis()
                                   .name()
                                   .toLowerCase(),
                         jsonArray );
            dataJson.put( rowJson );
        }
        graphJson.put("graphData", dataJson );
    }
}
