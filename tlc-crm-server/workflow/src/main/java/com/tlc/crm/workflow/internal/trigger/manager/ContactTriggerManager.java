package com.tlc.crm.workflow.internal.trigger.manager;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
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
 * Responsible for commencing contact-related trigger actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ContactTriggerManager extends AbstractTriggerManager<Contact> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactTriggerManager.class);

    private final CompanyManager companyManager;
    private final CompanyTriggerManager companyTriggerManager;

    private ContactTriggerManager() {
        super(TriggerSource.CONTACT);
        this.companyManager = CompanyManager.getInstance();
        this.companyTriggerManager = CompanyTriggerManager.getInstance();
    }

    private static class Instance {
        private static final ContactTriggerManager INSTANCE = new ContactTriggerManager();
    }

    /**
     * Gets the {@link ContactTriggerManager} instance.
     *
     * @return The instance of {@link ContactTriggerManager}.
     */
    public static ContactTriggerManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerCreateEvent(final TriggerContext<Contact> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger update event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerUpdateEvent(final TriggerContext<Contact> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger delete event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerDeleteEvent(final TriggerContext<Contact> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of workflow.
     */
    @Override
    protected void tProcess(final TriggerContext<Contact> triggerContext, final Collection<Workflow> workflows) {
        final long orgId = triggerContext.orgId();

        for (final Pair<Contact, Contact> contactPair : triggerContext.models()) {
            final Contact contact = contactPair.getKey();
            final WorkflowNavigator workflowNavigator = new WorkflowNavigator((List<Workflow>) workflows);
            final Long contactId = contact.id();

            while (workflowNavigator.hasNext()) {
                final Workflow workflow = workflowNavigator.next();

                if (null != workflow) {
                    triggerRequestHandler.execute(() -> {

                        try {
                            final Long workflowId = workflow.id();

                            LOGGER.info("Workflow Execution: Trigger execution initiated for Org ID: {}, Contact ID: {}, Workflow ID: {}", orgId, contactId, workflowId);

                            final Long executionHistoryId = workflowExecutionHistoryManager.create(getWorkflowExecutionHistory(orgId, workflow));
                            final Collection<WorkflowCriteriaSet> workflowCriteriaSets = workflow.getCriteriaSets();

                            if (workflowCriteriaSets.isEmpty()) {
                                invokeActions(contact, workflow, executionHistoryId, triggerContext);
                            } else {

                                try {
                                    LOGGER.info("Workflow Execution: Expression evaluation process initiated for Org ID: {}, Contact ID: {}, Workflow ID: {}", orgId, contactId, workflowId);

                                    final WorkflowCriteriaExecutionStatus workflowCriteriaExecutionStatus = criteriaEvaluator
                                            .evaluate(contactPair, workflowCriteriaSets, triggerSource);

                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, workflowCriteriaExecutionStatus);
                                    LOGGER.info("Workflow Execution: Expression evaluation process completed with Status: {} for Org ID: {}, Contact ID: {}, Workflow ID: {}",
                                            workflowCriteriaExecutionStatus, orgId, contactId, workflowId);

                                    if (workflowCriteriaExecutionStatus == WorkflowCriteriaExecutionStatus.PASSED) {
                                        invokeActions(contact, workflow, executionHistoryId, triggerContext);
                                    } else {
                                        workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);
                                    }
                                } catch (Throwable cause) {
                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, WorkflowCriteriaExecutionStatus.FAILED);
                                    workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);

                                    LoggerUtil.logError(LOGGER, cause, "Workflow Execution: Expression evaluation failed for Contact ID: {}, Workflow ID: {}, Reason: {}", contactId, workflowId, cause.getMessage());
                                }
                            }
                        } finally {
                            triggerContext.clean();
                        }
                    });
                }
            }

            if (triggerContext.needNestedTrigger()) {
                final JsonObject nestedTriggerInfo = triggerContext.getNestedTriggerInfo();

                if (Objects.nonNull(nestedTriggerInfo) && nestedTriggerInfo.getJsonObject(String.valueOf(contactId)).optBoolean(Source.COMPANY.getName())) {
                    final Company company = contact.getCompany();

                    if (Objects.nonNull(company)) {
                        final TriggerSource companySource = TriggerSource.COMPANY;
                        final TriggerContext<Company> companyTriggerContext = new TriggerContext<>(orgId,
                                List.of(Pair.of(companyManager.get(orgId, company.id()), null)), companySource,
                                triggerContext.modelSource(), false, null, triggerContext.getEntityStateIdMap(), null);

                        companyTriggerManager.triggerCreateEvent(companyTriggerContext,
                                workflowManager.getWorkflowByTrigger(orgId, companySource, TriggerEvent.CREATED));
                    }
                }
            }
        }
    }

    /**
     * Invokes the trigger execution process after collecting the user ids to fetch the user details using event callback.
     *
     * @param triggerContext Refers the {@link TriggerContext}.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    private void collectUserIdsAndInvoke(final TriggerContext<Contact> triggerContext, final Collection<Workflow> workflows) {
        if (!workflows.isEmpty()) {
            final Set<Long> userIds = new HashSet<>();

            loadContactUserIds(triggerContext.models(), userIds);
            triggerService.collectUserIds(triggerContext.orgId(), workflows, userIds);

            invoke(triggerContext, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of contact to fetch the user details.
     *
     * @param contactPairs Refers the company pairs.
     * @param userIds      Refers the {@link Set} of user ids.
     */
    private void loadContactUserIds(final Collection<Pair<Contact, Contact>> contactPairs, final Set<Long> userIds) {
        final Consumer<Contact> userIdCollector = (contact) -> {
            userIds.add(contact.getSalesOwner().id());
            userIds.add(contact.getCreatedBy().id());
            userIds.add(contact.getUpdatedBy().id());
        };

        for (final Pair<Contact, Contact> contactPair : contactPairs) {
            final Contact oldContact = contactPair.getValue();

            userIdCollector.accept(contactPair.getKey());

            if (Objects.nonNull(oldContact)) {
                userIdCollector.accept(oldContact);
            }
        }
    }
}
