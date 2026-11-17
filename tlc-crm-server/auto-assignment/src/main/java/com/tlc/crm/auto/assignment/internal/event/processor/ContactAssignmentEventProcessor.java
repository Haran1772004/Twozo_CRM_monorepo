package com.tlc.crm.auto.assignment.internal.event.processor;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * Handles the {@link AutoAssignmentEventProcessor} of contact across different auto-assignment event processors.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ContactAssignmentEventProcessor extends AbstractAutoAssignmentEventProcessor<Contact> {

    private final ContactManager contactManager;

    private ContactAssignmentEventProcessor() {
        super(Source.CONTACT);
        this.contactManager = ContactManager.getInstance();
    }

    private static final class Instance {
        private static final ContactAssignmentEventProcessor EVENT_PROCESSOR = new ContactAssignmentEventProcessor();
    }

    static ContactAssignmentEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      The ID of the organization to which the models belong.
     * @param contactIds The collection of unique model IDs to retrieve.
     * @return The collection of model instances corresponding to the provided IDs.
     */
    @Override
    public Collection<Contact> fetchModels(final long orgId, final Collection<Long> contactIds) {
        final Collection<Contact> contacts = contactManager.get(orgId, contactIds);

        for (final Contact contact : contacts) {
            contactManager.loadSummaryDetails(orgId, contact);
        }

        return contacts;
    }

}
