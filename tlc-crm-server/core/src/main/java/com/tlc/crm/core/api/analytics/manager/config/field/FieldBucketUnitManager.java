package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMFIELDBUCKETUNIT;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Managing CRM reports with support for {@link FieldBucketUnit} CRUD operations.
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FieldBucketUnitManager extends CRMAbstractReportsManager<FieldBucketUnit>
{

    private static class Instance
    {
        private static final FieldBucketUnitManager INSTANCE = new FieldBucketUnitManager();
    }

    public static FieldBucketUnitManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    @Override
    protected Class<FieldBucketUnit> getModelClass()
    {
        return FieldBucketUnit.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMFIELDBUCKETUNIT.TABLE );
    }
}


