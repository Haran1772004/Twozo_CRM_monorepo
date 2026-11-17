package com.tlc.analytics.core.api.response;

import com.tlc.analytics.config.model.view.ViewModel;
import com.tlc.analytics.core.api.ViewRequest;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.commons.json.JsonObject;
import lombok.NonNull;

import java.util.List;

/**
 * Interface defining a response provider for a specific type of view request.
 *
 * @param <Req> The type of view request for which responses are provided.
 *
 * @author Aswath
 * @version 1.0
 */
public interface ResponseProvider<V extends ViewModel, Req extends ViewRequest<V>>
{
    /**
     * Retrieves the response JSON object for the given view request.
     *
     * @param request The view request for which to retrieve the response. Must not be null.
     * @return The response JSON object.
     * @throws Exception if an error occurs while generating the response.
     */
    @NonNull JsonObject getJsonResponse( final @NonNull Req request ) throws Exception;

    @NonNull
    List<GraphAxis<Object>> getGraphAxisResponse(final @NonNull Req request ) throws Exception;
}
