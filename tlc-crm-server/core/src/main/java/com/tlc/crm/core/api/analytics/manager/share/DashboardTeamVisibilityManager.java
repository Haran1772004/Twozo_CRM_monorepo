package com.tlc.crm.core.api.analytics.manager.share;

import com.tlc.crm.core.api.analytics.entity.share.DashboardTeamVisibility;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.MCANADTEAMVISBILITY;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * Provides an abstract way to manage the module details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class DashboardTeamVisibilityManager extends CRMAbstractReportsManager<DashboardTeamVisibility>
{
    private static class Instance
    {
        private static final DashboardTeamVisibilityManager INSTANCE = new DashboardTeamVisibilityManager();
    }

    public static DashboardTeamVisibilityManager INSTANCE()
    {
        return DashboardTeamVisibilityManager.Instance.INSTANCE;
    }

    protected @NonNull Class<DashboardTeamVisibility> getModelClass()
    {
        return DashboardTeamVisibility.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANADTEAMVISBILITY.TABLE );
    }
}
