package com.tlc.analytics.config.action.validator.layout;

import com.tlc.analytics.config.action.validator.AnalyticsConfigValidator;
import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.ViewType;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see AnalyticsConfigValidator
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class DashboardLayoutValidator implements AnalyticsConfigValidator<DashboardLayout>
{
    private static class Instance
    {
        private static final DashboardLayoutValidator INSTANCE = new DashboardLayoutValidator();
    }

    public static DashboardLayoutValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents an organization id
     * @param dashboardLayouts Represents the {@link DashboardLayout} that containing the dashboard layout data
     * @return {@code true} if the filter fields are valid; otherwise, {@code false}
     */
    @Override
    public boolean isValid( final @NonNull Long orgId,
                            final @NonNull Collection<DashboardLayout> dashboardLayouts )
    {
        for( final DashboardLayout dashboardLayout : dashboardLayouts )
        {
            final ViewDetails viewDetails =  dashboardLayout.getViewDetails();

            return Objects.nonNull(viewDetails)
                    && ViewType.DASHBOARD == viewDetails.getViewType()
                    && Objects.nonNull(viewDetails.getDisplayName());
        }

        return true;
    }
}
