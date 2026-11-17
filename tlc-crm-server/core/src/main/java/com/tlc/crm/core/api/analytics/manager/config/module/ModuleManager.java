package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModule;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMMODULE;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * Provides an abstract way to manage the modules
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ModuleManager extends CRMAbstractReportsManager<CRMModule>
{
    private static class Instance
    {
        private static final ModuleManager INSTANCE = new ModuleManager();
    }

    public static ModuleManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    protected Class<CRMModule> getModelClass()
    {
        return CRMModule.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMMODULE.TABLE );
    }

}
