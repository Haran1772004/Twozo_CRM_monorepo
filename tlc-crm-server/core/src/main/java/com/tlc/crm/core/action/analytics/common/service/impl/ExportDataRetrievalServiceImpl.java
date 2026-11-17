package com.tlc.crm.core.action.analytics.common.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.action.analytics.common.service.ExportDataRetrievalService;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.analytics.core.api.UnderlyingDataRequest;

import com.tlc.i18n.I18nResolver;

import com.tlc.analytics.config.keys.view.ViewKeys;
import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.core.api.GraphAxis;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton service implementation responsible for retrieving underlying graph data
 * used in analytics export and appropriate chart (chart, or KPI)
 * based on the provided request context and delegates the data retrieval accordingly.
 *
 * @author Saravanan
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ExportDataRetrievalServiceImpl implements ExportDataRetrievalService
{

    private static class Instance
    {
        private static final ExportDataRetrievalServiceImpl INSTANCE = new ExportDataRetrievalServiceImpl();
    }

    public static ExportDataRetrievalServiceImpl getInstance()
    {
        return Instance.INSTANCE;
    }

    /**
     * Retrieves the underlying graph axis data based on the request type and view context.
     *
     * @param analyticsRequest The request containing organization, user, and role details.
     * @param actionType       The action type being performed (e.g., EXPORT or VIEW).
     * @param requestData      JSON payload containing view configuration and identifiers.
     * @param i18nResolver     Resolver used to handle localized content and error messages.
     * @return A list of {@link GraphAxis} objects representing the graph's data axis.
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @Override
    public @NonNull List<GraphAxis<Object>> fetchData( final @NonNull AnalyticsRequest analyticsRequest,
                                                       final @NonNull ActionType actionType,
                                                       final @NonNull JsonObject requestData,
                                                       final @NonNull I18nResolver i18nResolver,
                                                       final Map<Long, ReportModularField> reportFieldMap )
    {
        final CRMReportsAPI reportsAPI = CRMReportsAPI.getInstance();

        if ( requestData.containsKey( CRMViewPropertyKeys.CHILD_VIEW_ID ) )
        {
            return reportsAPI.underlyingDataLayoutAPI()
                             .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap );
        }

        final int viewId = requestData.getInt( ViewKeys.VIEW_TYPE_ID );
        final CRMViewType viewType = CRMViewType.getCrmViewType( viewId );

        if (Objects.isNull( viewType ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_TYPE,
                                 "i18n.analytics.config.invalid.view.type" );
        }

        return CRMViewType.isChartType( viewType ) ? reportsAPI.getChartDataAPI()
                                                               .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap )
                                                   : reportsAPI.getKPIDataAPI()
                                                               .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap );
    }

    /**
     * Pre-builds the {@link UnderlyingDataRequest} from the given request context without applying any
     * pagination limit. Use this once per export operation and then call
     * {@link #fetchData(UnderlyingDataRequest, int, int)} per chunk to avoid rebuilding the request on
     * every iteration.
     *
     * @param analyticsRequest The request containing organization, user, and role details.
     * @param actionType       The action type being performed (e.g., EXPORT).
     * @param requestData      JSON payload containing view configuration and identifiers.
     * @param i18nResolver     Resolver used to handle localized content and error messages.
     * @return The fully-constructed {@link UnderlyingDataRequest} without a pagination limit.
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @Override
    public @NonNull UnderlyingDataRequest buildUnderlyingRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                                                 final @NonNull ActionType actionType,
                                                                 final @NonNull JsonObject requestData,
                                                                 final @NonNull I18nResolver i18nResolver,
                                                                 final Map<Long, ReportModularField> reportFieldMap) {
        return CRMReportsAPI.getInstance()
                .underlyingDataLayoutAPI()
                .buildUnderlyingRequest(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
    }

    /**
     * Retrieves graph axis data for one export chunk using an already-built {@link UnderlyingDataRequest}.
     * Only the pagination window ({@code start}/{@code count}) changes between chunks; the rest of the
     * request remains identical to what was built by {@link #buildUnderlyingRequest}.
     *
     * @param builtRequest The pre-built underlying data request.
     * @param start        1-based start row index for this chunk.
     * @param count        Number of rows to fetch for this chunk.
     * @return A list of {@link GraphAxis} objects for the requested chunk.
     */
    @Override
    public @NonNull List<GraphAxis<Object>> fetchData(final @NonNull UnderlyingDataRequest builtRequest,
                                                      final int start,
                                                      final int count) {
        return CRMReportsAPI.getInstance()
                .underlyingDataLayoutAPI()
                .getGraphAxis(builtRequest, start, count);
    }
}
