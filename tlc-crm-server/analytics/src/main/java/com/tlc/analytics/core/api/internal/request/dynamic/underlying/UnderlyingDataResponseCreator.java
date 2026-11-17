package com.tlc.analytics.core.api.internal.request.dynamic.underlying;

import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsUnderlyingData;

import com.tlc.analytics.core.api.UnderlyingDataRequest;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.internal.general.view.response.UnderlyingResponse;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.query.ReportsQueryConverter;
import com.tlc.analytics.core.api.query.SelectQueryConverter;
import com.tlc.analytics.core.api.response.AbstractResponseCreator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * <p>
 * Manages the response creator for the underlying data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class UnderlyingDataResponseCreator extends AbstractResponseCreator<AnalyticsUnderlyingData, UnderlyingDataRequest, ViewResponse<UnderlyingDataRequest>>
{
    private static class Instance
    {
        private static final UnderlyingDataResponseCreator INSTANCE = new UnderlyingDataResponseCreator();
    }

    public static UnderlyingDataResponseCreator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param graphAxisList The list of graph axes to be included in the response.
     * @return The generated response object.
     */
    @NonNull
    @Override
    protected final ViewResponse<UnderlyingDataRequest> getResponse( final @NonNull List<GraphAxis<Object>> graphAxisList )
    {
        return UnderlyingResponse.builder()
                                 .graphAxes( graphAxisList )
                                 .build();
    }

    /**
     * {@inheritDoc}
     *
     * @return The select query converter instance.
     */
    @NonNull
    @Override
    protected final SelectQueryConverter<AnalyticsUnderlyingData, UnderlyingDataRequest> getSelectQueryConverter()
    {
        return ReportsQueryConverter.INSTANCE()
                                    .getUnderlyingQueryConverter();
    }
}
