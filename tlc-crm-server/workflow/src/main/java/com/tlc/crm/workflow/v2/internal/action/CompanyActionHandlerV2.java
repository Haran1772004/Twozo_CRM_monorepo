package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.AbstractActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.internal.action.formatter.CompanyJsonFormatterV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.iam.common.models.User;

import java.util.Map;
import java.util.Objects;

/**
 * Handles the company actions for the workflow.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class CompanyActionHandlerV2 extends AbstractActionHandler {

    private final CompanyJsonFormatterV2 companyJsonFormatter;
    private final CompanyConverter companyConverter;
    private final CompanyManager companyManager;
    private final CompanyValidator companyValidator;

    private CompanyActionHandlerV2() {
        this.companyJsonFormatter = CompanyJsonFormatterV2.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.companyValidator = new CompanyValidator();
    }

    private static class Instance {
        private static final CompanyActionHandlerV2 INSTANCE = new CompanyActionHandlerV2();
    }

    /**
     * Gets the instance of {@link CompanyActionHandlerV2}.
     *
     * @return {@link CompanyActionHandlerV2} instance.
     */
    static CompanyActionHandlerV2 getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates the company.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The company.
     */
    Company create(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                   final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject companyJson = companyJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);
        final Company company = companyConverter.convert(new ConverterConfig(orgId, userId, null, false, companyJson), companyJson);

        if (companyValidator.validate(company, nodeExecutionContext)) {
            final Company createdCompany = companyManager.create(company);

            insertSourceEntity(nodeExecutionContext, createdCompany, actionConfiguration);
            nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), createdCompany));
            index(orgId, createdCompany, Action.CREATE);

            return createdCompany;
        } else {
            actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * Updates the company.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The company.
     */
    Company update(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                   final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject companyJson = companyJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);

        if (companyManager.exists(orgId, companyJson.getLong(ActionAttributes.ID))) {
            final Company company = companyConverter.convert(new ConverterConfig(orgId, userId, null, true, companyJson), companyJson);

            insertSourceEntity(nodeExecutionContext, companyManager.get(orgId, companyJson.getLong(ActionAttributes.ID)), actionConfiguration);

            if (companyValidator.validate(company, nodeExecutionContext)) {
                final WorkflowV2 workflow = nodeExecutionContext.workflow();

                loadFieldAuditInfo(orgId, workflow.id(), workflow.getName(), company);
                final Company updatedCompany = companyManager.update(company);

                nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), updatedCompany));
                index(orgId, updatedCompany, Action.UPDATE);

                return updatedCompany;
            }
        }
        actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

        return null;
    }

    /**
     * Validates the {@link Company} entity.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class CompanyValidator implements Validator<Company> {

        private CompanyValidator() {
        }

        /**
         * {@inheritDoc}
         *
         * @param company              Refers the {@link Company} to be evaluated.
         * @param nodeExecutionContext Refers the {@code NodeExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final Company company, final NodeExecutionContext nodeExecutionContext) {
            final CrmUser salesOwner = company.getSalesOwner();
            final Map<Long, User> groupedUsers = nodeExecutionContext.groupedUsers();

            return Objects.nonNull(company.getName()) && isActiveUser(groupedUsers.get(salesOwner.getIamId()));
        }
    }
}
