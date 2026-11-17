package com.tlc.analytics.core.api.creator;

import com.tlc.analytics.config.model.view.ViewModel;
import com.tlc.analytics.core.api.ViewRequest;
import com.tlc.analytics.core.api.ViewResponse;

import lombok.NonNull;

/**
 * Interface defining a view response creator responsible for generating a view response
 * from a given view request.
 *
 * @param <V> The type of view.
 * @param <Request> The type of view request for which responses are created.
 * @param <Response> The type of view response created.
 *
 * @author Aswath
 * @version 1.0
 */
public interface ViewResponseCreator<V extends ViewModel, Request extends ViewRequest<V>, Response extends ViewResponse<Request>>
{
    /**
     * Creates a view response from the given view request.
     *
     * @param request The view request for which to create the response. Must not be null.
     * @return The created view response.
     * @throws Exception if an error occurs while creating the response.
     */
    @NonNull
    Response create(final @NonNull Request request ) throws Exception;
}
