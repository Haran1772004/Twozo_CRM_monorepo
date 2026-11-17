package com.tlc.analytics.core.api.query;

import com.tlc.analytics.config.model.view.ViewModel;
import com.tlc.analytics.core.api.ViewRequest;
import com.tlc.sql.common.api.dml.With;

import javax.annotation.Nonnull;

/**
 * Interface for converting view requests into select queries.
 *
 * @param <V> The type of view model associated with the request.
 * @param <Request> The type of view request that is converted into a select query.
 *
 * @author Aswath
 * @version 1.0
 */
public interface SelectQueryConverter<V extends ViewModel, Request extends ViewRequest<V>>
{
    /**
     * Converts a view request into a select query.
     *
     * @param viewRequest The view request object containing the parameters for the select query.
     * @return The generated select query based on the view request.
     * @throws Exception If any error occurs during the conversion process.
     */
    @Nonnull
    With convert(final @Nonnull Request viewRequest ) throws Exception;
}
