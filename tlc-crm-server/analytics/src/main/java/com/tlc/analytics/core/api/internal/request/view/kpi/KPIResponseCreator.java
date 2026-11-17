package com.tlc.analytics.core.api.internal.request.view.kpi;

import com.tlc.analytics.config.model.view.report.KPIView;

import com.tlc.analytics.core.api.KPIRequest;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.internal.general.view.response.KPIResponse;
import com.tlc.analytics.core.api.query.ReportsQueryConverter;
import com.tlc.analytics.core.api.query.SelectQueryConverter;
import com.tlc.analytics.core.api.response.AbstractResponseCreator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * <p>
 * Manages the response creator for the KPI
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class KPIResponseCreator extends AbstractResponseCreator<KPIView, KPIRequest, ViewResponse<KPIRequest>>
{
    private static class Instance
    {
        private static final KPIResponseCreator INSTANCE = new KPIResponseCreator();
    }

    public static KPIResponseCreator INSTANCE()
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
    protected final ViewResponse<KPIRequest> getResponse( final @NonNull List<GraphAxis<Object>> graphAxisList )
    {
        return KPIResponse.builder()
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
    protected final SelectQueryConverter<KPIView, KPIRequest> getSelectQueryConverter()
    {
        return ReportsQueryConverter.INSTANCE()
                                    .getKPIQueryConverter();
    }
}
