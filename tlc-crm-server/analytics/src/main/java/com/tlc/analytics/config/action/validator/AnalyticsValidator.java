package com.tlc.analytics.config.action.validator;

import com.tlc.analytics.config.action.validator.filter.FilterValidator;
import com.tlc.analytics.config.action.validator.reports.ReportsValidator;

/**
 * <p>
 * Provides the functionality to validate the reports and filter configuration
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
public class AnalyticsValidator implements ReportsValidator,
                                           FilterValidator
{
    private static class Instance
    {
        private static final AnalyticsValidator INSTANCE = new AnalyticsValidator();
    }

    public static AnalyticsValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

}
