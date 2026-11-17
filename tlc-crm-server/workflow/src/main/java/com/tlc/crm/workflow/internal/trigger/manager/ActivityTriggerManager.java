package com.tlc.crm.workflow.internal.trigger.manager;

import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.entity.features.api.activity.model.Activity;

import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;

import com.tlc.crm.workflow.internal.trigger.model.TriggerContext;
import com.tlc.crm.workflow.internal.trigger.model.navigate.WorkflowNavigator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Responsible for commencing activity-related trigger actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ActivityTriggerManager extends AbstractTriggerManager<ActivityHolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityTriggerManager.class);

    private ActivityTriggerManager() {
        super(TriggerSource.ACTIVITY);
    }

    private static class Instance {
        private static final ActivityTriggerManager INSTANCE = new ActivityTriggerManager();
    }

    /**
     * Gets the {@link ActivityTriggerManager} instance.
     *
     * @return The instance of {@link ActivityTriggerManager}.
     */
    public static ActivityTriggerManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerCreateEvent(final TriggerContext<ActivityHolder> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger update event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerUpdateEvent(final TriggerContext<ActivityHolder> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger delete event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerDeleteEvent(final TriggerContext<ActivityHolder> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of workflow.
     */
    @Override
    protected void tProcess(final TriggerContext<ActivityHolder> triggerContext, final Collection<Workflow> workflows) {
        final long orgId = triggerContext.orgId();

        for (final Pair<ActivityHolder, ActivityHolder> activityHolderPair : triggerContext.models()) {
            final ActivityHolder activityHolder = activityHolderPair.getKey();
            final WorkflowNavigator workflowNavigator = new WorkflowNavigator((List<Workflow>) workflows);

            while (workflowNavigator.hasNext()) {
                final Workflow workflow = workflowNavigator.next();

                if (null != workflow) {
                    triggerRequestHandler.execute(() -> {

                        try {
                            final Long activityId = activityHolder.id();
                            final Long workflowId = workflow.id();

                            LOGGER.info("Workflow Execution: Trigger execution initiated for Org ID: {}, Activity ID: {}, Workflow ID: {}", orgId, activityId, workflowId);

                            final Long executionHistoryId = workflowExecutionHistoryManager.create(getWorkflowExecutionHistory(orgId, workflow));
                            final Collection<WorkflowCriteriaSet> workflowCriteriaSets = workflow.getCriteriaSets();

                            if (workflowCriteriaSets.isEmpty()) {
                                invokeActions(activityHolder, workflow, executionHistoryId, triggerContext);
                            } else {

                                try {
                                    LOGGER.info("Workflow Execution: Expression evaluation process initiated for Org ID: {}, Activity ID: {}, Workflow ID: {}", orgId, activityId, workflowId);

                                    final WorkflowCriteriaExecutionStatus workflowCriteriaExecutionStatus = criteriaEvaluator
                                            .evaluate(activityHolderPair, workflowCriteriaSets, triggerSource);

                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, workflowCriteriaExecutionStatus);
                                    LOGGER.info("Workflow Execution: Expression evaluation process completed with Status: {} for Org ID: {}, Activity ID: {}, Workflow ID: {}",
                                            workflowCriteriaExecutionStatus, orgId, activityId, workflowId);

                                    if (workflowCriteriaExecutionStatus == WorkflowCriteriaExecutionStatus.PASSED) {
                                        invokeActions(activityHolder, workflow, executionHistoryId, triggerContext);
                                    } else {
                                        workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);
                                    }
                                } catch (Throwable cause) {
                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, WorkflowCriteriaExecutionStatus.FAILED);
                                    workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);

                                    LoggerUtil.logError(LOGGER, cause, "Workflow Execution: Expression evaluation failed for Activity ID: {}, Workflow ID: {}, Reason: {}", activityId, workflowId, cause.getMessage());
                                }
                            }
                        } finally {
                            triggerContext.clean();
                        }
                    });
                }
            }
        }
    }

    /**
     * Invokes the trigger execution process.
     *
     * @param triggerContext Refers the {@link TriggerContext}.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    private void collectUserIdsAndInvoke(final TriggerContext<ActivityHolder> triggerContext,
                                         final Collection<Workflow> workflows) {
        if (!workflows.isEmpty()) {
            final Set<Long> userIds = new HashSet<>();

            loadActivityUserIds(triggerContext.models(), userIds);
            triggerService.collectUserIds(triggerContext.orgId(), workflows, userIds);

            invoke(triggerContext, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of contact to fetch the user details.
     *
     * @param activityHolderPairs Refers the company pairs.
     * @param userIds             Refers the {@link Set} of user ids.
     */
    private void loadActivityUserIds(final Collection<Pair<ActivityHolder, ActivityHolder>> activityHolderPairs, final Set<Long> userIds) {
        final Consumer<Activity> userIdCollector = (activity) -> {
            userIds.add(activity.getAssigneeId());
            userIds.add(activity.getCreatorId());
        };

        for (final Pair<ActivityHolder, ActivityHolder> activityHolderPair : activityHolderPairs) {
            final ActivityHolder oldActivityHolder = activityHolderPair.getValue();

            userIdCollector.accept(activityHolderPair.getKey().getActivity());

            if (Objects.nonNull(oldActivityHolder)) {
                userIdCollector.accept(oldActivityHolder.getActivity());
            }
        }
    }
}
