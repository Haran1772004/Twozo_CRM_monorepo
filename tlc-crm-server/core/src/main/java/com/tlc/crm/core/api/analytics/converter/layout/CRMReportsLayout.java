package com.tlc.crm.core.api.analytics.converter.layout;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

/**
 * {@link CRMReportsLayout} is an Interface, This class contains the view request category.
 *
 * @author jamesh
 * @version 1.0
 **/
public interface CRMReportsLayout<T>
{
    /**
     * Performs an action for create a Layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void create(final @NonNull AnalyticsRequest analyticsRequest,
                final @NonNull JsonObject request,
                final @NonNull I18nResolver i18nResolver,
                final @NonNull ActionType actionType);

    /**
     * Performs an action for update a Layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void update(final @NonNull AnalyticsRequest analyticsRequest,
                final @NonNull JsonObject request,
                final @NonNull I18nResolver i18nResolver,
                final @NonNull ActionType actionType);

    /**
     * Performs an action for get a Layout as response
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void processDashboardResponse(final @NonNull AnalyticsRequest analyticsRequest,
                                  final @NonNull JsonObject request,
                                  final @NonNull I18nResolver i18nResolver,
                                  final @NonNull ActionType actionType);

    /**
     * Performs an action for delete a Layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void delete(final @NonNull AnalyticsRequest analyticsRequest,
                final @NonNull JsonObject request,
                final @NonNull I18nResolver i18nResolver);

    /**
     * Performs an action for trash a Layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void trash(final @NonNull AnalyticsRequest analyticsRequest,
               final @NonNull JsonObject request,
               final @NonNull I18nResolver i18nResolver);

    /**
     * Performs an action for apply a Layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return {@link JsonObject} as response
     **/
    T apply( final @NonNull AnalyticsRequest analyticsRequest,
             final @NonNull JsonObject request,
             final @NonNull I18nResolver i18nResolver );

    /**
     * Performs an action for favourite create a Layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void favourite(final @NonNull AnalyticsRequest analyticsRequest,
                   final @NonNull JsonObject request,
                   final @NonNull I18nResolver i18nResolver);

    /**
     * Performs restores a Layout from trash
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void restore(final @NonNull AnalyticsRequest analyticsRequest,
                 final @NonNull JsonObject request,
                 final @NonNull I18nResolver i18nResolver);

    /**
     * Performs shares the layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void share(final @NonNull AnalyticsRequest analyticsRequest,
               final @NonNull JsonObject request,
               final @NonNull I18nResolver i18nResolver);

    /**
     * Performs copies the given layout
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void clone(final @NonNull AnalyticsRequest analyticsRequest,
               final @NonNull JsonObject request,
               final @NonNull I18nResolver i18nResolver);

    /**
     * Performs movies a given layout to new location
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          The request object containing data
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     **/
    void move(final @NonNull AnalyticsRequest analyticsRequest,
              final @NonNull JsonObject request,
              final @NonNull I18nResolver i18nResolver);
}
