package com.tlc.crm.core.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.manager.config.field.SystemModularFieldManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Organization initiator that seeds analytics report modular fields.
 * <p>
 * Author: karthick V
 * Version: 1.0
 */
@OrgInitiator(name = "analytics")
public class AnalyticsOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsOrgInitiator.class);

    private final CRMReportManagerAPI crmReportManagerAPI;
    private final CRMViewConfigManager<ReportModularField> reportFieldManager;
    private final SystemModularFieldManager systemModularFieldManager;
    /**
     * Creates initiator dependencies.
     */
    public AnalyticsOrgInitiator() {
        this.crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        this.reportFieldManager = crmReportManagerAPI.getReportFieldManager();
        this.systemModularFieldManager = SystemModularFieldManager.INSTANCE();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();
        LOGGER.info("Org Init Action: Analytics initialization started. Org ID: {}, User ID: {}", orgId, userId);

        final Collection<Long> fieldIds = systemModularFieldManager.fetchAllFieldIds();
        final Collection<ReportModularField> reportModularFields = new ArrayList<>(fieldIds.size());

        for (final Long fieldId : fieldIds) {
            reportModularFields.add(ReportModularField.builder().systemFieldId(fieldId).orgId(orgId).build());
        }

        reportFieldManager.create(orgId, reportModularFields);

        LOGGER.info("Org Init Action: Analytics initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
