package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.workflow.handler.TriggerModelHandler;
import com.tlc.crm.core.workflow.internal.WorkflowEmailTracker;
import com.tlc.crm.core.workflow.internal.WorkflowEmailTrackerManager;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.internal.trigger.evaluate.WorkflowCriteriaEvaluator;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.ConditionConfiguration;
import com.tlc.crm.workflow.v2.api.model.DelayConfiguration;
import com.tlc.crm.workflow.v2.api.model.EventContextType;
import com.tlc.crm.workflow.v2.api.model.ExecutionEntity;
import com.tlc.crm.workflow.v2.api.model.IfElseConfiguration;
import com.tlc.crm.workflow.v2.api.model.NodeEvent;
import com.tlc.crm.workflow.v2.api.model.TriggerConfiguration;
import com.tlc.crm.workflow.v2.api.model.WaitForConditionConfiguration;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowNodeSupport;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.action.ActionOutcome;
import com.tlc.crm.workflow.v2.internal.action.ActionExecutor;
import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Executes one graph node and returns the next edge, park, or abort.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class NodeWalker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeWalker.class);

    private final WorkflowV2ExecutionManager executionManager;
    private final WorkflowCriteriaEvaluator criteriaEvaluator;
    private final TriggerModelHandler triggerModelHandler;
    private final ActionExecutor actionExecutor;
    private final NodeExecutionContextRegistry contextRegistry;
    private final WorkflowEmailTrackerManager workflowEmailTrackerManager;
    private final WorkflowEmailConditionValidator workflowEmailConditionValidator;

    NodeWalker() {
        this.executionManager = WorkflowV2ExecutionManager.getInstance();
        this.criteriaEvaluator = WorkflowCriteriaEvaluator.getInstance();
        this.actionExecutor = ActionExecutor.getInstance();
        this.triggerModelHandler = TriggerModelHandler.getInstance();
        this.contextRegistry = NodeExecutionContextRegistry.getInstance();
        this.workflowEmailTrackerManager = WorkflowEmailTrackerManager.getInstance();
        this.workflowEmailConditionValidator = WorkflowEmailConditionValidator.getInstance();
    }

    /**
     * Outcome of a single node step.
     *
     * @param nextNodeId next node or null
     * @param parked     delay/wait parked the run
     * @param aborted    fail-fast stopped the run
     */
    public record Step(Long nextNodeId, boolean parked, boolean aborted) {

        static Step next(final Long nodeId) {
            return new Step(nodeId, false, false);
        }

        static Step park() {
            return new Step(null, true, false);
        }

        static Step abort() {
            return new Step(null, false, true);
        }
    }

    /**
     * Runs {@code node}. Callers start the node row unless {@code alreadyStarted} is true.
     *
     * @param context        live graph context
     * @param node           current node
     * @param alreadyStarted true when the node row already exists (unused for park resume)
     * @return next step
     */
    public Step step(final NodeExecutionContext context, final WorkflowNode node, final boolean alreadyStarted) {
        final long orgId = context.orgId();
        final long executionId = context.executionId();

        try {
            if (!alreadyStarted) {
                executionManager.startNode(orgId, executionId, node.id());
            }

            return switch (node.getNodeType()) {
                case TRIGGER -> handleTrigger(context, node);
                case INSTANT_CONDITION -> handleInstantCondition(context, node);
                case IF_ELSE_CONDITION -> handleIfElseCondition(context, node);
                case ACTION -> handleAction(context, node);
                case DELAY -> handleDelay(context, node);
                case WAIT_FOR_CONDITION -> handleWait(context, node);
            };
        } catch (Throwable throwable) {
            LOGGER.error("Workflow V2: Node {} failed for execution {}. Reason: {}", node.id(), executionId, throwable.getMessage(), throwable);
            final Short reason = throwable instanceof ErrorCode
                    ? WorkflowActionExecutionFailureCause.DB_OPERATION_FAILED.getId()
                    : WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR.getId();

            executionManager.failFast(orgId, executionId, node.id(), reason);

            return Step.abort();
        }
    }

    /**
     * Handles the trigger node.
     *
     * @param context Represents the {@link NodeExecutionContext}.
     * @param node    Represents the {@link WorkflowNode}.
     * @return The next step.
     */
    private Step handleTrigger(final NodeExecutionContext context, final WorkflowNode node) {
        final TriggerConfiguration triggerConfiguration = WorkflowNodeSupport.triggerConfiguration(node);
        final TriggerSource.TriggerEvent triggerEvent = triggerConfiguration.getTriggerEvent();
        final Pair<TlcModel, TlcModel> triggerModelPair = context.triggerModelPair();
        final TlcModel tlcModel = Objects.equals(TriggerSource.TriggerEvent.EDITED, triggerEvent)
                ? triggerModelPair.getValue() : triggerModelPair.getKey();

        if (Objects.nonNull(tlcModel)) {
            final String source = triggerConfiguration.getTriggerSource().getName();
            final JsonObject entityJson = triggerModelHandler.getPriorStateJson(Source.get(source), tlcModel);

            executionManager.upsertSourceEntity(new ExecutionEntity(context.orgId(), context.executionId(), node.id(), tlcModel.id(), entityJson, ExecutionEntity.ExecutionSource.get(source)));
        }
        executionManager.completeNode(context.orgId(), context.executionId(), node.id());

        return Step.next(NodeNavigator.nextNodeId(node, true));
    }

    /**
     * Handles the instant condition node.
     *
     * @param context Represents the {@link NodeExecutionContext}.
     * @param node    Represents the {@link WorkflowNode}.
     * @return The next step.
     */
    private Step handleInstantCondition(final NodeExecutionContext context, final WorkflowNode node) {
        final Long orgId = context.orgId();
        final ConditionConfiguration conditionConfiguration = WorkflowNodeSupport.conditionConfiguration(node);
        final NodeEvent nodeEvent = conditionConfiguration.getNodeEvent();
        final ExecutionEntity executionEntity = executionManager.getExecutionEntity(orgId, context.executionId(), nodeEvent.getSourceNodeId());
        final ExecutionEntity.ExecutionSource executionSource = executionEntity.executionSource();
        final WorkflowCriteriaExecutionStatus status = switch (executionSource) {
            case COMPANY, CONTACT, DEAL, ACTIVITY -> {
                final Pair<TlcModel, TlcModel> modelPair = triggerModelHandler.fetchModelPair(orgId, executionEntity.entityId(), Source.get(executionSource.name()), executionEntity.entityJson());

                yield criteriaEvaluator.evaluate(modelPair, conditionConfiguration.getCriteriaSets(), executionSource);
            }
            case EMAIL -> {
                final JsonObject jsonObject = executionEntity.entityJson();
                final WorkflowEmailTracker workflowEmailTracker = workflowEmailTrackerManager.get(context.orgId(), executionEntity.entityId(), jsonObject.getLong(WorkflowAttributesV2.THREAD_ID));

                yield workflowEmailConditionValidator.validate(context, workflowEmailTracker, conditionConfiguration);
            }
        };

        if (WorkflowCriteriaExecutionStatus.PASSED == status) {
            executionManager.completeNode(orgId, context.executionId(), node.id());

            return Step.next(NodeNavigator.nextNodeId(node, true));
        }
        executionManager.failFast(orgId, context.executionId(), node.id(), null);

        return Step.abort();
    }

    /**
     * Handles the if else condition node.
     *
     * @param context Represents the {@link NodeExecutionContext}.
     * @param node    Represents the {@link WorkflowNode}.
     * @return The next step.
     */
    private Step handleIfElseCondition(final NodeExecutionContext context, final WorkflowNode node) {
        final Long orgId = context.orgId();
        final IfElseConfiguration ifElseConfiguration = WorkflowNodeSupport.ifElseConfiguration(node);
        final NodeEvent nodeEvent = ifElseConfiguration.getNodeEvent();
        final ExecutionEntity executionEntity = executionManager.getExecutionEntity(orgId, context.executionId(), nodeEvent.getSourceNodeId());
        final ExecutionEntity.ExecutionSource executionSource = executionEntity.executionSource();
        final WorkflowCriteriaExecutionStatus status = switch (executionSource) {
            case COMPANY, CONTACT, DEAL, ACTIVITY -> {
                final Pair<TlcModel, TlcModel> modelPair = triggerModelHandler.fetchModelPair(orgId, executionEntity.entityId(), Source.get(executionSource.name()), executionEntity.entityJson());

                yield criteriaEvaluator.evaluate(modelPair, ifElseConfiguration.getCondition().getCriteriaSets(), executionSource);
            }
            case EMAIL -> {
                final JsonObject jsonObject = executionEntity.entityJson();
                final WorkflowEmailTracker workflowEmailTracker = workflowEmailTrackerManager.get(context.orgId(), executionEntity.entityId(), jsonObject.getLong(WorkflowAttributesV2.THREAD_ID));

                yield workflowEmailConditionValidator.validate(context, workflowEmailTracker, ifElseConfiguration.getCondition());
            }
        };
        final boolean pass = WorkflowCriteriaExecutionStatus.PASSED == status;

        executionManager.completeNode(context.orgId(), context.executionId(), node.id());
        return Step.next(NodeNavigator.nextNodeId(node, pass));
    }

    /**
     * Handles the action node.
     *
     * @param context Represents the {@link NodeExecutionContext}.
     * @param node    Represents the {@link WorkflowNode}.
     * @return The next step.
     */
    private Step handleAction(final NodeExecutionContext context, final WorkflowNode node) {
        final ActionConfiguration actionConfiguration = WorkflowNodeSupport.actionConfiguration(node);

        if (Objects.isNull(actionConfiguration)) {
            executionManager.failFast(context.orgId(), context.executionId(), node.id(), WorkflowActionExecutionFailureCause.INVALID_DATA.getId());
            return Step.abort();
        }
        final Long userId = context.userId();
        final ActionFailureCauseTracker actionFailureCauseTracker = ActionFailureCauseTracker.create();
        final ActionOutcome outcome = actionExecutor.execute(context.orgId(), userId, actionConfiguration, context, actionFailureCauseTracker);

        if (outcome.isAsyncPending()) {
            executionManager.parkAction(context.orgId(), context.executionId(), node.id());
            contextRegistry.put(context.executionId(), context);

            return Step.park();
        }

        if (outcome.isSyncFailure() || actionFailureCauseTracker.hasErrors()) {
            final WorkflowActionExecutionFailureCause cause = actionFailureCauseTracker.getFailureCause(actionConfiguration.id());

            executionManager.failFast(context.orgId(), context.executionId(), node.id(), Objects.isNull(cause) ? WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR.getId() : cause.getId());
            return Step.abort();
        }
        executionManager.completeNode(context.orgId(), context.executionId(), node.id());

        return Step.next(NodeNavigator.nextNodeId(node, true));
    }

    /**
     * Handles the delay node.
     *
     * @param context Represents the {@link NodeExecutionContext}.
     * @param node    Represents the {@link WorkflowNode}.
     * @return The next step.
     */
    private Step handleDelay(final NodeExecutionContext context, final WorkflowNode node) {
        final DelayConfiguration configuration = WorkflowNodeSupport.delayConfiguration(node);

        if (Objects.isNull(configuration) || Objects.isNull(configuration.getSpan())) {
            throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_CONFIGURATION, "i18n.workflow.v2.graph.span.config.required");
        }
        final Long orgId = context.orgId();
        final long fireAt = SpanCalculator.fireAt(orgId, context.userId(), configuration.getSpan());

        executionManager.parkDelay(orgId, context.executionId(), node.id(), fireAt);
        contextRegistry.put(context.executionId(), context);

        return Step.park();
    }

    /**
     * Handles the wait node.
     *
     * @param context Represents the {@link NodeExecutionContext}.
     * @param node    Represents the {@link WorkflowNode}.
     * @return The next step.
     */
    private Step handleWait(final NodeExecutionContext context, final WorkflowNode node) {
        final WaitForConditionConfiguration configuration = WorkflowNodeSupport.waitForConditionConfiguration(node);

        if (Objects.isNull(configuration) || Objects.isNull(configuration.getSpan())) {
            throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_CONFIGURATION, "i18n.workflow.v2.graph.span.config.required");
        }
        final NodeEvent nodeEvent = configuration.getNodeEvent();
        final EventContextType contextType = nodeEvent.getContextType();
        final Long sourceNodeId = nodeEvent.getSourceNodeId();
        final Long orgId = context.orgId();
        final long fireAt = SpanCalculator.fireAt(orgId, context.userId(), configuration.getSpan());

        executionManager.parkWait(orgId, context.executionId(), node.id(), sourceNodeId, contextType, fireAt);
        contextRegistry.put(context.executionId(), context);

        return Step.park();
    }
}
