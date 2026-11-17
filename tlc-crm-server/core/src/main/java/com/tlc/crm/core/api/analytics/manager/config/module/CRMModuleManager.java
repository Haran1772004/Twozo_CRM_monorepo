package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModule;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMMODULE;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the module details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class CRMModuleManager extends CRMAbstractReportsManager<CRMModule>
{
    private static class Instance
    {
        private static final CRMModuleManager INSTANCE = new CRMModuleManager();
    }

    public static CRMModuleManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    protected @NonNull Class<CRMModule> getModelClass()
    {
        return CRMModule.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMMODULE.TABLE );
    }

}
