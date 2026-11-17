package com.tlc.crm.core.action.analytics.common.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.i18n.I18nResolver;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.UnderlyingDataRequest;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;

import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Service interface for retrieving graph axis data.
 *
 * <p>Implementations of this interface provide methods to fetch graph axis data
 * based on the given analytics request, action type, and additional request parameters.</p>
 *
 * @author Kathir
 * @version 1.0
 */
public interface ExportDataRetrievalService
{
    /**
     * Retrieves the graph axis data based on the provided request details.
     *
     * @param analyticsRequest The analytics request containing required parameters.
     * @param actionType       The type of action being performed (e.g., FETCH, EXPORT).
     * @param requestData      Additional request parameters in JSON format.
     * @param i18nResolver     Internationalization resolver for localized content.
     * @param reportFieldMap   Pre-fetched report field map.
     */
    @NonNull List<GraphAxis<Object>> fetchData( final @NonNull AnalyticsRequest analyticsRequest,
                                                final @NonNull ActionType actionType,
                                                final @NonNull JsonObject requestData,
                                                final @NonNull I18nResolver i18nResolver,
                                                final Map<Long, ReportModularField> reportFieldMap );

    /**
     * Pre-builds the {@link UnderlyingDataRequest} for an underlying export once, so the expensive
     * construction (field mappings, path resolution, etc.) is not repeated for every export chunk.
     *
     * @param analyticsRequest The analytics request containing required parameters.
     * @param actionType       The type of action being performed.
     * @param requestData      Additional request parameters in JSON format.
     * @param i18nResolver     Internationalization resolver for localized content.
     * @param reportFieldMap   Pre-fetched report field map.
     * @return The fully-constructed {@link UnderlyingDataRequest} with no limit applied.
     */
    @NonNull
    UnderlyingDataRequest buildUnderlyingRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                                 final @NonNull ActionType actionType,
                                                 final @NonNull JsonObject requestData,
                                                 final @NonNull I18nResolver i18nResolver,
                                                 final Map<Long, ReportModularField> reportFieldMap);

    /**
     * Retrieves graph axis data for one export chunk using an already-built {@link UnderlyingDataRequest}.
     * Only the pagination window ({@code start}/{@code count}) changes between chunks.
     *
     * @param builtRequest The pre-built underlying data request.
     * @param start        1-based start row index for this chunk.
     * @param count        Number of rows to fetch for this chunk.
     * @return A list of {@link GraphAxis} objects for the requested chunk.
     */
    @NonNull
    List<GraphAxis<Object>> fetchData(final @NonNull UnderlyingDataRequest builtRequest,
                                      final int start,
                                      final int count);
}
