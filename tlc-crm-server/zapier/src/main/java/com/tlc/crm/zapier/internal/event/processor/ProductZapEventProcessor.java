package com.tlc.crm.zapier.internal.event.processor;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;

import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.event.converter.ProductZapEventDataConverter;

import java.util.Collection;

/**
 * Handles the product event {@link ZapEventProcessor}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ProductZapEventProcessor extends AbstractZapEventProcessor<Product> {

    private final ProductZapEventDataConverter productZapEventDataConverter;
    private final ProductManager productManager;

    private ProductZapEventProcessor() {
        this.productZapEventDataConverter = ProductZapEventDataConverter.getInstance();
        this.productManager = ProductManager.getInstance();
    }

    private static final class Instance {
        private static final ProductZapEventProcessor EVENT_PROCESSOR = new ProductZapEventProcessor();
    }

    public static ProductZapEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapEventData Additional metadata required to process the event.
     */
    @Override
    public void process(final ZapEventData zapEventData) {
        final Collection<JsonObject> eventPayloads = productZapEventDataConverter.convert(zapEventData.getModels(), zapEventData);

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
    public Collection<Product> fetchModels(final long orgId, final Collection<Long> modelIds) {
        return productManager.get(orgId, modelIds);
    }
}
