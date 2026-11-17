package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;

import java.util.Objects;
import java.util.Optional;

/**
 * Builder that can construct the {@link Company} for importing
 *
 * @author sundar
 * @version 1.0
 * @see ModelBuilder
 */
public class CompanyBuilder implements ModelBuilder<Company> {

    private static final Source SOURCE = Source.COMPANY;
    private final CompanyConverter companyConverter;

    private CompanyBuilder() {
        this.companyConverter = CompanyConverter.getInstance();
    }

    private static final class Instance {
        private static final CompanyBuilder COMPANY_BUILDER = new CompanyBuilder();
    }

    /**
     * Gets the instance of {@link CompanyBuilder}
     *
     * @return {@link CompanyBuilder}
     */
    public static CompanyBuilder getInstance() {
        return Instance.COMPANY_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @return The constructed object of type {@link Company} using the data from the DataHolder for importing
     */
    @Override
    public Optional<Company> build(final DataHolder dataHolder, final int index) {
        final JsonObject companyJson = dataHolder.sourceRecordContainer().get(index).get(Source.COMPANY);

        if (Objects.isNull(companyJson)) {
            return Optional.empty();
        }
        return Optional.of(companyConverter.convert(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(),
                false, companyJson, true), companyJson));
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @param company    Represents the existing model that will be updated with the latest data.
     * @return {@link Company} loaded using the data from the {@link DataHolder} for import
     */
    @Override
    public Optional<Company> build(final DataHolder dataHolder, final int index, final Company company) {
        final JsonObject companyJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);

        if (Objects.nonNull(companyJson)) {
            companyConverter.setJsonDataToModel(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(),
                    false, companyJson, true), company, companyJson);
        }
        return Optional.of(company);
    }

}