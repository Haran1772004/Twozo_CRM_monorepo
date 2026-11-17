package com.tlc.crm.core.api.analytics.manager.data.underlying;

import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMDefaultFieldConfig;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMREPORTDEFAULTFIELDCONFIG;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the default field configuration for underlying data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class CRMDefaultFieldConfigManager extends CRMAbstractReportsManager<CRMDefaultFieldConfig>
{
    private static class Instance
    {
        private static final CRMDefaultFieldConfigManager INSTANCE = new CRMDefaultFieldConfigManager();
    }

    public static CRMDefaultFieldConfigManager INSTANCE()
    {
        return Instance.INSTANCE;
    }
    @Override
    public Table getTable()
    {
        return Table.get( CRMREPORTDEFAULTFIELDCONFIG.TABLE );
    }

    @NonNull
    @Override
    protected Class<CRMDefaultFieldConfig> getModelClass()
    {
        return CRMDefaultFieldConfig.class;
    }
}
