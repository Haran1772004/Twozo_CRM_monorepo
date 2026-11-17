package com.tlc.analytics.config.action.validator.reports;

import com.tlc.analytics.config.action.validator.layout.DashboardLayoutValidator;

/**
 * <p>
 * Provides an abstract way to validate the reports configuration
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
public interface ReportsValidator
{
    /**
     * Retrieves the instance of the {@link DashboardLayoutValidator}
     *
     * @return the singleton instance of {@link DashboardLayoutValidator}
     */
    default DashboardLayoutValidator getDashboardValidator()
    {
        return DashboardLayoutValidator.INSTANCE();
    }

    /**
     * Retrieves the instance of the {@link ChartViewValidator}
     *
     * @return the singleton instance of {@link ChartViewValidator}
     */
    default ChartViewValidator getChartViewValidator()
    {
        return ChartViewValidator.INSTANCE();
    }

    /**
     * Retrieves the instance of the {@link ReportFieldsValidator}
     *
     * @return the singleton instance of {@link ReportFieldsValidator}
     */
    default ReportFieldsValidator getReportsFieldValidator()
    {
        return ReportFieldsValidator.INSTANCE();
    }
}
