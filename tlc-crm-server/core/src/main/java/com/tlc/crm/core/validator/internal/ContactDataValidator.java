package com.tlc.crm.core.validator.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;

import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.validator.EntityDataValidator;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Validates the {@link Contact}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ContactDataValidator implements EntityDataValidator<Contact> {

    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final CrmUserManager crmUserManager;
    private final OptionService optionService;
    private final TerritoryManager territoryManager;

    private ContactDataValidator() {
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        optionService = OptionService.getInstance();
        territoryManager = TerritoryManager.getInstance();
    }

    /**
     * Holds the instance of {@link ContactDataValidator}
     */
    private static final class InstanceHolder {
        private static final ContactDataValidator CONTACT_CONTEXT_VALIDATOR = new ContactDataValidator();
    }

    /**
     * <p>
     * Gets the instance of {@link ContactDataValidator}
     * </p>
     *
     * @return the instance of {@link ContactDataValidator}
     */
    public static ContactDataValidator getInstance() {
        return InstanceHolder.CONTACT_CONTEXT_VALIDATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the org id
     * @param contacts Represents the contacts to be validated
     */
    @Override
    public void validate(final Long orgId, final Collection<Contact> contacts) {
        final Set<Long> lifeCycleStageIds = new HashSet<>();
        final Set<Long> lifeCycleStatusIds = new HashSet<>();
        final Collection<Long> lostReasonIds = new HashSet<>();
        final Collection<Long> contactSourceIds = new HashSet<>();
        final Collection<Long> salesOwnerIds = new HashSet<>();
        final Collection<Long> territoryIds = new HashSet<>();

        for (final Contact contact : contacts) {
            final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
            final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
            final Option lostReason = contact.getLostReason();
            final Option contactSource = contact.getContactSource();
            final Collection<Email> emails = contact.getEmails();
            final Collection<Phone> phones = contact.getPhones();
            final Territory territory = contact.getTerritory();

            if (Objects.nonNull(emails) && !emails.isEmpty()) {
                final Collection<String> emailAddress = new HashSet<>();

                for (final Email email : emails) {

                    if (!emailAddress.add(email.getValue())) {
                        throw ErrorCode.get(ContactErrorCodes.EMAIL_ADDRESS_ALREADY_EXISTS, "i18n.contact.email.already.exists");
                    }
                }
            }

            if (Objects.nonNull(phones) && !phones.isEmpty()) {
                final Collection<String> phoneNumbers = new HashSet<>();

                for (final Phone phone : phones) {

                    if (!phoneNumbers.add(phone.getNumber())) {
                        throw ErrorCode.get(ContactErrorCodes.PHONE_ALREADY_EXISTS, "i18n.contact.phone.already.exists");
                    }
                }
            }

            if (Objects.nonNull(lifeCycleStage)) {
                final Long lifecycleStageId = lifeCycleStage.id();

                if (Objects.nonNull(lifecycleStageId)) {
                    lifeCycleStageIds.add(lifecycleStageId);
                }
            }

            if (Objects.nonNull(lifeCycleStatus)) {
                final Long lifecycleStatusId = lifeCycleStatus.id();

                if (Objects.nonNull(lifecycleStatusId)) {
                    lifeCycleStatusIds.add(lifecycleStatusId);
                }
            }

            if (Objects.nonNull(lostReason)) {
                final Long lostReasonId = lostReason.id();

                if (Objects.nonNull(lostReasonId)) {
                    lostReasonIds.add(lostReasonId);
                }
            }

            if (Objects.nonNull(contactSource)) {
                final Long contactSourceId = contactSource.id();

                if (Objects.nonNull(contactSourceId)) {
                    contactSourceIds.add(contactSourceId);
                }
            }

            if (Objects.nonNull(territory)) {
                territoryIds.add(territory.id());
            }
            salesOwnerIds.add(contact.getSalesOwner().id());

            if (!lostReasonIds.isEmpty() && !optionService.exists(orgId, lostReason.getFieldId(), lostReasonIds, false)) {
                throw ErrorCode.get(ContactErrorCodes.LOST_REASON_NOT_FOUND, "i18n.contact.lostReason.not.found");
            }

            if (!contactSourceIds.isEmpty() && !optionService.exists(orgId, contactSource.getFieldId(), contactSourceIds, false)) {
                throw ErrorCode.get(ContactErrorCodes.CONTACT_SOURCE_NOT_FOUND, "i18n.contact.source.not.found");
            }
        }

        if (!lifeCycleStageIds.isEmpty() && !lifeCycleStageManager.exists(orgId, lifeCycleStageIds)) {
            throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STAGE_NOT_FOUND, "i18n.lifecycle.stage.not.found");
        }

        if (!lifeCycleStatusIds.isEmpty() && !lifeCycleStatusManager.exists(orgId, lifeCycleStatusIds)) {
            throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STATUS_NOT_FOUND, "i18n.lifecycle.status.not.found");
        }

        if (!crmUserManager.exists(orgId, salesOwnerIds)) {
            throw ErrorCode.get(ContactErrorCodes.SALES_OWNER_NOT_FOUND, "i18n.contact.salesOwner.not.found");
        }

        if (!territoryIds.isEmpty() && !territoryManager.exists(orgId, territoryIds)) {
            throw ErrorCode.get(ContactErrorCodes.TERRITORY_NOT_FOUND, "i18n.contact.territory.not.found");
        }
    }
}
