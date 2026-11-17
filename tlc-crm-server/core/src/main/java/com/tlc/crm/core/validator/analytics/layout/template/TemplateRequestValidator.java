package com.tlc.crm.core.validator.analytics.layout.template;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.validator.analytics.RequestValidator;

/**
 * <p>
 * Provides access to various template request validators for CRM analytics dashboards.
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
public interface TemplateRequestValidator {
    /**
     * <p>
     * Returns an instance of the dashboard create request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for creating dashboards
     */
    default RequestValidator<JsonObject> templateListValidator() {
        return TemplateListValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the dashboard create request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for creating dashboards
     */
    default RequestValidator<JsonObject> templateGetValidator() {
        return TemplateGetValidator.INSTANCE();
    }
}
