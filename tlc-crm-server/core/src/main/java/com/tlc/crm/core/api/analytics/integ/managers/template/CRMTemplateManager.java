package com.tlc.crm.core.api.analytics.integ.managers.template;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMTemplateDetails;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEDETAILS;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Singleton manager class responsible for handling operations related to {@link CRMTemplateDetails}.
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class CRMTemplateManager extends CRMAbstractReportsManager<CRMTemplateDetails> {
    private static class Instance {
        private static final CRMTemplateManager INSTANCE = new CRMTemplateManager();
    }

    public static CRMTemplateManager INSTANCE() {
        return CRMTemplateManager.Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<CRMTemplateDetails> getModelClass() {
        return CRMTemplateDetails.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The dashboard details table
     */
    @Override
    public Table getTable() {
        return Table.get(CRMTEMPLATEDETAILS.TABLE);
    }
}
