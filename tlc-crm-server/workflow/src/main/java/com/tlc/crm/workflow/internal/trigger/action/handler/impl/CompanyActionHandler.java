package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;

import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.formatter.CompanyJsonFormatter;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.iam.common.models.User;

import java.util.Map;
import java.util.Objects;

/**
 * The handler which handles the {@link Company} related actions of a {@link com.tlc.crm.workflow.api.model.Workflow}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CompanyActionHandler extends AbstractActionHandler implements SourceActionHandler {

    private final CompanyJsonFormatter companyJsonFormatter;
    private final CompanyConverter companyConverter;
    private final CompanyManager companyManager;
    private final CompanyValidator companyValidator;

    private CompanyActionHandler() {
        this.companyJsonFormatter = CompanyJsonFormatter.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.companyValidator = new CompanyValidator();
    }

    private static class Instance {
        private static final CompanyActionHandler INSTANCE = new CompanyActionHandler();
    }

    /**
     * Gets the instance of {@link CompanyActionHandler}.
     *
     * @return {@link CompanyActionHandler} instance.
     */
    public static CompanyActionHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The created {@link Company}.
     */
    @Override
    public Company create(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject companyJson = companyJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());
        final Company company = companyConverter.convert(new ConverterConfig(orgId, userId, null, false, companyJson), companyJson);

        if (companyValidator.validate(company, actionExecutionContext)) {
            final Company createdCompany = companyManager.create(company);

            index(orgId, createdCompany, Action.CREATE);
            return createdCompany;
        } else {
            actionExecutionContext.tracker().track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The updated {@link Company}.
     */
    @Override
    public Company update(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject companyJson = companyJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());

        if (companyManager.exists(orgId, companyJson.getLong(ActionAttributes.ID))) {
            final Company company = companyConverter.convert(new ConverterConfig(orgId, userId, null, true, companyJson), companyJson);

            if (companyValidator.validate(company, actionExecutionContext)) {
                loadFieldAuditInfo(orgId, workflowActionId, company);
                final Company updatedCompany = companyManager.update(company);

                index(orgId, updatedCompany, Action.UPDATE);
                return updatedCompany;
            }
        }
        actionExecutionContext.tracker().track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

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
         * @param company                Refers the {@link Company} to be evaluated.
         * @param actionExecutionContext Refers the {@code ActionExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final Company company, final ActionExecutionContext actionExecutionContext) {
            final CrmUser salesOwner = company.getSalesOwner();
            final Map<Long, User> groupedUsers = actionExecutionContext.groupedUsers();

            return Objects.nonNull(company.getName()) && isActiveUser(groupedUsers.get(salesOwner.getIamId()));
        }
    }
}
