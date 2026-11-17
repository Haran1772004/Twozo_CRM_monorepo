package com.tlc.crm.core.api.analytics.manager.share;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.fields.FieldOperation;
import com.tlc.crm.core.api.analytics.entity.config.fields.FieldTypeComparator;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.entity.config.fields.filter.Comparator;
import com.tlc.crm.core.api.analytics.entity.share.DashboardEveryoneVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardTeamVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardTerritoryVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardUserVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardVisibility;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;

/**
 * <p>
 * Provides an abstract way to manage the dashboard share services
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface DashboardShareManagerService
{
    /**
     * <p>
     * Retrieves the dashboard visibility manager responsible for dashboard share visibility details
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that handles the {@link FieldOperation}
     */
    default CRMViewConfigManager<DashboardVisibility> getDashBoardVisibilityManager()
    {
        return DashboardVisibilityManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the dashboard user visibility manager responsible for dashboard share user visibility details
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link Comparator}
     */
    default CRMViewConfigManager<DashboardUserVisibility> getDashBoardUserVisibilityManager()
    {
        return DashboardUserVisibilityManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the dashboard team visibility manager responsible for dashboard share team visibility details
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link AnalyticsField}
     */
    default CRMViewConfigManager<DashboardTeamVisibility> getDashBoardTeamVisibilityManager()
    {
        return DashboardTeamVisibilityManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the dashboard territory visibility manager responsible for dashboard share territory visibility details
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link FieldTypeComparator}
     */
    default CRMViewConfigManager<DashboardTerritoryVisibility> getDashBoardTerritoryVisibilityManager()
    {
        return DashboardTerritoryVisibilityManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the dashboard everyone visibility manager responsible for dashboard share everyone details
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link FieldBucketUnit}
     */
    default CRMViewConfigManager<DashboardEveryoneVisibility> getDashBoardEveryoneVisibilityManager()
    {
        return DashboardEveryoneVisibilityManager.INSTANCE();
    }
}
