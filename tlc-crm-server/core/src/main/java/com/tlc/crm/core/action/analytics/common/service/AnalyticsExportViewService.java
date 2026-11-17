package com.tlc.crm.core.action.analytics.common.service;

import com.tlc.analytics.config.action.AnalyticsRequest;

import lombok.NonNull;

/**
 * Service interface for handling view-related operations.
 * <p>
 * This interface provides methods to retrieve view-related metadata,
 * such as the view name, based on the provided analytics request.
 * </p>
 *
 * @author Kathir
 * @version 1.0
 */
public interface AnalyticsExportViewService
{

    /**
     * Retrieves the name of a view based on the provided request and view ID.
     *
     * @param request The analytics request containing metadata and context. Must not be null.
     * @param viewId  The unique identifier of the view.
     * @return The name of the view as a non-null string.
     */
    @NonNull String getViewName( final @NonNull AnalyticsRequest request,
                                 final @NonNull long viewId );
}
