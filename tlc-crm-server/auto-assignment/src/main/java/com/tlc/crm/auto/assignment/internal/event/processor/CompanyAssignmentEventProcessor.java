package com.tlc.crm.auto.assignment.internal.event.processor;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * Handles the {@link AutoAssignmentEventProcessor} of company across different auto-assignment event processors.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class CompanyAssignmentEventProcessor extends AbstractAutoAssignmentEventProcessor<Company> {

    private final CompanyManager companyManager;

    private CompanyAssignmentEventProcessor() {
        super(Source.COMPANY);
        this.companyManager = CompanyManager.getInstance();
    }

    private static final class Instance {
        private static final CompanyAssignmentEventProcessor EVENT_PROCESSOR = new CompanyAssignmentEventProcessor();
    }

    static CompanyAssignmentEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      The ID of the organization to which the models belong.
     * @param companyIds The collection of unique model IDs to retrieve.
     * @return The collection of model instances corresponding to the provided IDs.
     */
    @Override
    public Collection<Company> fetchModels(final long orgId, final Collection<Long> companyIds) {
        final Collection<Company> companies = companyManager.get(orgId, companyIds);

        for (final Company company : companies) {
            companyManager.loadSummaryDetails(orgId, company);
        }

        return companies;
    }

}
