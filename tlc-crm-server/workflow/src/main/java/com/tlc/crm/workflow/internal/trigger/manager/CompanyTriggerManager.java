package com.tlc.crm.workflow.internal.trigger.manager;

import com.tlc.commons.pair.Pair;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.api.contact.model.Company;

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
 * Responsible for commencing company-related trigger actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class CompanyTriggerManager extends AbstractTriggerManager<Company> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyTriggerManager.class);

    private CompanyTriggerManager() {
        super(TriggerSource.COMPANY);
    }

    private static class Instance {
        private static final CompanyTriggerManager INSTANCE = new CompanyTriggerManager();
    }

    /**
     * Gets the {@link CompanyTriggerManager} instance.
     *
     * @return The instance of {@link CompanyTriggerManager}.
     */
    public static CompanyTriggerManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerCreateEvent(final TriggerContext<Company> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger update event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerUpdateEvent(final TriggerContext<Company> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger delete event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    @Override
    public void triggerDeleteEvent(final TriggerContext<Company> triggerContext, final Collection<Workflow> workflows) {
        collectUserIdsAndInvoke(triggerContext, workflows);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of workflow.
     */
    @Override
    protected void tProcess(final TriggerContext<Company> triggerContext, final Collection<Workflow> workflows) {
        final long orgId = triggerContext.orgId();

        for (final Pair<Company, Company> companyPair : triggerContext.models()) {
            final Company company = companyPair.getKey();
            final WorkflowNavigator workflowNavigator = new WorkflowNavigator((List<Workflow>) workflows);

            while (workflowNavigator.hasNext()) {
                final Workflow workflow = workflowNavigator.next();

                if (null != workflow) {
                    triggerRequestHandler.execute(() -> {

                        try {
                            final Long companyId = company.id();
                            final Long workflowId = workflow.id();

                            LOGGER.info("Workflow Execution: Trigger execution initiated for Org ID: {}, Company ID: {}, Workflow ID: {}", orgId, companyId, workflowId);

                            final Long executionHistoryId = workflowExecutionHistoryManager.create(getWorkflowExecutionHistory(orgId, workflow));
                            final Collection<WorkflowCriteriaSet> workflowCriteriaSets = workflow.getCriteriaSets();

                            if (workflowCriteriaSets.isEmpty()) {
                                invokeActions(company, workflow, executionHistoryId, triggerContext);
                            } else {

                                try {
                                    LOGGER.info("Workflow Execution: Expression evaluation process initiated for Org ID: {}, Company ID: {}, Workflow ID: {}", orgId, companyId, workflowId);

                                    final WorkflowCriteriaExecutionStatus workflowCriteriaExecutionStatus = criteriaEvaluator
                                            .evaluate(companyPair, workflowCriteriaSets, triggerSource);

                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, workflowCriteriaExecutionStatus);
                                    LOGGER.info("Workflow Execution: Expression evaluation process completed with Status: {} for Org ID: {}, Company ID: {}, Workflow ID: {}",
                                            workflowCriteriaExecutionStatus, orgId, companyId, workflowId);

                                    if (workflowCriteriaExecutionStatus == WorkflowCriteriaExecutionStatus.PASSED) {
                                        invokeActions(company, workflow, executionHistoryId, triggerContext);
                                    } else {
                                        workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);
                                    }
                                } catch (Throwable cause) {
                                    workflowExecutionHistoryManager.updateCriteriaExecutionStatus(orgId, executionHistoryId, WorkflowCriteriaExecutionStatus.FAILED);
                                    workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);

                                    LoggerUtil.logError(LOGGER, cause, "Workflow Execution: Expression evaluation failed for Company ID: {}, Workflow ID: {}, Reason: {}", companyId, workflowId, cause.getMessage());
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
     * Invokes the trigger execution process after collecting the user ids to fetch the user details using event callback.
     *
     * @param triggerContext Refers the {@link TriggerContext}.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    private void collectUserIdsAndInvoke(final TriggerContext<Company> triggerContext, final Collection<Workflow> workflows) {
        if (!workflows.isEmpty()) {
            final Set<Long> userIds = new HashSet<>();

            loadCompanyUserIds(triggerContext.models(), userIds);
            triggerService.collectUserIds(triggerContext.orgId(), workflows, userIds);

            invoke(triggerContext, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of a company to fetch the user details.
     *
     * @param companyPairs Refers the company pairs.
     * @param userIds      Refers the {@link Set} of userIds.
     */
    private void loadCompanyUserIds(final Collection<Pair<Company, Company>> companyPairs, final Set<Long> userIds) {
        final Consumer<Company> userIdCollector = (company) -> {
            userIds.add(company.getSalesOwner().id());
            userIds.add(company.getCreatedBy().id());
            userIds.add(company.getUpdatedBy().id());
        };

        for (final Pair<Company, Company> companyPair : companyPairs) {
            final Company oldCompany = companyPair.getValue();

            userIdCollector.accept(companyPair.getKey());

            if (Objects.nonNull(oldCompany)) {
                userIdCollector.accept(oldCompany);
            }
        }
    }
}
