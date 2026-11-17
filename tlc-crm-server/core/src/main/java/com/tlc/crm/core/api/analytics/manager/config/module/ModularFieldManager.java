package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.CRMMODULARFIELD;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * Provides an abstract way to manage the modular fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ModularFieldManager extends CRMAbstractReportsManager<ModularField>
{
    private static class Instance
    {
        private static final ModularFieldManager INSTANCE = new ModularFieldManager();
    }

    public static ModularFieldManager INSTANCE()
    {
        return Instance.INSTANCE;
    }
    @NotNull
    protected Class<ModularField> getModelClass()
    {
        return ModularField.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMMODULARFIELD.TABLE );
    }

}
