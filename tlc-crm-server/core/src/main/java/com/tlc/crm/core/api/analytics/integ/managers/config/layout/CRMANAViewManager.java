package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
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
class CRMANAViewManager extends CRMAbstractReportsManager<CRMANAViewMapper>
{

    private static class Instance
    {
        private static final CRMANAViewManager INSTANCE = new CRMANAViewManager();
    }

    public static CRMANAViewManager INSTANCE()
    {
        return CRMANAViewManager.Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<CRMANAViewMapper> getModelClass()
    {
        return CRMANAViewMapper.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The child view details table
     */
    @Override
    public Table getTable()
    {
        return Table.get( "CRMANAViewMapper" );
    }
}
