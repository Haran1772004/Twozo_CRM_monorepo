package com.tlc.crm.core.api.analytics.manager.config.function;

import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunctionFieldColumnMapper;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMFUNCTIONFIELDCOLUMNMAPPER;

import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the FunctionFieldColumnMapper
 * </p>
 *
 * @author saravanan
 * @version 1.0
 */
public class CRMFunctionFieldColumnMapperManager extends CRMAbstractReportsManager<CRMFunctionFieldColumnMapper> {

    private static class Instance{
        private static final CRMFunctionFieldColumnMapperManager INSTANCE = new CRMFunctionFieldColumnMapperManager();
    }

    public static CRMFunctionFieldColumnMapperManager INSTANCE() {
        return Instance.INSTANCE;
    }

    @NonNull
    @Override
    protected Class<CRMFunctionFieldColumnMapper> getModelClass()
    {
        return CRMFunctionFieldColumnMapper.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMFUNCTIONFIELDCOLUMNMAPPER.TABLE );
    }
}
