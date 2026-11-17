package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncUpdateProcessor;
import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Processor responsible for handling async update operations for {@link Company} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
final class CompanyAsyncUpdateProcessor extends AsyncUpdateProcessor<Company> {

    private final CompanyConverter companyConverter;
    private final CompanyManager companyManager;

    CompanyAsyncUpdateProcessor() {
        companyConverter = CompanyConverter.getInstance();
        companyManager = CompanyManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param company Represents the {@link Company}
     */
    @Override
    protected void loadSummaryDetails(final Company company) {
        companyManager.loadSummaryDetails(company.orgId(), company);
    }

    /**
     * {@inheritDoc}
     *
     * @return The  {@link Source}
     */
    protected Source getSource() {
        return Source.COMPANY;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id
     * @param entity Refers the {@link Company} to be cloned
     * @return the cloned {@link Company}
     */
    @Override
    protected Company cloneModel(final long orgId, final Company entity) {
        final Company company = super.cloneModel(orgId, entity);

        company.setOrgId(orgId);
        company.setId(entity.id());

        return company;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param company         Represents the model to be updated
     */
    @Override
    protected void mapJsonToModel(final ConverterConfig converterConfig, final Company company) {
        final JsonObject dataJson = converterConfig.dataToBeConverted();

        if (converterConfig.isPartialData()) {
            companyConverter.setJsonDataToModel(converterConfig, company, dataJson);
        } else {
            companyConverter.convert(converterConfig, dataJson);
        }
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
}
