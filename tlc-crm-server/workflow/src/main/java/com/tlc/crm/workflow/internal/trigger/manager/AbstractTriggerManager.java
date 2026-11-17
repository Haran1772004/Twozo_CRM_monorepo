package com.tlc.crm.workflow.internal.trigger.manager;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.commons.json.Json;
import com.tlc.commons.pair.Pair;

import com.tlc.validator.TlcModel;
import com.tlc.web.client.event.EventCallback;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.workflow.api.manager.history.WorkflowExecutionHistoryManager;
import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.converter.WorkflowConverter;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionHistory;
import com.tlc.crm.workflow.field.WorkflowAttributes;

import com.tlc.crm.workflow.internal.trigger.service.TriggerRequestService;
import com.tlc.crm.workflow.internal.trigger.action.publish.ActionPublisher;
import com.tlc.crm.workflow.internal.trigger.handler.TriggerRequestHandler;
import com.tlc.crm.workflow.internal.trigger.action.ActionDataProcessor;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.evaluate.WorkflowCriteriaEvaluator;
import com.tlc.crm.workflow.internal.trigger.action.navigate.ActionNavigator;
import com.tlc.crm.workflow.internal.trigger.service.impl.TriggerService;
import com.tlc.crm.workflow.internal.trigger.model.TriggerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An abstract way for delivering the {@link TriggerManager} to initiate various triggers actions.
 *
 * @param <T> Refers the type of the model.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class AbstractTriggerManager<T extends TlcModel> implements TriggerManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTriggerManager.class);

    protected final WorkflowManager workflowManager;
    protected final WorkflowExecutionHistoryManager workflowExecutionHistoryManager;
    protected final TriggerService triggerService;
    protected final TriggerSource triggerSource;
    protected final TriggerRequestHandler triggerRequestHandler;
    protected final WorkflowCriteriaEvaluator criteriaEvaluator;

    private final OrganizationService organizationService;
    private final UserService iamUserService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final ActionDataProcessor actionDataProcessor;
    private final ActionPublisher actionPublisher;
    private final WorkflowConverter workflowConverter;

    protected AbstractTriggerManager(final TriggerSource triggerSource) {
        this.triggerSource = triggerSource;
        this.workflowManager = WorkflowManager.getInstance();
        this.workflowExecutionHistoryManager = WorkflowExecutionHistoryManager.getInstance();
        this.triggerService = TriggerService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.iamUserService = UserService.getInstance();
        this.crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        this.triggerRequestHandler = TriggerRequestService.get().getTriggerHandler();
        this.criteriaEvaluator = WorkflowCriteriaEvaluator.getInstance();
        this.actionDataProcessor = ActionDataProcessor.getInstance();
        this.actionPublisher = ActionPublisher.getInstance();
        this.workflowConverter = WorkflowConverter.getInstance();
    }

    /**
     * Initiates the trigger process to forward the request to another executor,
     * fetching user details upon success in the {@link EventCallback}.
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of workflow.
     * @param userIds        Refers the user ids.
     */
    protected final void invoke(final TriggerContext<T> triggerContext, final Collection<Workflow> workflows, final Set<Long> userIds) {
        final Long orgId = triggerContext.orgId();
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param users Response from the consumer
             */
            @Override
            public void onSuccess(final Collection<User> users) {
                LOGGER.info("Workflow Execution: Successfully retrieved user information for OrgId: {} Trigger: {}", orgId, triggerSource.name());
                final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                triggerContext.addUsers(userMap);
                triggerRequestHandler.execute(() -> {

                    try {
                        tProcess(triggerContext, workflows);
                    } catch (Exception exception) {
                        LOGGER.error("Workflow Execution: Failed to process the trigger invoke for OrgId: {}, Reason: {}", triggerContext.orgId(), exception.getMessage());
                        triggerContext.clean();
                    }
                });
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure
             */
            @Override
            public void onFailure(final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Workflow Execution: User retrieval failed for OrgId: {} Trigger: {}", orgId, triggerSource.name());
            }
        };

        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId),
                crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).toList(), usersFetchCallBack);
    }

    /**
     * Processes the trigger actions of a model.
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of workflow.
     */
    protected abstract void tProcess(final TriggerContext<T> triggerContext, final Collection<Workflow> workflows);

    /**
     * Retrieves the execution history of a specified workflow for a given organization.
     *
     * @param orgId    The unique identifier of the organization.
     * @param workflow The workflow for which the execution history is to be retrieved.
     * @return A {@link WorkflowExecutionHistory} containing the history of the workflow execution.
     */
    protected final WorkflowExecutionHistory getWorkflowExecutionHistory(final long orgId, final Workflow workflow) {
        final JsonObject workflowJson = workflowConverter.convert(new ConverterConfig(orgId, workflow.getOwnerId(),
                Locale.getDefault(), false, null), workflow);

        workflowJson.put(WorkflowAttributes.USER_IDS, workflow.getUserIds());
        final WorkflowExecutionHistory workflowExecutionHistory = new WorkflowExecutionHistory(orgId, workflow.id());

        workflowExecutionHistory.setWorkflowState(workflowJson);
        return workflowExecutionHistory;
    }

    /**
     * Invokes the {@link WorkflowAction} execution process.
     *
     * @param model              Refers the model.
     * @param workflow           Refers the {@link Workflow}.
     * @param executionHistoryId Refers the execution history id.
     * @param triggerContext     Refers the {@link TriggerContext}
     */
    protected final void invokeActions(final T model, final Workflow workflow,
                                       final Long executionHistoryId, final TriggerContext<T> triggerContext) {
        final Long orgId = model.orgId();
        final ActionNavigator actionNavigator = new ActionNavigator((List<WorkflowAction>) workflow.getActions());
        final ActionExecutionContext actionExecutionContext = new ActionExecutionContext(Pair.of(triggerSource, model), actionNavigator,
                triggerContext.groupedUsers(), executionHistoryId, List.of(), Json.object(), triggerContext.getEntityStateIdMap());
        final String actionRequestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s:%s", orgId, workflow.getOwnerId(),
                executionHistoryId, triggerSource.getName()).getBytes());

        actionPublisher.publish(actionRequestId, actionDataProcessor.wrapData(orgId, workflow, actionExecutionContext));
        LOGGER.info("Workflow Execution: Action request published for Org ID: {}, Entity: {}, ID: {}, Workflow ID: {}", orgId, triggerSource, model.id(), workflow.id());
    }
}
