package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.features.internal.dataimport.FieldSourcePair;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Builder that can construct the {@link Deal} for importing
 *
 * @author sundar
 * @version 1.0
 * @see AbstractModelBuilder
 */
public class DealBuilder extends AbstractModelBuilder<Deal> {

    private static final Source SOURCE = Source.DEAL;

    private final DealConverter dealConverter;

    private DealBuilder() {
        this.dealConverter = DealConverter.getInstance();
    }

    private static final class Instance {
        private static final DealBuilder INSTANCE = new DealBuilder();
    }

    /**
     * Gets the instance of {@link DealBuilder}
     *
     * @return {@link DealBuilder}
     */
    public static DealBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @return The constructed object of type {@link Deal} using the data from the DataHolder for importing
     */
    @Override
    public Optional<Deal> build(final DataHolder dataHolder, final int index) {
        final JsonObject dealJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);

        if (Objects.nonNull(dealJson)) {
            final Deal deal = dealConverter.convert(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(),
                    dataHolder.locale(), false, dealJson, true), dealJson);

            loadRelatedSourceModel(dataHolder, deal, index);

            return Optional.of(deal);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link SourceModelContainer}
     * @param index      Represents the index.
     * @param deal       Represents the existing deal that will be updated with the latest data.
     * @return {@link Deal} loaded using the data from the {@link DataHolder} for import
     */
    @Override
    public Optional<Deal> build(final DataHolder dataHolder, final int index, final Deal deal) {
        final JsonObject dealJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);

        if (Objects.nonNull(dealJson)) {
            dealConverter.setJsonToDeal(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(),
                    false, dealJson, true), deal, dealJson);
        }
        return Optional.of(deal);
    }

    /**
     * Loads the related source model into the given {@link Deal}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param deal       Represents the model that will be updated with the related data
     * @param index      Represents the index
     */
    private void loadRelatedSourceModel(final DataHolder dataHolder, final Deal deal, final int index) {
        final Collection<FieldSourcePair> relatedFields = dataHolder.sourceRecordContainer().getRelatedFields(SOURCE);

        if (Objects.nonNull(relatedFields)) {
            final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();

            for (final FieldSourcePair relatedField : relatedFields) {
                final Source relatedSource = relatedField.relatedSource();

                switch (relatedSource) {
                    case COMPANY -> {
                        final Optional<TlcModel> optionalRelatedSourceModel = getRelatedSourceModel(index, sourceModelContainer, SOURCE, relatedSource);

                        optionalRelatedSourceModel.ifPresent(relatedSourceModel -> deal.setCompany((Company) relatedSourceModel));
                    }
                    case CONTACT -> {
                        final Optional<TlcModel> optionalRelatedSourceModel = getRelatedSourceModel(index, sourceModelContainer, SOURCE, relatedSource);

                        optionalRelatedSourceModel.ifPresent(relatedSourceModel -> {
                            final Contact contact = (Contact) relatedSourceModel;

                            deal.setPrimaryContact(contact);

                            if (Objects.isNull(deal.getCompany())) {
                                final Company contactCompany = contact.getCompany();

                                if (Objects.nonNull(contactCompany)) {
                                    deal.setCompany(contactCompany);
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}
