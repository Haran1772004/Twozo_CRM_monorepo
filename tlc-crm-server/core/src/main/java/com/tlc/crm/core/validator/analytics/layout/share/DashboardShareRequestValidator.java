package com.tlc.crm.core.validator.analytics.layout.share;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.validator.analytics.RequestValidator;

/**
 * <p>
 * Provides an abstract way to validate the dashboard share visibility configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface DashboardShareRequestValidator
{
    /**
     * <p>
     * Returns the validator for dashboard update share visibility requests
     * </p>
     *
     * @return the {@link RequestValidator} instance for updating dashboard visibility
     */
    default RequestValidator<JsonObject> visibilityUpdateValidator()
    {
        return DashboardVisibilityUpdateValidator.getInstance();
    }

    /**
     * <p>
     * Returns the validator for dashboard share visibility get requests
     * </p>
     *
     * @return the {@link RequestValidator} instance for getting dashboard visibility
     */
    default RequestValidator<JsonObject> visibilityGetValidator()
    {
        return DashboardVisibilityGetValidator.getInstance();
    }
}
