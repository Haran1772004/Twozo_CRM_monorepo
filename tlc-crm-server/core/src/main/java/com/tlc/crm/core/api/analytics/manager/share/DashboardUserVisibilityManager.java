package com.tlc.crm.core.api.analytics.manager.share;

import com.tlc.crm.core.api.analytics.entity.share.DashboardUserVisibility;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.MCANADUSERVISBILITY;

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
class DashboardUserVisibilityManager extends CRMAbstractReportsManager<DashboardUserVisibility>
{
    private static class Instance
    {
        private static final DashboardUserVisibilityManager INSTANCE = new DashboardUserVisibilityManager();
    }

    public static DashboardUserVisibilityManager INSTANCE()
    {
        return DashboardUserVisibilityManager.Instance.INSTANCE;
    }

    protected @NonNull Class<DashboardUserVisibility> getModelClass()
    {
        return DashboardUserVisibility.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANADUSERVISBILITY.TABLE );
    }
}
