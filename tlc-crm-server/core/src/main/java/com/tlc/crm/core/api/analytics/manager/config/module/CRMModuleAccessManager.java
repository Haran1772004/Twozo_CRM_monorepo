package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleAccess;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMMODULEACCESS;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Managing CRM reports with support for {@link CRMModuleAccess} CRUD operations.
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class CRMModuleAccessManager extends CRMAbstractReportsManager<CRMModuleAccess>
{

    @Getter
    @Accessors( fluent = true )
    private static final CRMModuleAccessManager INSTANCE = new CRMModuleAccessManager();

    @NonNull
    @Override
    protected Class<CRMModuleAccess> getModelClass()
    {
        return CRMModuleAccess.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMMODULEACCESS.TABLE );
    }
}