package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the child view
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class CRMChildViewManager extends CRMAbstractReportsManager<CRMChildViewDetails>
{

    private static class Instance
    {
        private static final CRMChildViewManager INSTANCE = new CRMChildViewManager();
    }

    public static CRMChildViewManager INSTANCE()
    {
        return CRMChildViewManager.Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<CRMChildViewDetails> getModelClass()
    {
        return CRMChildViewDetails.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The child view details table
     */
    @Override
    public Table getTable()
    {
        return Table.get( "CRMChildViewDetails" );
    }
}
