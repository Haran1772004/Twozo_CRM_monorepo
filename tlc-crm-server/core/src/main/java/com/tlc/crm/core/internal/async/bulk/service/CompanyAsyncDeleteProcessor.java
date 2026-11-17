package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncDeleteProcessor;

/**
 * <p>
 * Processor responsible for handling async delete operations for {@link Company} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
class CompanyAsyncDeleteProcessor extends AsyncDeleteProcessor<Company> {

    private final CompanyManager companyManager;

    CompanyAsyncDeleteProcessor() {
        companyManager = CompanyManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<Company> getAbstractConfigManager() {
        return companyManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param company Refers the {@code Company}.
     */
    @Override
    protected void loadSummaryDetails(final Company company) {
        companyManager.loadSummaryDetails(company.orgId(), company);
    }
}
