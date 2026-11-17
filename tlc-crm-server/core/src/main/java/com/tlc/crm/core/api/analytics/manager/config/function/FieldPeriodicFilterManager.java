package com.tlc.crm.core.api.analytics.manager.config.function;

import com.tlc.crm.core.api.analytics.entity.config.function.FieldPeriodicFilter;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMFIELDPERIODICFILTER;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the periodic filter fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FieldPeriodicFilterManager extends CRMAbstractReportsManager<FieldPeriodicFilter>
{

    private static class Instance
    {
        private static final FieldPeriodicFilterManager INSTANCE = new FieldPeriodicFilterManager();
    }

    public static FieldPeriodicFilterManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    @Override
    protected Class<FieldPeriodicFilter> getModelClass()
    {
        return FieldPeriodicFilter.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMFIELDPERIODICFILTER.TABLE );
    }
}


