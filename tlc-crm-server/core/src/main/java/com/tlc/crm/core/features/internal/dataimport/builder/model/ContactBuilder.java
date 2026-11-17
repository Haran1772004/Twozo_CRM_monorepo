package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
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
 * Builder that can construct the {@link Contact} for importing
 *
 * @author sundar
 * @version 1.0
 * @see AbstractModelBuilder
 */
public class ContactBuilder extends AbstractModelBuilder<Contact> {

    private static final Source SOURCE = Source.CONTACT;
    private final ContactConverter contactConverter;

    private ContactBuilder() {
        this.contactConverter = ContactConverter.getInstance();
    }

    private static final class Instance {
        private static final ContactBuilder CONTACT_BUILDER = new ContactBuilder();
    }

    /**
     * Gets the instance of {@link ContactBuilder}
     *
     * @return {@link ContactBuilder}
     */
    public static ContactBuilder getInstance() {
        return Instance.CONTACT_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index
     * @return The constructed object of type {@link Contact} using the data from the DataHolder for importing.
     */
    @Override
    public Optional<Contact> build(final DataHolder dataHolder, final int index) {
        final JsonObject contactJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);

        if (Objects.nonNull(contactJson)) {
            final Contact contact = contactConverter.convert(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(),
                    dataHolder.locale(), false, contactJson, true), contactJson);

            loadRelatedSourceData(dataHolder, contact, index);

            return Optional.of(contact);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link SourceModelContainer}
     * @param index      Represents the index.
     * @param contact    Represents the existing model that will be updated with the latest data.
     * @return {@link Contact} loaded using the data from the {@link DataHolder} for import
     */
    public Optional<Contact> build(final DataHolder dataHolder, final int index, final Contact contact) {
        final JsonObject contactJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);

        if (Objects.nonNull(contactJson)) {
            contactConverter.setJsonDataToModel(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(),
                    false, contactJson, true), contact, contactJson);
        }
        loadRelatedSourceData(dataHolder, contact, index);

        return Optional.of(contact);
    }

    /**
     * Loads the related source model into the given {@link Contact}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param contact    Represents the model that will be updated with the related data
     * @param index      Represents the index
     */
    private void loadRelatedSourceData(final DataHolder dataHolder, final Contact contact, final int index) {
        final Collection<FieldSourcePair> relatedFields = dataHolder.sourceRecordContainer().getRelatedFields(SOURCE);

        if (Objects.nonNull(relatedFields)) {
            final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();

            for (final FieldSourcePair fieldSourcePair : relatedFields) {
                final Source relatedSource = fieldSourcePair.relatedSource();

                if (Source.COMPANY == relatedSource) {
                    final Optional<TlcModel> optionalRelatedSourceModel = getRelatedSourceModel(index, sourceModelContainer, SOURCE, relatedSource);

                    optionalRelatedSourceModel.ifPresent(model -> contact.setCompany((Company) model));
                }
            }
        }
    }
}
