package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;

import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.formatter.ContactJsonFormatter;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.iam.common.models.User;

import java.util.Map;
import java.util.Objects;

/**
 * The handler which handles the {@link Contact} related actions of a {@link com.tlc.crm.workflow.api.model.Workflow}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ContactActionHandler extends AbstractActionHandler implements SourceActionHandler {

    private final ContactJsonFormatter contactJsonFormatter;
    private final ContactConverter contactConverter;
    private final ContactManager contactManager;
    private final ContactValidator contactValidator;

    private ContactActionHandler() {
        this.contactJsonFormatter = ContactJsonFormatter.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.contactValidator = new ContactValidator();
    }

    private static class Instance {
        private static final ContactActionHandler INSTANCE = new ContactActionHandler();
    }

    /**
     * Gets the instance of {@link ContactActionHandler}.
     *
     * @return {@link ContactActionHandler} instance.
     */
    public static ContactActionHandler getInstance() {
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
     * @return The created {@link Contact}.
     */
    @Override
    public Contact create(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject contactJson = contactJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());
        final Contact contact = contactConverter.convert(new ConverterConfig(orgId, userId, null, false, contactJson), contactJson);

        if (contactValidator.validate(contact, actionExecutionContext)) {
            final Contact createdContact = contactManager.create(contact);

            index(orgId, createdContact, Action.CREATE);
            return createdContact;
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
     * @return The updated {@link Contact}.
     */
    @Override
    public Contact update(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject contactJson = contactJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());

        if (contactManager.exists(orgId, contactJson.getLong(ActionAttributes.ID))) {
            final Contact contact = contactConverter.convert(new ConverterConfig(orgId, userId, null, true, contactJson), contactJson);

            if (contactValidator.validate(contact, actionExecutionContext)) {
                loadFieldAuditInfo(orgId, workflowActionId, contact);
                final Contact updatedContact = contactManager.update(contact);

                index(orgId, updatedContact, Action.UPDATE);
                return updatedContact;
            }

        }
        actionExecutionContext.tracker().track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

        return null;
    }

    /**
     * Validates the {@link Contact} entity.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class ContactValidator implements Validator<Contact> {

        private final LifeCycleStatusManager lifeCycleStatusManager;

        private ContactValidator() {
            lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param contact                Refers the {@link Contact}.
         * @param actionExecutionContext Refers the {@code ActionExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final Contact contact, final ActionExecutionContext actionExecutionContext) {
            final CrmUser salesOwner = contact.getSalesOwner();
            final Map<Long, User> groupedUsers = actionExecutionContext.groupedUsers();

            return Objects.nonNull(contact.getFirstName()) && validateLifeCycle(contact) && (contact.hasEmails() || contact.hasPhones())
                    && isActiveUser(groupedUsers.get(salesOwner.getIamId()));
        }

        /**
         * <p>
         * Validates lifecycle stage and lifecycle status of the provided {@link Contact}
         * </p>
         *
         * @param contact Represents the {@link Contact} to be validated
         * @return true if it is valid, false otherwise
         */
        private boolean validateLifeCycle(final Contact contact) {
            final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
            final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();

            if (Objects.nonNull(lifeCycleStage) && Objects.nonNull(lifeCycleStatus)) {
                return lifeCycleStatusManager.exists(contact.orgId(), lifeCycleStage.id(), lifeCycleStatus.id());
            }

            return false;
        }
    }
}
