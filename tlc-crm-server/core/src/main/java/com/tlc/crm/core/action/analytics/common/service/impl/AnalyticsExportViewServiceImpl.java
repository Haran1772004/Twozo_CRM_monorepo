package com.tlc.crm.core.action.analytics.common.service.impl;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.crm.core.action.analytics.common.service.AnalyticsExportViewService;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Implementation of the {@link AnalyticsExportViewService} interface.
 * <p>
 * This class provides functionality to retrieve view-related metadata,
 * such as the view name, based on the provided analytics request.
 *
 * @author Kathir
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class AnalyticsExportViewServiceImpl implements AnalyticsExportViewService
{

    private static class Instance
    {
        private static final AnalyticsExportViewServiceImpl INSTANCE = new AnalyticsExportViewServiceImpl();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link AnalyticsExportViewServiceImpl} singleton
     */
    public static AnalyticsExportViewServiceImpl getInstance()
    {
        return Instance.INSTANCE;
    }

    /**
     * Retrieves the name of a view based on the provided request and view ID.
     *
     * @param request The analytics request containing metadata and context. Must not be null.
     * @param viewId  The unique identifier of the view.
     * @return The name of the view as a non-null string.
     */
    @Override
    public @NonNull String getViewName( final @NonNull AnalyticsRequest request,
                                        final long viewId )
    {
        return CRMViewManagerAPI.INSTANCE()
                                .getCRMChildViewManager()
                                .get( request.orgId(), viewId )
                                .getName();
    }
}
