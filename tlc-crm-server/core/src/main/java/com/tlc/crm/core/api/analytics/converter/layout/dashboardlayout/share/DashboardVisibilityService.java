package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.share;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.json.JsonObject;

import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.DataContainer;

import lombok.NonNull;

/**
 * Provides an abstract way for managing dashboard visibility and sharing settings
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface DashboardVisibilityService
{
    /**
     * <p>
     * Shares the dashboard layout with specific visibility settings
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          A JSON object containing the sharing details
     * @param i18nResolver     An instance of {@link I18nResolver} for handling localization of messages
     * @return A {@link JsonObject} containing the response of the share dashboard
     **/
    @NonNull
    JsonObject share( final @NonNull AnalyticsRequest analyticsRequest,
                      final @NonNull JsonObject request,
                      final @NonNull I18nResolver i18nResolver );


    /**
     * <p>
     * Retrieves the visibility settings for a given dashboard
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param jsonObject       A JSON object containing request parameters, such as the dashboard id
     * @param i18nResolver     An instance of {@link I18nResolver} for handling localization of messages
     * @return A {@link JsonObject} containing the visibility details of the dashboard
     **/
    @NonNull
    JsonObject getShareVisibility( final @NonNull AnalyticsRequest analyticsRequest,
                                   final @NonNull JsonObject jsonObject,
                                   final @NonNull I18nResolver i18nResolver );

    /**
     * <p>
     * Creates the visibility configuration for a specific dashboard
     * </p>
     *
     * @param orgId        Represents an organisation id
     * @param userId       Represents the user id
     * @param dashboardId  Represents the dashboard id
     * @param dataContainer    Holds the rows that will get committed
     */
    void createVisibility( final @NonNull Long orgId,
                           final @NonNull Long userId,
                           final @NonNull Long dashboardId,
                           final DataContainer dataContainer );
}
