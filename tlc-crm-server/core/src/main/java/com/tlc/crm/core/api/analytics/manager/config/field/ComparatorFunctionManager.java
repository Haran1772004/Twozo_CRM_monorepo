package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.filter.ComparatorFunction;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.MCANACOMPARATORFUNCTION;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the fields comparator function
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ComparatorFunctionManager extends CRMAbstractReportsManager<ComparatorFunction>
{

    private static class Instance
    {
        private static final ComparatorFunctionManager INSTANCE = new ComparatorFunctionManager();
    }

    public static ComparatorFunctionManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<ComparatorFunction> getModelClass()
    {
        return ComparatorFunction.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANACOMPARATORFUNCTION.TABLE );
    }
}
