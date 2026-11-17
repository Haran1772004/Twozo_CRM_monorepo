package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.entity.features.api.activity.model.Activity;

import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Activity trigger processor for the workflow V2.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class ActivityTriggerProcessorV2 extends AbstractTriggerProcessorV2<ActivityHolder> {

    private ActivityTriggerProcessorV2() {
    }

    private static class Instance {
        private static final ActivityTriggerProcessorV2 INSTANCE = new ActivityTriggerProcessorV2();
    }

    /**
     * Gets the {@link ActivityTriggerProcessorV2} instance.
     *
     * @return The instance of {@link ActivityTriggerProcessorV2}.
     */
    public static ActivityTriggerProcessorV2 getInstance() {
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
    private void collectUserIdsAndInvoke(final Long userId, final TriggerExecutionData triggerExecutionData,
                                         final Collection<WorkflowV2> workflows) {
        if (!workflows.isEmpty()) {
            final Set<Long> userIds = new HashSet<>();

            userIds.add(userId);
            loadActivityUserIds(triggerExecutionData.models(), userIds);
            triggerService.collectUserIds(triggerExecutionData.orgId(), userIds, workflows);

            invoke(userId, triggerExecutionData, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of activity to fetch the user details.
     *
     * @param activityHolderPairs Represents the activity holder pairs.
     * @param userIds             Represents the {@link Set} of user ids.
     */
    private void loadActivityUserIds(final Collection<Pair<TlcModel, TlcModel>> activityHolderPairs, final Set<Long> userIds) {
        final Consumer<Activity> userIdCollector = (activity) -> {
            userIds.add(activity.getAssigneeId());
            userIds.add(activity.getCreatorId());
        };

        for (final Pair<TlcModel, TlcModel> activityHolderPair : activityHolderPairs) {
            final ActivityHolder oldActivityHolder = (ActivityHolder) activityHolderPair.getValue();

            userIdCollector.accept(((ActivityHolder) activityHolderPair.getKey()).getActivity());

            if (Objects.nonNull(oldActivityHolder)) {
                userIdCollector.accept(oldActivityHolder.getActivity());
            }
        }
    }

}
