package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.filter.Comparator;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.MCANACOMPARATOR;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the fields comparator
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ComparatorManager extends CRMAbstractReportsManager<Comparator>
{

    private static class Instance
    {
        private static final ComparatorManager INSTANCE = new ComparatorManager();
    }

    public static ComparatorManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<Comparator> getModelClass()
    {
        return Comparator.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANACOMPARATOR.TABLE );
    }
}
