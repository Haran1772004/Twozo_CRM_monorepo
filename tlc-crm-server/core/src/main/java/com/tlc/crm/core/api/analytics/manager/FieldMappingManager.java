package com.tlc.crm.core.api.analytics.manager;

import com.tlc.crm.core.api.analytics.entity.integ.reports.FieldColumnMapping;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.CRMFIELDCOLUMNMAPPING;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * Manages CRM Manager API
 * </p>
 *
 * @author James
 * @version 1.0
 */

@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FieldMappingManager extends CRMAbstractReportsManager<FieldColumnMapping>
{
    private static class Instance
    {
        private static final FieldMappingManager INSTANCE = new FieldMappingManager();
    }

    public static FieldMappingManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    @Override
    protected Class<FieldColumnMapping> getModelClass()
    {
        return FieldColumnMapping.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMFIELDCOLUMNMAPPING.TABLE );
    }

}
