package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMCUSTOMMODULARFIELD;
import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELD;
import com.tlc.crm.core.sql.resource.OMCRMMODULARFIELD;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Manager for report modular-field mapping records.
 * Fetches {@link ReportModularField} entries with their associated system/custom modular field POJOs
 * using LEFT JOINs to avoid security-context dependency.
 *
 * @author karthick V
 * @version 1.0
 */
public class ReportModularFieldManager extends CRMAbstractReportsManager<ReportModularField> {

    private static class Instance {
        private static final ReportModularFieldManager INSTANCE = new ReportModularFieldManager();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link ReportModularFieldManager} singleton
     */
    public static ReportModularFieldManager INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NonNull Class<ReportModularField> getModelClass() {
        return ReportModularField.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMCRMMODULARFIELD.TABLE);
    }

    /**
     * Fetches all {@link ReportModularField} entries for the given org ID with joined child POJOs.
     *
     * @param orgId the organization identifier
     * @return collection of {@link ReportModularField}, never null
     */
    public Collection<ReportModularField> fetchByOrgId(final Long orgId) {
        try {
            final Collection<ReportModularField> result = getAll(orgId, Criteria.eq(getTable().getColumn(OMCRMMODULARFIELD.ORG_ID), orgId));

            LOGGER.info("CRM Analytics : Fetched [{}] ReportModularField from DB for orgId [{}]", result.size(), orgId);
            return result;
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics : Failed to fetch ReportModularField for orgId [{}]", orgId, exception);
            return Collections.emptyList();
        }
    }

    /**
     * Fetches a single {@link ReportModularField} by field ID and org ID with joined child POJOs.
     *
     * @param orgId   the organization identifier
     * @param fieldId the field identifier
     * @return {@link ReportModularField} or {@code null} if not found
     */
    public ReportModularField fetchByFieldId(final Long orgId, final Long fieldId) {
        try {
            final Collection<ReportModularField> result = get(orgId, Collections.singleton(fieldId));

            if (!result.isEmpty()) {
                return result.iterator().next();
            }
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics : Failed to fetch ReportModularField for fieldId [{}]", fieldId, exception);
        }
        LOGGER.info("CRM Analytics : No ReportModularField found in DB for fieldId [{}]", fieldId);
        return null;
    }
}
