package com.tlc.crm.core.api.analytics.manager.share;

import com.tlc.crm.core.api.analytics.entity.share.DashboardVisibility;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;

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
@NoArgsConstructor( access = AccessLevel.PRIVATE )
@Accessors( fluent = true )
class DashboardVisibilityManager extends CRMAbstractReportsManager<DashboardVisibility>
{
    private static class Instance
    {
        private static final DashboardVisibilityManager INSTANCE = new DashboardVisibilityManager();
    }

    public static DashboardVisibilityManager INSTANCE()
    {
        return DashboardVisibilityManager.Instance.INSTANCE;
    }

    protected @NonNull Class<DashboardVisibility> getModelClass()
    {
        return DashboardVisibility.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( OMCANADASHBOARDVISBILITY.TABLE );
    }

}
