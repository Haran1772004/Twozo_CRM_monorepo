package com.tlc.analytics.config.manager.view.path;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.model.view.report.path.PathInfo;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;

/**
 * Interface to manage path-related configurations.
 * Provides default methods to retrieve instances of path managers.
 *
 * @author aswath
 * @version 1.0
 */
public interface PathConfigManager
{

    /**
     * Retrieves the PathManager instance, which is responsible for managing
     * configurations related to paths.
     *
     * @return an instance of AnalyticsConfigManager managing PathInfo.
     */
    default AnalyticsConfigManager<PathInfo> getPathManager()
    {
        return PathManager.INSTANCE();
    }

    /**
     * Retrieves the ReportPathManager instance, which is responsible for managing
     * configurations related to report path mappings.
     *
     * @return an instance of AnalyticsConfigManager managing ReportPathMapping.
     */
    default AnalyticsConfigManager<ReportPathMapping> getReportPathMappingManager()
    {
        return ReportPathManager.INSTANCE();
    }
}