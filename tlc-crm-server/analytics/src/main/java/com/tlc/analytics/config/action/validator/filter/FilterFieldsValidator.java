package com.tlc.analytics.config.action.validator.filter;

import com.tlc.analytics.config.action.validator.AnalyticsConfigValidator;
import com.tlc.analytics.config.model.view.filter.FilterField;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to validate the filter field configuration
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 * @see AnalyticsConfigValidator
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class FilterFieldsValidator implements AnalyticsConfigValidator<FilterField>
{

    private static class Instance
    {
        private static final FilterFieldsValidator INSTANCE = new FilterFieldsValidator();
    }

    public static FilterFieldsValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param filterFields Represents the {@link FilterField} that containing the filter fields data
     * @return {@code true} if the filter fields are valid; otherwise, {@code false}
     */
    @Override
    public boolean isValid( final @NonNull Long orgId,
                            final @NonNull Collection<FilterField> filterFields )
    {
        return true;
    }
}
