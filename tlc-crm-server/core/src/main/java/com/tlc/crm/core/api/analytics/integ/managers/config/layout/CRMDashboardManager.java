package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the dashboard
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class CRMDashboardManager extends CRMAbstractReportsManager<CRMDashboardDetails>
{

    private static class Instance
    {
        private static final CRMDashboardManager INSTANCE = new CRMDashboardManager();
    }

    public static CRMDashboardManager INSTANCE()
    {
        return CRMDashboardManager.Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<CRMDashboardDetails> getModelClass()
    {
        return CRMDashboardDetails.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The dashboard details table
     */
    @Override
    public Table getTable()
    {
        return Table.get( CRMDASHBOARDDETAILS.TABLE );
    }
}
