package com.tlc.crm.core.api.analytics.manager.share;

import com.tlc.crm.core.api.analytics.entity.share.DashboardEveryoneVisibility;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.MCANADEVERYONEVISBILITY;

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
class DashboardEveryoneVisibilityManager extends CRMAbstractReportsManager<DashboardEveryoneVisibility>
{
    private static class Instance
    {
        private static final DashboardEveryoneVisibilityManager INSTANCE = new DashboardEveryoneVisibilityManager();
    }

    public static DashboardEveryoneVisibilityManager INSTANCE()
    {
        return DashboardEveryoneVisibilityManager.Instance.INSTANCE;
    }

    protected @NonNull Class<DashboardEveryoneVisibility> getModelClass()
    {
        return DashboardEveryoneVisibility.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANADEVERYONEVISBILITY.TABLE );
    }

}
