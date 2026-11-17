package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncDeleteProcessor;

/**
 * <p>
 * Processor responsible for handling async delete operations for {@link Contact} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
class ContactAsyncDeleteProcessor extends AsyncDeleteProcessor<Contact> {

    private final ContactManager contactManager;

    ContactAsyncDeleteProcessor() {
        contactManager = ContactManager.getInstance();
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

    /**
     * {@inheritDoc}
     *
     * @param contact Refers the {@code Contact}.
     */
    @Override
    protected void loadSummaryDetails(final Contact contact) {
        contactManager.loadSummaryDetails(contact.orgId(), contact);
    }
}
