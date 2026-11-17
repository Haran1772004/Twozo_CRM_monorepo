package com.tlc.crm.core.api.analytics.manager.share;

import com.tlc.crm.core.api.analytics.entity.share.DashboardTerritoryVisibility;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.MCANADTERRITORYVISBILITY;

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
class DashboardTerritoryVisibilityManager extends CRMAbstractReportsManager<DashboardTerritoryVisibility>
{
    private static class Instance
    {
        private static final DashboardTerritoryVisibilityManager INSTANCE = new DashboardTerritoryVisibilityManager();
    }

    public static DashboardTerritoryVisibilityManager INSTANCE()
    {
        return DashboardTerritoryVisibilityManager.Instance.INSTANCE;
    }

    protected @NonNull Class<DashboardTerritoryVisibility> getModelClass()
    {
        return DashboardTerritoryVisibility.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANADTERRITORYVISBILITY.TABLE );
    }
}
