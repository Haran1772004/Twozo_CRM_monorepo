package com.tlc.crm.core.api.analytics.converter.general.meta.fields;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.commons.json.JsonObject;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;


/**
 * <p>
 * Provides an abstract way for handling the report response
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface ReportsMetaResponse<T>
{
    /**
     * <p>
     * Generates a response of type {@code T} based on the provided parameters
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param jsonObject   A {@link JsonObject} containing the request data needed to generate the response
     * @param i18nResolver An instance of {@link I18nResolver} for handling internationalization
     * @return A response of type {@code T} based on the provided input parameters
     */
    @NonNull
    T getResponse( final @NonNull AnalyticsRequest analyticsRequest,
                   final @NonNull JsonObject jsonObject,
                   final @NonNull I18nResolver i18nResolver );
}