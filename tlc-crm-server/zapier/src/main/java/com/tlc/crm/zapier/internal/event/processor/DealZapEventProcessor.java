package com.tlc.crm.zapier.internal.event.processor;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.event.converter.DealZapEventDataConverter;

import java.util.Collection;

/**
 * Handles the deal event {@link ZapEventProcessor}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class DealZapEventProcessor extends AbstractZapEventProcessor<Deal> {

    private final DealZapEventDataConverter dealZapEventDataConverter;
    private final DealManager dealManager;

    private DealZapEventProcessor() {
        this.dealZapEventDataConverter = DealZapEventDataConverter.getInstance();
        this.dealManager = DealManager.getInstance();
    }

    private static final class Instance {
        private static final DealZapEventProcessor EVENT_PROCESSOR = new DealZapEventProcessor();
    }

    public static DealZapEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapEventData Additional metadata required to process the event.
     */
    @Override
    public void process(final ZapEventData zapEventData) {
        final Collection<JsonObject> eventPayloads = dealZapEventDataConverter.convert(zapEventData.getModels(), zapEventData);

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
    public Collection<Deal> fetchModels(final long orgId, final Collection<Long> modelIds) {
        return dealManager.get(orgId, modelIds);
    }

}
