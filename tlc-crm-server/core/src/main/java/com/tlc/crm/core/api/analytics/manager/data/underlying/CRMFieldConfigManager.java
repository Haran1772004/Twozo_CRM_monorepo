package com.tlc.crm.core.api.analytics.manager.data.underlying;

import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMFieldConfig;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMREPORTFIELDCONFIG;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the field configuration for underlying data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class CRMFieldConfigManager extends CRMAbstractReportsManager<CRMFieldConfig>
{
    private static class Instance
    {
        private static final CRMFieldConfigManager INSTANCE = new CRMFieldConfigManager();
    }

    public static CRMFieldConfigManager INSTANCE()
    {
        return Instance.INSTANCE;
    }
    @Override
    public Table getTable()
    {
        return Table.get( CRMREPORTFIELDCONFIG.TABLE );
    }

    @NonNull
    @Override
    protected Class<CRMFieldConfig> getModelClass()
    {
        return CRMFieldConfig.class;
    }
}
