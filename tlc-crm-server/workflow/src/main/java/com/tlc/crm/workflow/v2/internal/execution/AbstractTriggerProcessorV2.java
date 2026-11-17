package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.internal.trigger.handler.TriggerRequestHandler;
import com.tlc.crm.workflow.internal.trigger.service.TriggerRequestService;
import com.tlc.crm.workflow.internal.trigger.service.impl.TriggerService;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract trigger processor for the workflow V2.
 *
 * @param <T> Represents the type of the model.
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
abstract class AbstractTriggerProcessorV2<T extends TlcModel> implements TriggerProcessorV2<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTriggerProcessorV2.class);

    protected final TriggerService triggerService;
    protected final TriggerRequestHandler triggerRequestHandler;

    private final OrganizationService organizationService;
    private final UserService iamUserService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final WorkflowV2Manager workflowV2Manager;
    private final WorkflowV2ExecutionEngine workflowV2ExecutionEngine;
    private final WorkflowV2ExecutionManager executionManager;

    protected AbstractTriggerProcessorV2() {
        this.triggerService = TriggerService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.iamUserService = UserService.getInstance();
        this.crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        this.triggerRequestHandler = TriggerRequestService.get().getTriggerHandler();
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.workflowV2ExecutionEngine = WorkflowV2ExecutionEngine.getInstance();
        this.executionManager = WorkflowV2ExecutionManager.getInstance();

    }

    /**
     * Initiates the trigger process to forward the request to another executor,
     * fetching user details upon success in the {@link EventCallback}.
     *
     * @param triggerExecutionData Refers the {@link TriggerExecutionData} which holds the necessary data for trigger create event.
     * @param workflows            Refers the collection of workflow.
     * @param userIds              Refers the user ids.
     */
    protected final void invoke(final Long userId, final TriggerExecutionData triggerExecutionData, final Collection<WorkflowV2> workflows, final Set<Long> userIds) {
        final Long orgId = triggerExecutionData.orgId();
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param users Response from the consumer
             */
            @Override
            public void onSuccess(final Collection<User> users) {
                final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                triggerExecutionData.addUsers(userMap);
                triggerRequestHandler.execute(() -> {

                    try {
                        tProcess(userId, triggerExecutionData, workflows);
                    } catch (Exception exception) {
                        LOGGER.error("Workflow Execution: Failed to process the trigger invoke for OrgId: {}, Reason: {}", triggerExecutionData.orgId(), exception.getMessage());
                        triggerExecutionData.clean();
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
                LoggerUtil.logError(LOGGER, throwable, "Workflow Execution: User retrieval failed for OrgId: {}", orgId);
            }
        };

        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId),
                crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).toList(), usersFetchCallBack);
    }

    /**
     * Processes the trigger.
     *
     * @param userId               Represents the user id.
     * @param triggerExecutionData Represents the trigger execution data.
     * @param workflows            Represents the workflows.
     */
    private void tProcess(final Long userId, final TriggerExecutionData triggerExecutionData, final Collection<WorkflowV2> workflows) {
        for (final Pair<TlcModel, TlcModel> modelPair : triggerExecutionData.models()) {
            for (final WorkflowV2 workflowV2 : workflows) {

                if (workflowV2ExecutionEngine.matchesTriggerScope(workflowV2, userId)) {
                    triggerRequestHandler.execute(() -> {
                        try {
                            final Long executionId = executionManager.create(userId, workflowV2);
                            final NodeExecutionContext nodeExecutionContext = new NodeExecutionContext(triggerExecutionData.orgId(),
                                    userId, modelPair, triggerExecutionData.needNestedTrigger(),
                                    triggerExecutionData.getNestedTriggerInfo(), workflowV2, executionId,
                                    triggerExecutionData.groupedUsers(), Pair.of(triggerExecutionData.source(), modelPair.getKey()));

                            workflowV2ExecutionEngine.execute(nodeExecutionContext);
                        } catch (Throwable throwable) {
                            LoggerUtil.logError(LOGGER, throwable, "Workflow V2 Execution: Trigger execution failed for Org Id: {}, Reason: {}", triggerExecutionData.orgId(), throwable.getMessage());
                        }
                    });
                }

                if (triggerExecutionData.needNestedTrigger()) {
                    final JsonObject nestedTriggerInfo = triggerExecutionData.getNestedTriggerInfo();

                    if (Objects.nonNull(nestedTriggerInfo)) {
                        final TriggerSource source = triggerExecutionData.source();
                        final Long orgId = triggerExecutionData.orgId();

                        switch (source) {
                            case CONTACT -> {
                                final Contact contact = (Contact) modelPair.getKey();
                                final Company company = contact.getCompany();

                                if (Objects.nonNull(company) && nestedTriggerInfo.getJsonObject(String.valueOf(contact.id())).optBoolean(Source.COMPANY.getName())) {
                                    executeNestedCompanyTrigger(orgId, userId, company, triggerExecutionData);
                                }
                            }
                            case DEAL -> {
                                final Deal deal = (Deal) modelPair.getKey();
                                final JsonObject jsonObject = nestedTriggerInfo.getJsonObject(String.valueOf(deal.id()));
                                final Contact contact = deal.getPrimaryContact();
                                final Company company = deal.getCompany();

                                if (Objects.nonNull(contact) && jsonObject.optBoolean(Source.CONTACT.getName())) {
                                    executeNestedContactTrigger(orgId, userId, contact, triggerExecutionData);
                                }

                                if (Objects.nonNull(company) && jsonObject.optBoolean(Source.COMPANY.getName())) {
                                    executeNestedCompanyTrigger(orgId, userId, company, triggerExecutionData);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes the nested company trigger.
     *
     * @param orgId                Represents the organization id.
     * @param userId               Represents the user id.
     * @param company              Represents the company.
     * @param triggerExecutionData Represents the trigger execution data.
     */
    private void executeNestedCompanyTrigger(final long orgId, final long userId, final Company company, final TriggerExecutionData triggerExecutionData) {
        final TriggerSource companySource = TriggerSource.COMPANY;

        final TriggerExecutionData companyTriggerContext = new TriggerExecutionData(orgId, List.of(Pair.of(CompanyManager.getInstance().get(orgId, company.id()), null)),
                companySource, triggerExecutionData.modelSource(), false, null, new HashMap<>(), null);

        CompanyTriggerProcessorV2.getInstance().handle(userId, companyTriggerContext,
                workflowV2Manager.getWorkflowsByTrigger(orgId, companySource, TriggerSource.TriggerEvent.CREATED));
    }

    /**
     * Executes the nested contact trigger.
     *
     * @param orgId                Represents the organization id.
     * @param userId               Represents the user id.
     * @param contact              Represents the contact.
     * @param triggerExecutionData Represents the trigger execution data.
     */
    private void executeNestedContactTrigger(final long orgId, final long userId, final Contact contact, final TriggerExecutionData triggerExecutionData) {
        final TriggerSource contactSource = TriggerSource.CONTACT;

        final TriggerExecutionData contactTriggerContext = new TriggerExecutionData(orgId, List.of(Pair.of(ContactManager.getInstance().get(orgId, contact.id()), null)),
                contactSource, triggerExecutionData.modelSource(), false, null, new HashMap<>(), null);

        ContactTriggerProcessorV2.getInstance().handle(userId, contactTriggerContext,
                workflowV2Manager.getWorkflowsByTrigger(orgId, contactSource, TriggerSource.TriggerEvent.CREATED));
    }
}
