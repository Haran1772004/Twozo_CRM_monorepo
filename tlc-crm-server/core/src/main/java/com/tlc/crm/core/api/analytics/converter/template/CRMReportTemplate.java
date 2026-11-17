package com.tlc.crm.core.api.analytics.converter.template;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.action.analytics.ActionType;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

/**
 * <p>
 * Interface responsible for getting templates
 * </p>
 *
 * @author Ryan
 * @version 1.0
 * @see CRMReportTemplate
 */
public interface CRMReportTemplate {

    /**
     * <p>
     * Processes a request to retrieve a list of available report templates, grouped by module.
     *
     * @param analyticsRequest Represents the context of the analytics request, including organization and role identifiers.
     * @param request          The JSON request payload.
     * @param i18nResolver     An instance of {@link I18nResolver} for resolving internationalized display names.
     * @param actionType       The type of action being performed.
     */
    void processTemplateListRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                    final @NonNull JsonObject request,
                                    final @NonNull I18nResolver i18nResolver,
                                    final @NonNull ActionType actionType);

    /**
     * <p>
     * Processes a request to retrieve the detailed configuration data for a specific
     * report template.
     *
     * @param analyticsRequest Represents the context of the analytics request, including organization and role identifiers.
     * @param request          The JSON request payload, containing the template ID to fetch details for.
     * @param i18nResolver     An instance of {@link I18nResolver} for resolving internationalized labels.
     * @param actionType       The type of action being performed.
     */
    void processTemplateDataRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                    final @NonNull JsonObject request,
                                    final @NonNull I18nResolver i18nResolver,
                                    final @NonNull ActionType actionType);
}
