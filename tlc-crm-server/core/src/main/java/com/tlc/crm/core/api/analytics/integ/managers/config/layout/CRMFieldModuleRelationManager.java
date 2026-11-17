package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMFieldModuleRelation;
import com.tlc.crm.core.sql.resource.CRMFIELDMODULERELATION;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the field, primary module and secondary module
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CRMFieldModuleRelationManager extends CRMAbstractReportsManager<CRMFieldModuleRelation> {
    private static class Instance {
        private static final CRMFieldModuleRelationManager INSTANCE = new CRMFieldModuleRelationManager();
    }

    public static CRMFieldModuleRelationManager INSTANCE() {
        return CRMFieldModuleRelationManager.Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<CRMFieldModuleRelation> getModelClass() {
        return CRMFieldModuleRelation.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The field-module relation table
     */
    @Override
    public Table getTable() {
        return Table.get(CRMFIELDMODULERELATION.TABLE);
    }
}
