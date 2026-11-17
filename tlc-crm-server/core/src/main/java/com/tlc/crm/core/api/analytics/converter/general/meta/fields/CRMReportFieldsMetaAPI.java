package com.tlc.crm.core.api.analytics.converter.general.meta.fields;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * Provides an abstract way to deal with report fields meta
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface CRMReportFieldsMetaAPI
{
    /**
     * <p>
     * Returns an instance of the {@link ReportsMetaResponse} for metrics-related actions
     * </p>
     *
     * @return the {@link ReportsMetaResponse} instance for metrics-related actions
     */
    default ReportsMetaResponse<JsonObject> getMetrics()
    {
        return CRMMetricsFieldMetaGenerator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the {@link ReportsMetaResponse} for xAxis-related actions
     * </p>
     *
     * @return the {@link ReportsMetaResponse} instance for xAxis-related actions
     */
    default ReportsMetaResponse<JsonObject> getViewBy()
    {
        return CRMViewByFieldMetaGenerator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the {@link ReportsMetaResponse} for duration-related actions
     * </p>
     *
     * @return the {@link ReportsMetaResponse} instance for duration-related actions
     */
    default ReportsMetaResponse<JsonObject> getDuration()
    {
        return CRMDurationFieldMetaGenerator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the {@link ReportsMetaResponse} for filter-field-related actions
     * </p>
     *
     * @return the {@link ReportsMetaResponse} instance for filter-field-related actions
     */
    default ReportsMetaResponse<JsonObject> getFilterFields()
    {
        return CRMFilterFieldMetaGenerator.INSTANCE();
    }
}
