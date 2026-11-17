package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Deal trigger processor for the workflow V2.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class DealTriggerProcessorV2 extends AbstractTriggerProcessorV2<Deal> {

    private DealTriggerProcessorV2() {
    }

    private static class Instance {
        private static final DealTriggerProcessorV2 INSTANCE = new DealTriggerProcessorV2();
    }

    /**
     * Gets the {@link DealTriggerProcessorV2} instance.
     *
     * @return The instance of {@link DealTriggerProcessorV2}.
     */
    public static DealTriggerProcessorV2 getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param userId               Represents the user id.
     * @param triggerExecutionData Represents the trigger execution data.
     * @param workflows            Represents the workflows.
     */
    @Override
    public void handle(final Long userId, final TriggerExecutionData triggerExecutionData, final Collection<WorkflowV2> workflows) {
        collectUserIdsAndInvoke(userId, triggerExecutionData, workflows);
    }

    /**
     * Collects the user ids and invokes the trigger.
     *
     * @param userId               Represents the user id.
     * @param triggerExecutionData Represents the trigger execution data.
     * @param workflows            Represents the workflows.
     */
    private void collectUserIdsAndInvoke(final Long userId, final TriggerExecutionData triggerExecutionData, final Collection<WorkflowV2> workflows) {
        if (!workflows.isEmpty()) {
            final Set<Long> userIds = new HashSet<>();

            userIds.add(userId);
            loadDealUserIds(triggerExecutionData.models(), userIds);
            triggerService.collectUserIds(triggerExecutionData.orgId(), userIds, workflows);

            invoke(userId, triggerExecutionData, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of deal to fetch the user details.
     *
     * @param dealPairs Represents the deal pairs.
     * @param userIds   Represents the {@link Set} of user ids.
     */
    private void loadDealUserIds(final Collection<Pair<TlcModel, TlcModel>> dealPairs, final Set<Long> userIds) {
        final Consumer<Deal> userIdCollector = (deal) -> {
            userIds.add(deal.getSalesOwner().id());
            userIds.add(deal.getCreatedBy().id());
            userIds.add(deal.getUpdatedBy().id());
        };

        for (final Pair<TlcModel, TlcModel> dealPair : dealPairs) {
            final Deal oldDeal = (Deal) dealPair.getValue();

            userIdCollector.accept((Deal) dealPair.getKey());

            if (Objects.nonNull(oldDeal)) {
                userIdCollector.accept(oldDeal);
            }
        }
    }
}
