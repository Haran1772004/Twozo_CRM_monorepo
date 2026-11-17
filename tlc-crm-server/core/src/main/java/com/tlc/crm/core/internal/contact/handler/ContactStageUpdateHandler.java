package com.tlc.crm.core.internal.contact.handler;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleRuleDetails;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.handler.LifeCycleTimelineUpdateHandler;
import com.tlc.crm.core.field.contact.ContactFields;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles the lifecycle stage/status update event of the contact based on the deal actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ContactStageUpdateHandler {

    private final DealManager dealManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final OptionService optionService;
    private final FieldService fieldService;
    private final LifeCycleTimelineUpdateHandler lifeCycleTimelineUpdateHandler;

    private ContactStageUpdateHandler() {
        dealManager = DealManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        optionService = OptionService.getInstance();
        fieldService = FieldService.getSecureInstance();
        lifeCycleTimelineUpdateHandler = LifeCycleTimelineUpdateHandler.getInstance();
    }

    private static class Instance {
        private static final ContactStageUpdateHandler INSTANCE = new ContactStageUpdateHandler();
    }

    /**
     * <p>
     * Provides the {@link ContactStageUpdateHandler}.
     * </p>
     *
     * @return {@link ContactStageUpdateHandler}
     */
    public static ContactStageUpdateHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles the lifecycle stage/status update event of the contact based on the deal actions.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param action Represents the action of the request.
     * @param deals  Holds the details of the deals.
     */
    public void handle(final Long orgId, final Action action, final Collection<Deal> deals) {
        final Map<LifeCycleRuleDetails.LifeCycleRuleType, LifeCycleRuleDetails> lifeCycleRules = lifeCycleStageManager.getRules(orgId).stream()
                .collect(Collectors.toMap(LifeCycleRuleDetails::getType, Function.identity()));

        switch (action) {
            case CREATE -> {
                final Collection<Contact> contacts = new ArrayList<>();
                final Map<LifeCycleRuleDetails.LifeCycleRuleType, Collection<Contact>> lifeCycleRuleContactsMap =
                        new EnumMap<>(LifeCycleRuleDetails.LifeCycleRuleType.class);

                for (final Deal deal : deals) {
                    final LifeCycleRuleDetails lifeCycleRuleDetails = lifeCycleRules.get(Deal.State.WON == deal.getState()
                            ? LifeCycleRuleDetails.LifeCycleRuleType.DEAL_WON : LifeCycleRuleDetails.LifeCycleRuleType.DEAL_CREATION);

                    if (Objects.nonNull(lifeCycleRuleDetails)) {
                        loadContactsToBeUpdated(lifeCycleRuleDetails, deal, contacts);
                        lifeCycleRuleContactsMap.computeIfAbsent(lifeCycleRuleDetails.getType(),
                                contactList -> new ArrayList<>()).addAll(contacts);
                    }
                }

                if (!contacts.isEmpty()) {
                    final Collection<Contact> dealWonContacts = lifeCycleRuleContactsMap
                            .get(LifeCycleRuleDetails.LifeCycleRuleType.DEAL_WON);
                    final Collection<Contact> dealCreationContacts = lifeCycleRuleContactsMap
                            .get(LifeCycleRuleDetails.LifeCycleRuleType.DEAL_CREATION);

                    if (Objects.nonNull(dealWonContacts) && !dealWonContacts.isEmpty()) {
                        lifeCycleTimelineUpdateHandler.updateLifeCycleOnDealWon(orgId, dealWonContacts);
                    }

                    if (Objects.nonNull(dealCreationContacts) && !dealCreationContacts.isEmpty()) {
                        lifeCycleTimelineUpdateHandler.updateLifeCycleOnDealCreation(orgId, dealCreationContacts);
                    }
                }
            }
            case UPDATE, PARTIAL_UPDATE -> {
                final Collection<Contact> contacts = new ArrayList<>();
                final Collection<Long> dealIds = deals.stream().map(Deal::id).collect(Collectors.toSet());
                Map<Long, Collection<Long>> dealContactIds = null;

                for (final Deal deal : deals) {
                    if (Deal.State.WON == deal.getState()) {
                        final LifeCycleRuleDetails lifeCycleRuleDetails = lifeCycleRules.get(LifeCycleRuleDetails.LifeCycleRuleType.DEAL_WON);

                        if (Objects.nonNull(lifeCycleRuleDetails)) {
                            if (Objects.isNull(dealContactIds)) {
                                dealContactIds = dealManager.getDealContactIds(orgId, dealIds);
                            }
                            final Collection<Long> contactIds = dealContactIds.get(deal.id());

                            if (Objects.nonNull(contactIds)) {
                                final CrmUser updatedBy = deal.getUpdatedBy();

                                for (final Long contactId : contactIds) {
                                    final Contact contact = new Contact();

                                    contact.setId(contactId);
                                    loadLifeCycleDetailsIntoContact(lifeCycleRuleDetails, contact, updatedBy);
                                    contacts.add(contact);
                                }
                            }
                        }
                    }
                }

                if (!contacts.isEmpty()) {
                    lifeCycleTimelineUpdateHandler.updateLifeCycleOnDealWon(orgId, contacts);
                }
            }
        }
    }

    /**
     * <p>
     * Loads the contacts which need to update their lifecycle stage.
     * </p>
     *
     * @param lifeCycleRuleDetails Holds the details of the lifecycle rule.
     * @param deal          Holds the details of the deal.
     * @param contacts      Holds the contact details.
     */
    private void loadContactsToBeUpdated(final LifeCycleRuleDetails lifeCycleRuleDetails, final Deal deal, final Collection<Contact> contacts) {
        final Contact primaryContact = deal.getPrimaryContact();
        final CrmUser updatedBy = deal.getUpdatedBy();
        final Collection<Participant> participants = deal.getParticipants();

        if (Objects.nonNull(participants)) {
            for (final Participant participant : participants) {
                final Contact contact = participant.getContact();

                loadLifeCycleDetailsIntoContact(lifeCycleRuleDetails, contact, updatedBy);
                contacts.add(contact);
            }
        }
        loadLifeCycleDetailsIntoContact(lifeCycleRuleDetails, primaryContact, updatedBy);
        contacts.add(primaryContact);
    }

    /**
     * <p>
     * Loads the lifecycle details into {@link Contact}.
     * </p>
     *
     * @param lifeCycleRuleDetails Holds the rule of the lifecycle stage.
     * @param contact       Holds the details of the contact.
     * @param updatedBy     Holds the updated user details.
     */
    private void loadLifeCycleDetailsIntoContact(final LifeCycleRuleDetails lifeCycleRuleDetails, final Contact contact, final CrmUser updatedBy) {
        final Long orgId = lifeCycleRuleDetails.orgId();
        final LifeCycleStatus status = lifeCycleRuleDetails.getStatus();

        contact.setOrgId(orgId);
        contact.setUpdatedBy(updatedBy);
        contact.setLifeCycleStage(lifeCycleRuleDetails.getStage());
        contact.setLifeCycleStatus(status);

        if (LifeCycleStatus.Label.CLOSED_LOST == status.getLabel()) {
            final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
            final FieldInfo lostReasonField = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, lostReasonKey).get(lostReasonKey);

            contact.setLostReason(optionService.getDefaultOption(orgId, lostReasonField));
        }
    }
}