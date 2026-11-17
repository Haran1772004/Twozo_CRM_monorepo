package com.tlc.crm.core.validator.analytics.layout.view.axis;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.validator.analytics.RequestValidator;

/**
 * <p>
 * Provides an abstract way to validate the axis configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface AxisRequestValidator
{

    /**
     * <p>
     * Returns an instance of the compare to request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for compare to
     */
    default RequestValidator<JsonObject> getCompareToValidator()
    {
        return CompareToValidator.INSTANCE();
    }
}
