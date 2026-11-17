package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncUpdateProcessor;
import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Processor responsible for handling async update operations for {@link Contact} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
final class ContactAsyncUpdateProcessor extends AsyncUpdateProcessor<Contact> {

    private final ContactConverter contactConverter;
    private final ContactManager contactManager;

    ContactAsyncUpdateProcessor() {
        contactConverter = ContactConverter.getInstance();
        contactManager = ContactManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param contact Represents the {@link Contact}
     */
    @Override
    protected void loadSummaryDetails(final Contact contact) {
        contactManager.loadSummaryDetails(contact.orgId(), contact);
    }

    /**
     * {@inheritDoc}
     *
     * @return The  {@link Source}
     */
    protected Source getSource() {
        return Source.CONTACT;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id
     * @param entity Refers the {@link Contact} to be cloned
     * @return The cloned {@link Contact}
     */
    @Override
    protected Contact cloneModel(final long orgId, final Contact entity) {
        final Contact contact = super.cloneModel(orgId, entity);

        contact.setOrgId(orgId);
        contact.setId(entity.id());

        return contact;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param contact         Represents the model to be updated
     */
    @Override
    protected void mapJsonToModel(final ConverterConfig converterConfig, final Contact contact) {
        final JsonObject dataJson = converterConfig.dataToBeConverted();

        if (converterConfig.isPartialData()) {
            contactConverter.setJsonDataToModel(converterConfig, contact, dataJson);
        } else {
            contactConverter.convert(converterConfig, dataJson);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<Contact> getAbstractConfigManager() {
        return contactManager;
    }
}