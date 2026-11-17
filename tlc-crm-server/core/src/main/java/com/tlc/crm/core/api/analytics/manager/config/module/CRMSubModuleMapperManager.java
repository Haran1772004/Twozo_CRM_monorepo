package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSubModuleMapper;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.CRMSUBMODULEMAPPER;

import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the subModule fields details
 * </p>
 *
 * @author Ryan Sam Daniel
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
public class CRMSubModuleMapperManager extends CRMAbstractReportsManager<CRMSubModuleMapper>
{
    private static class Instance
    {
        private static final CRMSubModuleMapperManager INSTANCE = new CRMSubModuleMapperManager();
    }

    public static CRMSubModuleMapperManager INSTANCE()
    {
        return CRMSubModuleMapperManager.Instance.INSTANCE;
    }

    protected @NonNull Class<CRMSubModuleMapper> getModelClass()
    {
        return CRMSubModuleMapper.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMSUBMODULEMAPPER.TABLE );
    }
}
