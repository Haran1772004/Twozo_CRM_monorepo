package com.tlc.analytics.config.action.validator;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to validate the analytics configuration
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
public interface AnalyticsConfigValidator<T>
{
    /**
     * <p>
     * Checks if the given models are valid for the specified organization ID.
     * </p>
     *
     * @param orgId  the organization ID to check
     * @param models the collection of models to validate
     * @return true if the models are valid, false otherwise
     */
    boolean isValid( final Long orgId, final Collection<T> models );
}
