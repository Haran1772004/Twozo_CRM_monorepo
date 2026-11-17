package com.tlc.crm.workflow.internal.trigger.manager;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;

import com.tlc.crm.workflow.internal.trigger.model.TriggerContext;
import com.tlc.crm.workflow.internal.trigger.model.navigate.WorkflowNavigator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Responsible for commencing deal-related trigger actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class DealTriggerManager extends AbstractTriggerManager<Deal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealTriggerManager.class);

    private final CompanyManager companyManager;
    private final ContactManager contactManager;
    private final CompanyTriggerManager companyTriggerManager;
    private final ContactTriggerManager contactTriggerManager;

    private DealTriggerManager() {
        super(TriggerSource.DEAL);
        this.companyManager = CompanyManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyTriggerManager = CompanyTriggerManager.getInstance();
        this.contactTriggerManager = ContactTriggerManager.getInstance();
    }

    private static class Instance {
        private static final DealTriggerManager INSTANCE = new DealTriggerManager();
    }

    /**
     * Gets the {@link DealTriggerManager} instance.
     *
     * @return The instance of {@link DealTriggerManager}.
     */
    public static DealTriggerManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerCreateEvent(final TriggerContext<Deal> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger update event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerUpdateEvent(final TriggerContext<Deal> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger delete event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerDeleteEvent(final TriggerContext<Deal> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of workflow.
     */
    @Override
    protected void tProcess(final TriggerContext<Deal> triggerContext, final Collection<Workflow> workflows) {
        final long orgId = triggerContext.orgId();

        for (final Pair<Deal, Deal> dealPair : triggerContext.models()) {
            final Deal deal = dealPair.getKey();
            final WorkflowNavigator workflowNavigator = new WorkflowNavigator((List<Workflow>) workflows);
            final Long dealId = deal.id();

            while (workflowNavigator.hasNext()) {
                final Workflow workflow = workflowNavigator.next();

                if (null != workflow) {
                    triggerRequestHandler.execute(() -> {

                        try {
                            final Long workflowId = workflow.id();

                            LOGGER.info("Workflow Execution: Trigger execution initiated for Org ID: {}, Deal ID: {}, Workflow ID: {}", orgId, dealId, workflowId);

                            final Long executionHistoryId = workflowExecutionHistoryManager.create(getWorkflowExecutionHistory(orgId, workflow));
                            final Collection<WorkflowCriteriaSet> workflowCriteriaSets = workflow.getCriteriaSets();

                            if (workflowCriteriaSets.isEmpty()) {
                                invokeActions(deal, workflow, executionHistoryId, triggerContext);
                            } else {

                                try {
                                    LOGGER.info("Workflow Execution: Expression evaluation process initiated for Org ID: {}, Deal ID: {}, Workflow ID: {}", orgId, dealId, workflowId);

                                    final WorkflowCriteriaExecutionStatus workflowCriteriaExecutionStatus = criteriaEvaluator
                                            .evaluate(dealPair, workflowCriteriaSets, triggerSource);

                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, workflowCriteriaExecutionStatus);
                                    LOGGER.info("Workflow Execution: Expression evaluation process completed with Status: {} for Org ID: {}, Deal ID: {}, Workflow ID: {}",
                                            workflowCriteriaExecutionStatus, orgId, dealId, workflowId);

                                    if (workflowCriteriaExecutionStatus == WorkflowCriteriaExecutionStatus.PASSED) {
                                        invokeActions(deal, workflow, executionHistoryId, triggerContext);
                                    } else {
                                        workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);
                                    }
                                } catch (Throwable cause) {
                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, WorkflowCriteriaExecutionStatus.FAILED);
                                    workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);

                                    LoggerUtil.logError(LOGGER, cause, "Workflow Execution: Expression evaluation failed for Deal ID: {}, Workflow ID: {}, Reason: {}", dealId, workflowId, cause.getMessage());
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

                if (Objects.nonNull(nestedTriggerInfo)) {
                    final JsonObject jsonObject = nestedTriggerInfo.getJsonObject(String.valueOf(dealId));
                    final Contact contact = deal.getPrimaryContact();
                    final Company company = deal.getCompany();
                    final Map<Long, Long> entityStateIdMap = triggerContext.getEntityStateIdMap();

                    if (Objects.nonNull(contact) && jsonObject.optBoolean(Source.CONTACT.getName())) {
                        final TriggerSource contactSource = TriggerSource.CONTACT;
                        final TriggerContext<Contact> contactTriggerContext = new TriggerContext<>(orgId,
                                List.of(Pair.of(contactManager.get(orgId, contact.id()), null)), contactSource,
                                triggerContext.modelSource(), false, null, entityStateIdMap, null);

                        contactTriggerManager.triggerCreateEvent(contactTriggerContext,
                                workflowManager.getWorkflowByTrigger(orgId, contactSource, TriggerEvent.CREATED));
                    }

                    if (Objects.nonNull(company) && jsonObject.optBoolean(Source.COMPANY.getName())) {
                        final TriggerSource companySource = TriggerSource.COMPANY;
                        final TriggerContext<Company> companyTriggerContext = new TriggerContext<>(orgId,
                                List.of(Pair.of(companyManager.get(orgId, company.id()), null)), companySource,
                                triggerContext.modelSource(), false, null, entityStateIdMap, null);

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
    private void collectUserIdsAndInvoke(final TriggerContext<Deal> triggerContext, final Collection<Workflow> workflows) {
        if (!workflows.isEmpty()) {
            final Set<Long> userIds = new HashSet<>();

            loadDealUserIds(triggerContext.models(), userIds);
            triggerService.collectUserIds(triggerContext.orgId(), workflows, userIds);

            invoke(triggerContext, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of contact to fetch the user details.
     *
     * @param dealPairs Refers the company pairs.
     * @param userIds   Refers the {@link Set} of user ids.
     */
    private void loadDealUserIds(final Collection<Pair<Deal, Deal>> dealPairs, final Set<Long> userIds) {
        final Consumer<Deal> userIdCollector = (deal) -> {
            userIds.add(deal.getSalesOwner().id());
            userIds.add(deal.getCreatedBy().id());
            userIds.add(deal.getUpdatedBy().id());
        };

        for (final Pair<Deal, Deal> dealPair : dealPairs) {
            final Deal oldDeal = dealPair.getValue();

            userIdCollector.accept(dealPair.getKey());

            if (Objects.nonNull(oldDeal)) {
                userIdCollector.accept(oldDeal);
            }
        }
    }
}
