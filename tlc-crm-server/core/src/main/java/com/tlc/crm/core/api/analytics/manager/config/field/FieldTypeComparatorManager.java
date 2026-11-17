package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.FieldTypeComparator;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.MCANAFIELDTYPECOMPARATOR;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the field type related comparator
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FieldTypeComparatorManager extends CRMAbstractReportsManager<FieldTypeComparator>
{
    private static class Instance
    {
        private static final FieldTypeComparatorManager INSTANCE = new FieldTypeComparatorManager();
    }

    public static FieldTypeComparatorManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    protected Class<FieldTypeComparator> getModelClass()
    {
        return FieldTypeComparator.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANAFIELDTYPECOMPARATOR.TABLE );
    }
}
