package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.workflow.internal.WorkflowEmailTracker;
import com.tlc.crm.core.workflow.internal.WorkflowEmailTrackerManager;

import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.internal.trigger.evaluate.WorkflowCriteriaEvaluator;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.model.ExecutionEntity;
import com.tlc.crm.workflow.v2.api.model.ExecutionStatus;
import com.tlc.crm.workflow.v2.api.model.TriggerConfiguration;
import com.tlc.crm.workflow.v2.api.model.TriggerScope;
import com.tlc.crm.workflow.v2.api.model.WaitForConditionConfiguration;
import com.tlc.crm.workflow.v2.api.model.WaitRegistration;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowNodeSupport;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2Execution;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Starts, resumes, and fail-fast-aborts Workflow V2 graph executions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2ExecutionEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2ExecutionEngine.class);

    private final WorkflowV2ExecutionManager executionManager;
    private final NodeWalker nodeWalker;
    private final WorkflowCriteriaEvaluator criteriaEvaluator;
    private final NodeExecutionContextRegistry contextRegistry;
    private final WorkflowEmailConditionValidator workflowEmailConditionValidator;
    private final WorkflowEmailTrackerManager workflowEmailTrackerManager;

    private WorkflowV2ExecutionEngine() {
        this.executionManager = WorkflowV2ExecutionManager.getInstance();
        this.nodeWalker = new NodeWalker();
        this.criteriaEvaluator = WorkflowCriteriaEvaluator.getInstance();
        this.contextRegistry = NodeExecutionContextRegistry.getInstance();
        this.workflowEmailConditionValidator = WorkflowEmailConditionValidator.getInstance();
        this.workflowEmailTrackerManager = WorkflowEmailTrackerManager.getInstance();
    }

    private static final class Instance {
        private static final WorkflowV2ExecutionEngine INSTANCE = new WorkflowV2ExecutionEngine();
    }

    /**
     * Gets the instance of the {@link WorkflowV2ExecutionEngine}.
     *
     * @return The instance of the {@link WorkflowV2ExecutionEngine}.
     */
    public static WorkflowV2ExecutionEngine getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Executes the workflow V2.
     *
     * @param nodeExecutionContext Represents the {@link NodeExecutionContext}.
     */
    public void execute(final NodeExecutionContext nodeExecutionContext) {
        final WorkflowNode triggerNode = nodeExecutionContext.triggerNode();

        walk(nodeExecutionContext, triggerNode == null ? null : triggerNode.id());
    }

    /**
     * Walks the workflow V2.
     *
     * @param context     Represents the {@link NodeExecutionContext}.
     * @param startNodeId Represents the start node id.
     */
    private void walk(final NodeExecutionContext context, final Long startNodeId) {
        Long nodeId = startNodeId;

        if (Objects.isNull(nodeId)) {
            executionManager.completeExecution(context.orgId(), context.executionId());
            contextRegistry.remove(context.executionId());
            return;
        }

        while (Objects.nonNull(nodeId)) {
            final WorkflowNode node = context.node(nodeId);

            context.setNodeId(nodeId);

            if (Objects.isNull(node)) {
                executionManager.failFast(context.orgId(), context.executionId(), nodeId, WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR.getId());
                contextRegistry.remove(context.executionId());
                return;
            }
            final NodeWalker.Step step = nodeWalker.step(context, node, false);

            if (step.aborted()) {
                contextRegistry.remove(context.executionId());
                return;
            }

            if (step.parked()) {
                return;
            }
            nodeId = step.nextNodeId();
        }
        executionManager.completeExecution(context.orgId(), context.executionId());
        contextRegistry.remove(context.executionId());
    }

    /**
     * Resumes a parked delay node and continues the graph on the NEXT edge.
     *
     * @param orgId       Represents the organization id.
     * @param executionId execution id
     * @param nodeId      parked delay node id
     */
    public void resumeDelay(final long orgId, final long executionId, final long nodeId) {
        final WorkflowV2Execution execution = executionManager.get(orgId, executionId);

        if (Objects.isNull(execution) || ExecutionStatus.PENDING != execution.getStatus()) {
            LOGGER.info("Workflow V2: Ignoring delay resume for execution {} status {}", executionId,
                    Objects.isNull(execution) ? null : execution.getStatus());
            return;
        }
        final NodeExecutionContext context = contextRegistry.get(orgId, execution.id());

        executionManager.completeNodeAndMarkRunning(orgId, executionId, nodeId);
        walk(context, NodeNavigator.nextNodeId(context.node(nodeId), true));
    }

    /**
     * Expires a wait-for-condition node when the configured span elapses without the condition being met.
     *
     * @param orgId       Represents the organization id.
     * @param executionId Represents the execution id.
     * @param nodeId      parked wait node id
     */
    public void expireWait(final long orgId, final long executionId, final long nodeId) {
        final WorkflowV2Execution execution = executionManager.get(orgId, executionId);

        if (Objects.isNull(execution) || ExecutionStatus.PENDING != execution.getStatus()) {
            LOGGER.info("Workflow V2: Ignoring wait expiry for execution {} status {}", executionId, Objects.isNull(execution) ? null : execution.getStatus());
            return;
        }
        executionManager.expireWait(orgId, executionId, nodeId);
        workflowEmailTrackerManager.delete(orgId, executionId);
        contextRegistry.remove(executionId);
        LOGGER.info("Workflow V2: Wait node {} expired for execution {}", nodeId, executionId);
    }

    /**
     * First-wins wait wakeup from a later entity event on {@code NodeEvent.sourceNodeId}.
     *
     * @param triggerExecutionData incoming entity event
     * @param batchSize            max open waits fetched per batch
     */
    public void resumeMatchingWaits(final TriggerExecutionData triggerExecutionData, final int batchSize) {
        final long orgId = triggerExecutionData.orgId();
        final long totalOpen = executionManager.countOpenWaits(orgId);
        int examined = 0;
        int offset = 0;

        if (totalOpen > 0) {
            LOGGER.info("Workflow V2: Wait matcher — {} open wait(s), batchSize {}", totalOpen, batchSize);
        }

        while (offset < totalOpen) {
            final Collection<WaitRegistration> batch = executionManager.listOpenWaits(orgId, offset, batchSize);

            if (batch.isEmpty()) {
                break;
            }

            for (final Pair<TlcModel, TlcModel> modelPair : triggerExecutionData.models()) {
                for (final WaitRegistration wait : batch) {
                    tryResumeWait(triggerExecutionData, wait, modelPair);
                }
            }
            examined += batch.size();
            offset += batchSize;
        }

        if (totalOpen > 0) {
            LOGGER.info("Workflow V2: Wait matcher complete — examined {}/{} open wait(s)", examined, totalOpen);
        }
    }

    /**
     * Tries to resume the wait.
     *
     * @param triggerExecutionData Represents the {@link TriggerExecutionData}.
     * @param wait                 Represents the {@link WaitRegistration}.
     * @param incomingPair         Represents the incoming pair.
     */
    private void tryResumeWait(final TriggerExecutionData triggerExecutionData, final WaitRegistration wait,
                               final Pair<TlcModel, TlcModel> incomingPair) {
        final Long orgId = wait.getOrgId();
        final Long executionId = wait.getExecutionId();
        final WorkflowV2Execution execution = executionManager.get(orgId, executionId);

        if (ExecutionStatus.PENDING != execution.getStatus()) {
            return;
        }
        final Long watchedId = executionManager.getSourceEntityId(orgId, executionId, wait.getSourceNodeId());

        if (!Objects.equals(incomingPair.getKey().id(), watchedId)) {
            return;
        }
        final NodeExecutionContext context = contextRegistry.get(orgId, execution.id());
        final WorkflowNode waitNode = context.node(wait.getWaitNodeId());
        final WaitForConditionConfiguration waitForConditionConfiguration = WorkflowNodeSupport.waitForConditionConfiguration(waitNode);
        final Collection<WorkflowCriteriaSet> criteriaSets = waitForConditionConfiguration.getCondition().getCriteriaSets();
        final WorkflowCriteriaExecutionStatus status = criteriaEvaluator.evaluate(incomingPair, criteriaSets, ExecutionEntity.ExecutionSource.get(triggerExecutionData.source().getName()));

        if (WorkflowCriteriaExecutionStatus.PASSED != status) {
            return;
        }
        executionManager.completeWaitAndResume(orgId, executionId, wait.getWaitNodeId());
        walk(context, NodeNavigator.nextNodeId(waitNode, true));
    }

    /**
     * Tries to resume the email wait.
     *
     * @param workflowEmailTracker Represents the workflow email tracker.
     */
    public void tryEmailResumeWait(final WorkflowEmailTracker workflowEmailTracker) {
        final Long orgId = workflowEmailTracker.getOrgId();
        final Long executionId = workflowEmailTracker.getExecutionId();
        final WorkflowV2Execution execution = executionManager.get(orgId, executionId);
        final NodeExecutionContext context = contextRegistry.get(orgId, execution.id());
        final Long nodeId = context.nodeId();

        if (ExecutionStatus.PENDING != execution.getStatus()) {
            return;
        }
        final Optional<WaitRegistration> optionalWaitRegistration = executionManager.getWaitRegistration(orgId, executionId, nodeId);

        if (optionalWaitRegistration.isEmpty()) {
            return;
        }
        final Long watchedId = executionManager.getSourceEntityId(orgId, executionId, workflowEmailTracker.getNodeId());

        if (!Objects.equals(workflowEmailTracker.getConversationId(), watchedId)) {
            return;
        }

        final WorkflowNode waitNode = context.node(nodeId);
        final WaitForConditionConfiguration waitForConditionConfiguration = WorkflowNodeSupport.waitForConditionConfiguration(waitNode);
        final WorkflowCriteriaExecutionStatus status = workflowEmailConditionValidator.validate(context, workflowEmailTracker, waitForConditionConfiguration.getCondition());

        if (WorkflowCriteriaExecutionStatus.PASSED != status) {
            return;
        }
        executionManager.completeWaitAndResume(orgId, executionId, waitNode.id());
        walk(context, NodeNavigator.nextNodeId(waitNode, true));
    }

    /**
     * Resumes a parked async action after external success.
     *
     * @param orgId       Represents the organization id.
     * @param executionId Represents the execution id.
     * @param nodeId      Represents the action node id.
     * @param result      Represents the optional action result.
     */
    public void completeAsyncAction(final long orgId, final long executionId, final long nodeId, final TlcModel result) {
        final WorkflowV2Execution execution = executionManager.get(orgId, executionId);

        if (ExecutionStatus.PENDING != execution.getStatus()) {
            LOGGER.info("Workflow V2: Ignoring async action success for execution {} status {}", executionId, execution.getStatus());
            return;
        }
        final NodeExecutionContext context = contextRegistry.get(orgId, executionId);

        if (Objects.isNull(context) || !Objects.equals(context.nodeId(), nodeId)) {
            LOGGER.info("Workflow V2: Ignoring async action success for execution {} node {}", executionId, nodeId);
            return;
        }
        executionManager.completeNodeAndMarkRunning(orgId, executionId, nodeId);
        walk(context, NodeNavigator.nextNodeId(context.node(nodeId), true));
    }

    /**
     * Fails a parked async action after external failure.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @param nodeId      action node id
     * @param reason      optional failure reason id
     */
    public void failAsyncAction(final long orgId, final long executionId, final long nodeId, final Short reason) {
        final WorkflowV2Execution execution = executionManager.get(orgId, executionId);

        if (ExecutionStatus.PENDING != execution.getStatus()) {
            LOGGER.info("Workflow V2: Ignoring async action failure for execution {} status {}", executionId, execution.getStatus());
            return;
        }
        executionManager.failFast(orgId, executionId, nodeId, reason);
        contextRegistry.remove(executionId);
    }

    /**
     * Checks if the trigger scope matches the user id.
     *
     * @param workflow Represents the {@link WorkflowV2}.
     * @param userId   Represents the user id.
     * @return true if the trigger scope matches the user id, false otherwise.
     */
    public boolean matchesTriggerScope(final WorkflowV2 workflow, final Long userId) {
        final TriggerConfiguration triggerConfiguration = WorkflowNodeSupport.configurationAs(workflow.getNodesAsCollection(), TriggerConfiguration.class);

        if (TriggerScope.EVERYONE == triggerConfiguration.getTriggerScope()) {
            return true;
        }
        final Collection<Long> triggerUserIds = triggerConfiguration.getUserIds();

        if (triggerUserIds.isEmpty()) {
            return false;
        }
        return triggerUserIds.contains(userId);
    }

}
