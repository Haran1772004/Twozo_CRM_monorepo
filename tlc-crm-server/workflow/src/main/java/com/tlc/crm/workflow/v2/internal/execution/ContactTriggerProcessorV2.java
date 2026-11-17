package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Contact trigger processor for the workflow V2.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class ContactTriggerProcessorV2 extends AbstractTriggerProcessorV2<Contact> {

    private ContactTriggerProcessorV2() {
    }

    private static class Instance {
        private static final ContactTriggerProcessorV2 INSTANCE = new ContactTriggerProcessorV2();
    }

    /**
     * Gets the {@link ContactTriggerProcessorV2} instance.
     *
     * @return The instance of {@link ContactTriggerProcessorV2}.
     */
    public static ContactTriggerProcessorV2 getInstance() {
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
            loadContactUserIds(triggerExecutionData.models(), userIds);
            triggerService.collectUserIds(triggerExecutionData.orgId(), userIds, workflows);

            invoke(userId, triggerExecutionData, workflows, userIds);
        }
    }

    /**
     * Loads all the user ids of contact to fetch the user details.
     *
     * @param contactPairs Represents the contact pairs.
     * @param userIds      Represents the {@link Set} of user ids.
     */
    private void loadContactUserIds(final Collection<Pair<TlcModel, TlcModel>> contactPairs, final Set<Long> userIds) {
        final Consumer<Contact> userIdCollector = (contact) -> {
            userIds.add(contact.getSalesOwner().id());
            userIds.add(contact.getCreatedBy().id());
            userIds.add(contact.getUpdatedBy().id());
        };

        for (final Pair<TlcModel, TlcModel> contactPair : contactPairs) {
            final Contact oldContact = (Contact) contactPair.getValue();

            userIdCollector.accept((Contact) contactPair.getKey());

            if (Objects.nonNull(oldContact)) {
                userIdCollector.accept(oldContact);
            }
        }
    }
}
