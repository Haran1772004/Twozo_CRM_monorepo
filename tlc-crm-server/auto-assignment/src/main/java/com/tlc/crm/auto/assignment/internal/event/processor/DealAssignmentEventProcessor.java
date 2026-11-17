package com.tlc.crm.auto.assignment.internal.event.processor;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * Handles the {@link AutoAssignmentEventProcessor} of deal across different auto-assignment event processors.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class DealAssignmentEventProcessor extends AbstractAutoAssignmentEventProcessor<Deal> {

    private final DealManager dealManager;

    private DealAssignmentEventProcessor() {
        super(Source.DEAL);
        this.dealManager = DealManager.getInstance();
    }

    private static final class Instance {
        private static final DealAssignmentEventProcessor EVENT_PROCESSOR = new DealAssignmentEventProcessor();
    }

    static DealAssignmentEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   The ID of the organization to which the models belong.
     * @param dealIds The collection of unique model IDs to retrieve.
     * @return The collection of model instances corresponding to the provided IDs.
     */
    @Override
    public Collection<Deal> fetchModels(final long orgId, final Collection<Long> dealIds) {
        final Collection<Deal> deals = dealManager.get(orgId, dealIds);

        for (final Deal deal : deals) {
            dealManager.loadSummaryDetails(orgId, deal);
        }

        return deals;
    }
}
