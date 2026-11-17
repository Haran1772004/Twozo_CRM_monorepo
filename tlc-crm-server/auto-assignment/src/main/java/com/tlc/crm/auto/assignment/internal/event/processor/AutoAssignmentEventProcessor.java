package com.tlc.crm.auto.assignment.internal.event.processor;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.internal.event.AutoAssignmentEventData;
import com.tlc.crm.auto.assignment.internal.status.AutoAssignmentErrorCodes;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * Defines a contract for processing auto-assignment events of a specific type.
 *
 * @param <T> the type of the event payload to be processed.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface AutoAssignmentEventProcessor<T> {

    /**
     * Returns an instance of the appropriate {@code AutoAssignmentEventProcessor} based on the given source type.
     *
     * @param source The {@link Source} enum indicating the type.
     * @return An instance of {@link AutoAssignmentEventProcessor} corresponding to the source.
     */
    static AutoAssignmentEventProcessor<?> getInstance(final Source source) {
        return switch (source) {
            case CONTACT -> ContactAssignmentEventProcessor.getInstance();
            case COMPANY -> CompanyAssignmentEventProcessor.getInstance();
            case DEAL -> DealAssignmentEventProcessor.getInstance();
            default -> throw ErrorCode.get(AutoAssignmentErrorCodes.INVALID_MODULE);
        };
    }

    /**
     * Processes a collection of models by preparing and invoking their assignment event flow.
     *
     * @param autoAssignmentEventData Additional metadata required to process the event.
     * @param autoAssignmentRules     Refers the collection {@code AutoAssignmentRule}.
     */
    void process(final AutoAssignmentEventData autoAssignmentEventData, final Collection<AutoAssignmentRule> autoAssignmentRules);

    /**
     * Fetches a collection of models based on the given organization ID and model IDs.
     *
     * @param orgId    The ID of the organization to which the models belong.
     * @param modelIds The collection of unique model IDs to retrieve.
     * @return The collection of model instances corresponding to the provided IDs.
     */
    Collection<T> fetchModels(final long orgId, final Collection<Long> modelIds);
}
