package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.filter.ComparatorFilterType;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.MCANACOMPARATORFILTERVALUETYPE;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the fields comparator value type
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ComparatorFilterTypeManager extends CRMAbstractReportsManager<ComparatorFilterType>
{

    private static class Instance
    {
        private static final ComparatorFilterTypeManager INSTANCE = new ComparatorFilterTypeManager();
    }

    public static ComparatorFilterTypeManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<ComparatorFilterType> getModelClass()
    {
        return ComparatorFilterType.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANACOMPARATORFILTERVALUETYPE.TABLE );
    }
}
