package com.tlc.crm.zapier.internal.event.processor;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import java.util.Collection;

/**
 * Interface representing a processor that handles Zapier event data for a specific model type.
 *
 * @param <T> the type of model to be processed (e.g., Contact, Deal, etc.)
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ZapEventProcessor<T> {

    /**
     * Returns an instance of the appropriate {@code ZapEventProcessor} based on the given source type.
     *
     * @param source The {@link Source} enum indicating the type.
     * @return An instance of {@link ZapEventProcessor} corresponding to the source.
     */
    static ZapEventProcessor<?> getInstance(final Source source) {
        return switch (source) {
            case CONTACT -> ContactZapEventProcessor.getInstance();
            case COMPANY -> CompanyZapEventProcessor.getInstance();
            case DEAL -> DealZapEventProcessor.getInstance();
            case PRODUCT -> ProductZapEventProcessor.getInstance();
            case ACTIVITY -> ActivityZapEventProcessor.getInstance();
            default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_SOURCE);
        };
    }

    /**
     * Processes a collection of models by preparing and invoking their Zapier event flow.
     *
     * @param zapEventData Additional metadata required to process the event.
     */
    void process(final ZapEventData zapEventData);

    /**
     * Fetches a collection of models based on the given organization ID and model IDs.
     *
     * @param orgId    The ID of the organization to which the models belong.
     * @param modelIds The collection of unique model IDs to retrieve.
     * @return The collection of model instances corresponding to the provided IDs.
     */
    Collection<T> fetchModels(final long orgId, final Collection<Long> modelIds);
}
