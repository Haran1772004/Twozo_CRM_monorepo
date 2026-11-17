package com.tlc.analytics.config.action.validator.filter;

/**
 * <p>
 * Provides an abstract way to validate the filter configuration
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
public interface FilterValidator
{
    default FilterFieldsValidator getFilterValidator()
    {
        return FilterFieldsValidator.INSTANCE();
    }
}
