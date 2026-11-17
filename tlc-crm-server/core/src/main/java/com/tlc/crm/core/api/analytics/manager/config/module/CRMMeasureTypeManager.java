package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMMeasureType;
import com.tlc.crm.core.sql.resource.CRMMEASURETYPE;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the measure type details
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMMeasureTypeManager extends CRMAbstractReportsManager<CRMMeasureType>
{
    private static class Instance
    {
        private static final CRMMeasureTypeManager INSTANCE = new CRMMeasureTypeManager();
    }

    public static CRMMeasureTypeManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    protected Class<CRMMeasureType> getModelClass()
    {
        return CRMMeasureType.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Table}
     */
    @Override
    public Table getTable()
    {
        return Table.get( CRMMEASURETYPE.TABLE );
    }
}