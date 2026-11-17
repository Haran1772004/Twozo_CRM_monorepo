package com.tlc.crm.zapier.internal.event.processor;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.event.converter.ContactZapEventDataConverter;;

import java.util.Collection;

/**
 * Handles the contact event {@link ZapEventProcessor}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ContactZapEventProcessor extends AbstractZapEventProcessor<Contact> {

    private final ContactZapEventDataConverter contactZapEventDataConverter;
    private final ContactManager contactManager;

    private ContactZapEventProcessor() {
        this.contactZapEventDataConverter = ContactZapEventDataConverter.getInstance();
        this.contactManager = ContactManager.getInstance();
    }

    private static final class Instance {
        private static final ContactZapEventProcessor EVENT_PROCESSOR = new ContactZapEventProcessor();
    }

    public static ContactZapEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapEventData Additional metadata required to process the event.
     */
    @Override
    public void process(final ZapEventData zapEventData) {
        final Collection<JsonObject> eventPayloads = contactZapEventDataConverter.convert(zapEventData.getModels(), zapEventData);

        zapEventData.setEventPayloads(eventPayloads);
        handle(zapEventData);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The ID of the organization to which the models belong.
     * @param modelIds The collection of unique model IDs to retrieve.
     * @return The collection of model instances corresponding to the provided IDs.
     */
    @Override
    public Collection<Contact> fetchModels(final long orgId, final Collection<Long> modelIds) {
        return contactManager.get(orgId, modelIds);
    }

}
