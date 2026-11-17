package com.tlc.crm.zapier.internal.event.processor;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;

import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.event.converter.CompanyZapEventDataConverter;

import java.util.Collection;

/**
 * Handles the company event {@link ZapEventProcessor}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class CompanyZapEventProcessor extends AbstractZapEventProcessor<Company> {

    private final CompanyZapEventDataConverter companyZapEventDataConverter;
    private final CompanyManager companyManager;

    private CompanyZapEventProcessor() {
        this.companyZapEventDataConverter = CompanyZapEventDataConverter.getInstance();
        this.companyManager = CompanyManager.getInstance();
    }

    private static final class Instance {
        private static final CompanyZapEventProcessor EVENT_PROCESSOR = new CompanyZapEventProcessor();
    }

    public static CompanyZapEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapEventData Additional metadata required to process the event.
     */
    @Override
    public void process(final ZapEventData zapEventData) {
        final Collection<JsonObject> eventPayloads = companyZapEventDataConverter.convert(zapEventData.getModels(), zapEventData);

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
    public Collection<Company> fetchModels(final long orgId, final Collection<Long> modelIds) {
        return companyManager.get(orgId, modelIds);
    }
}
