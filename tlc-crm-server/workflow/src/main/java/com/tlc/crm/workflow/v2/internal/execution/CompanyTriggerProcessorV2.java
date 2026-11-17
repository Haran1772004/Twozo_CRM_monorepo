package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.api.contact.model.Company;

import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Company trigger processor for the workflow V2.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class CompanyTriggerProcessorV2 extends AbstractTriggerProcessorV2<Company> {

    private CompanyTriggerProcessorV2() {
    }

    private static class Instance {
        private static final CompanyTriggerProcessorV2 INSTANCE = new CompanyTriggerProcessorV2();
    }

    /**
     * Gets the {@link CompanyTriggerProcessorV2} instance.
     *
     * @return The instance of {@link CompanyTriggerProcessorV2}.
     */
    public static CompanyTriggerProcessorV2 getInstance() {
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
            loadCompanyUserIds(triggerExecutionData.models(), userIds);
            triggerService.collectUserIds(triggerExecutionData.orgId(), userIds, workflows);

            invoke(userId, triggerExecutionData, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of a company to fetch the user details.
     *
     * @param companyPairs Represents the company pairs.
     * @param userIds      Represents the {@link Set} of userIds.
     */
    private void loadCompanyUserIds(final Collection<Pair<TlcModel, TlcModel>> companyPairs, final Set<Long> userIds) {
        final Consumer<Company> userIdCollector = (company) -> {
            userIds.add(company.getSalesOwner().id());
            userIds.add(company.getCreatedBy().id());
            userIds.add(company.getUpdatedBy().id());
        };

        for (final Pair<TlcModel, TlcModel> companyPair : companyPairs) {
            final Company oldCompany = (Company) companyPair.getValue();

            userIdCollector.accept((Company) companyPair.getKey());

            if (Objects.nonNull(oldCompany)) {
                userIdCollector.accept(oldCompany);
            }
        }
    }
}
