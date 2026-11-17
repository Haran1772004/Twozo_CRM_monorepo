package com.tlc.analytics.core.api.internal.request.view.kpi;

import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.core.api.KPIRequest;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.creator.ViewResponseCreator;
import com.tlc.analytics.core.api.internal.ReportResponseProvider;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.json.JsonObject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * <p>
 * Manages the response provider for the KPI
 * </p>
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class KPIResponseProvider extends ReportResponseProvider<KPIView, KPIRequest, ViewResponse<KPIRequest>>
{

    private static class Instance
    {
        private static final KPIResponseProvider INSTANCE = new KPIResponseProvider();
    }

    public static KPIResponseProvider INSTANCE()
    {
        return Instance.INSTANCE;
    }


    @NonNull
    @Override
    protected final ViewResponseCreator<KPIView, KPIRequest, ViewResponse<KPIRequest>> getResponseCreator()
    {
        return KPIResponseCreator.INSTANCE();
    }

    /**
     * {@inheritDoc}
     *
     * @param response The view response to generate the JSON object from. Must not be null.
     * @param request  The view request for which to generate the cache key.
     * @return The generated response JSON object. Must not be null.
     */
    @NonNull
    @Override
    protected final JsonObject generateResponse( final @NonNull ViewResponse<KPIRequest> response,
                                                 final @NonNull KPIRequest request )
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
    @NonNull
    @Override
    protected final List<GraphAxis<Object>> generateGraphAxis( final @NonNull ViewResponse<KPIRequest> response,
                                                               final @NonNull KPIRequest request )
    {
        return response.getGraphAxis( request );
    }

    /**
     * {@inheritDoc}
     *
     * @param request The view request for which to generate the cache key. Must not be null.
     * @return The generated cache key. Must not be null.
     */
    @NonNull
    @Override
    protected final String generateCacheKey( final @NonNull KPIRequest request )
    {
        return "";
    }
}
