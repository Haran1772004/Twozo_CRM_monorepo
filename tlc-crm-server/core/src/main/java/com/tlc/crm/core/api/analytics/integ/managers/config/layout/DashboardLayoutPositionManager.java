package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutPosition;
import com.tlc.crm.core.sql.resource.DASHBOARDLAYOUTPOSITION;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the dashboard layout position
 * </p>
 *
 * @author saravanan
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class DashboardLayoutPositionManager extends CRMAbstractReportsManager<DashboardLayoutPosition>
{

    private static class Instance
    {
        private static final DashboardLayoutPositionManager INSTANCE = new DashboardLayoutPositionManager();
    }

    public static DashboardLayoutPositionManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<DashboardLayoutPosition> getModelClass()
    {
        return DashboardLayoutPosition.class;
    }

    @Override
    public @NonNull Table getTable()
    {
        return Table.get( DASHBOARDLAYOUTPOSITION.TABLE );
    }
}
