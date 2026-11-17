package com.tlc.crm.core.validator.analytics.fields;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.validator.analytics.RequestValidator;

/**
 * <p>
 * Provides an abstract way to validate the field configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface FieldRequestValidator
{
    /**
     * <p>
     * Returns an instance of the metrics field config validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for metrics field configuration
     */
    default RequestValidator<JsonObject> getMetricsFieldValidator()
    {
        return MetricsFieldValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the xAxis config validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for xAxis field configuration
     */
    default RequestValidator<JsonObject> getXAxisFieldValidator()
    {
        return XAxisFieldValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the duration config validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for duration field configuration
     */
    default RequestValidator<JsonObject> getDurationFieldValidator()
    {
        return DurationFieldValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the filter field config validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for filter field configuration
     */
    default RequestValidator<JsonObject> getFilterFieldValidator()
    {
        return FilterFieldValidator.INSTANCE();
    }
}
