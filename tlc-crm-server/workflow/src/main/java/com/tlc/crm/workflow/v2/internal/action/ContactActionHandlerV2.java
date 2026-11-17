package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.AbstractActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.internal.action.formatter.ContactJsonFormatterV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.iam.common.models.User;

import java.util.Map;
import java.util.Objects;

/**
 * Handles the contact actions for the workflow.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class ContactActionHandlerV2 extends AbstractActionHandler {

    private final ContactJsonFormatterV2 contactJsonFormatter;
    private final ContactConverter contactConverter;
    private final ContactManager contactManager;
    private final ContactValidator contactValidator;

    private ContactActionHandlerV2() {
        this.contactJsonFormatter = ContactJsonFormatterV2.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.contactValidator = new ContactValidator();
    }

    private static class Instance {
        private static final ContactActionHandlerV2 INSTANCE = new ContactActionHandlerV2();
    }

    /**
     * Gets the instance of {@link ContactActionHandlerV2}.
     *
     * @return {@link ContactActionHandlerV2} instance.
     */
    static ContactActionHandlerV2 getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates the contact.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The contact.
     */
    Contact create(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                   final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject contactJson = contactJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);
        final Contact contact = contactConverter.convert(new ConverterConfig(orgId, userId, null, false, contactJson), contactJson);

        if (contactValidator.validate(contact, nodeExecutionContext)) {
            final Contact createdContact = contactManager.create(contact);

            insertSourceEntity(nodeExecutionContext, createdContact, actionConfiguration);
            nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), createdContact));
            index(orgId, createdContact, Action.CREATE);

            return createdContact;
        } else {
            actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * Updates the contact.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The contact.
     */
    Contact update(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                   final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject contactJson = contactJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);

        if (contactManager.exists(orgId, contactJson.getLong(ActionAttributes.ID))) {
            final Contact contact = contactConverter.convert(new ConverterConfig(orgId, userId, null, true, contactJson), contactJson);

            insertSourceEntity(nodeExecutionContext, contactManager.get(orgId, contactJson.getLong(ActionAttributes.ID)), actionConfiguration);

            if (contactValidator.validate(contact, nodeExecutionContext)) {
                final WorkflowV2 workflow = nodeExecutionContext.workflow();

                loadFieldAuditInfo(orgId, workflow.id(), workflow.getName(), contact);
                final Contact updatedContact = contactManager.update(contact);

                nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), updatedContact));
                index(orgId, updatedContact, Action.UPDATE);

                return updatedContact;
            }

        }
        actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

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
         * @param contact              Refers the {@link Contact}.
         * @param nodeExecutionContext Refers the {@code NodeExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final Contact contact, final NodeExecutionContext nodeExecutionContext) {
            final CrmUser salesOwner = contact.getSalesOwner();
            final Map<Long, User> groupedUsers = nodeExecutionContext.groupedUsers();

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
