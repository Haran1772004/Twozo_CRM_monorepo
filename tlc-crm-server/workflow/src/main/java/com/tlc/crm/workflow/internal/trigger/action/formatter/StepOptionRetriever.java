package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.validator.TlcModel;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.cache.service.StepOptionService;
import com.tlc.crm.workflow.internal.trigger.action.formatter.AbstractJsonFormatter.StepOptionInfo;
import com.tlc.crm.workflow.v2.internal.action.StepOptionInfoV2;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Handles the data retrieval process for {@link StepOption} related to an entity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class StepOptionRetriever {

    private final StepOptionService stepOptionService;
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final DateUtil dateUtil;

    private StepOptionRetriever() {
        this.stepOptionService = StepOptionService.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.dateUtil = DateUtil.getInstance();
    }

    private static class Instance {
        private static final StepOptionRetriever INSTANCE = new StepOptionRetriever();
    }

    /**
     * Gets the instance of {@link StepOptionRetriever}.
     *
     * @return {@link StepOptionRetriever} instance.
     */
    public static StepOptionRetriever getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the value of step option from the {@link TlcModel} provided.
     *
     * @param stepOptionInfo Refers the {@link StepOptionInfo} attributes.
     * @param actionSource   Refers the {@link TriggerActionSource} for which the step option are retrieved.
     * @return The step option value.
     */
    Object getValue(final StepOptionInfo stepOptionInfo, final TriggerActionSource actionSource) {
        final StepOption stepOption = stepOptionService.getStepOption(stepOptionInfo.stepOptionId());
        final Map<Long, User> groupedUsers = stepOptionInfo.groupedUsers();

        return switch (stepOptionInfo.source()) {
            case COMPANY -> fetchCompanyValue(stepOptionInfo.model(), stepOption, groupedUsers);
            case CONTACT -> fetchContactValue(stepOptionInfo.model(), stepOption, groupedUsers);
            case DEAL -> fetchDealValue(stepOptionInfo.model(), stepOption, groupedUsers);
            case ACTIVITY -> fetchActivityValue(stepOptionInfo.model(), stepOption, groupedUsers, actionSource);
            default -> null;
        };
    }

    /**
     * Gets the value of step option from the {@link TlcModel} provided.
     *
     * @param stepOptionInfo Refers the {@link StepOptionInfo} attributes.
     * @param actionSource   Refers the {@link TriggerActionSource} for which the step option are retrieved.
     * @return The step option value.
     */
    public Object getValue(final StepOptionInfoV2 stepOptionInfo, final TriggerActionSource actionSource) {
        final StepOption stepOption = stepOptionService.getStepOption(stepOptionInfo.stepOptionId());
        final Map<Long, User> groupedUsers = stepOptionInfo.groupedUsers();

        return switch (stepOptionInfo.source()) {
            case COMPANY -> fetchCompanyValue(stepOptionInfo.model(), stepOption, groupedUsers);
            case CONTACT -> fetchContactValue(stepOptionInfo.model(), stepOption, groupedUsers);
            case DEAL -> fetchDealValue(stepOptionInfo.model(), stepOption, groupedUsers);
            case ACTIVITY -> fetchActivityValue(stepOptionInfo.model(), stepOption, groupedUsers, actionSource);
            default -> null;
        };
    }

    /**
     * Fetches the {@link Contact} related {@link StepOption}.
     *
     * @param contact      Refers the {@link Contact} that holds the value of the step option.
     * @param stepOption   Refers the {@link StepOption}.
     * @param groupedUsers Refers the grouped users from which the user related data fetched.
     * @return The step option value.
     */
    private Object fetchContactValue(final Contact contact, final StepOption stepOption, final Map<Long, User> groupedUsers) {
        return switch (stepOption.aliasName()) {
            case "contactOwner" -> contact.getSalesOwner().id();
            case "contactOwnerEmail" -> {
                final User user = groupedUsers.get(contact.getSalesOwner().getIamId());

                yield user.getEmail();
            }
            case "contactCreatorEmail" -> {
                final User user = groupedUsers.get(contact.getCreatedBy().getIamId());

                yield user.getEmail();
            }
            case "contact" -> contact.id();
            case "contactCompany" -> {
                final Company company = contact.getCompany();

                yield Objects.isNull(company) ? null : company.id();
            }
            case "contactEmail" -> {
                final Email email = contact.getPrimaryEmail();

                yield Objects.isNull(email) ? null : email;
            }
            case "contactPhone" -> {
                final Phone phone = contact.getPrimaryPhone();

                yield Objects.isNull(phone) ? null : phone.getNumber();
            }
            default -> null;
        };
    }

    /**
     * Fetches the {@link Deal} related {@link StepOption}.
     *
     * @param deal         Refers the {@link Deal} that holds the value of the step option.
     * @param stepOption   Refers the {@link StepOption}.
     * @param groupedUsers Refers the grouped users from which the user related data fetched.
     * @return The step option value.
     */
    private Object fetchDealValue(final Deal deal, final StepOption stepOption, final Map<Long, User> groupedUsers) {
        final Function<Contact, Contact> contactFetcher = (contact) -> {

            if (Objects.nonNull(contact)) {
                return contactManager.partialGet(deal.orgId(), contact.id());
            }
            return contact;
        };

        return switch (stepOption.aliasName()) {
            case "dealContact" -> deal.getPrimaryContact().id();
            case "dealValue" -> deal.getDealValue().getValue();
            case "dealCompany" -> {
                final Company dealCompany = deal.getCompany();

                yield Objects.isNull(dealCompany) ? null : dealCompany.id();
            }
            case "dealOwner" -> deal.getSalesOwner().id();
            case "dealCreator" -> deal.getCreatedBy().id();
            case "dealStatus" -> deal.getState().id();
            case "dealExpectedCloseDate" -> dateUtil.getTimeStamp(deal.getExpectedCloseDate());
            case "dealContactPhone" -> {
                final Contact dealContact = contactFetcher.apply(deal.getPrimaryContact());
                final Phone phone = dealContact.getPrimaryPhone();

                yield Objects.isNull(phone) ? null : phone.getNumber();
            }
            case "dealContactEmail" -> {
                final Contact dealContact = contactFetcher.apply(deal.getPrimaryContact());

                yield getContactPrimaryEmail(deal.orgId(), dealContact);
            }
            case "deal" -> deal.id();
            case "dealOwnerEmail" -> {
                final User user = groupedUsers.get(deal.getSalesOwner().getIamId());

                yield user.getEmail();
            }
            case "dealCreatorEmail" -> {
                final User user = groupedUsers.get(deal.getCreatedBy().getIamId());

                yield user.getEmail();
            }
            case "dealPipeline" -> deal.getPipeline().id();
            default -> null;
        };
    }

    /**
     * Fetches the {@link Company} related {@link StepOption}.
     *
     * @param company      Refers the {@link Company} that holds the value of the step option.
     * @param stepOption   Refers the {@link StepOption}.
     * @param groupedUsers Refers the grouped users from which the user related data fetched.
     * @return The step option value.
     */
    private Object fetchCompanyValue(final Company company, final StepOption stepOption, final Map<Long, User> groupedUsers) {
        return switch (stepOption.aliasName()) {
            case "companyOwner" -> company.getSalesOwner().id();
            case "companyOwnerEmail" -> {
                final User user = groupedUsers.get(company.getSalesOwner().getIamId());

                yield user.getEmail();
            }
            case "company" -> company.id();
            default -> null;
        };
    }

    /**
     * Fetches the {@link ActivityHolder} related {@link StepOption}.
     *
     * @param activityHolder Refers the {@link ActivityHolder} that holds the value of the step option.
     * @param stepOption     Refers the {@link StepOption}.
     * @param groupedUsers   Refers the grouped users from which the user related data fetched.
     * @param actionSource   Refers the {@link TriggerActionSource} for which the step option are retrieved.
     * @return The step option value.
     */
    private Object fetchActivityValue(final ActivityHolder activityHolder, final StepOption stepOption,
                                      final Map<Long, User> groupedUsers, final TriggerActionSource actionSource) {
        final Function<Deal, Deal> dealFetcher = (deal) -> {
            if (Objects.nonNull(deal)) {
                final Deal activityDeal = dealManager.get(activityHolder.orgId(), deal.id());

                activityHolder.setDeal(activityDeal);
                return activityDeal;
            }
            return deal;
        };
        final Function<Contact, Contact> contactFetcher = (contact) -> {
            if (Objects.isNull(contact.getFirstName())) {
                final Contact activityContact = contactManager.partialGet(contact.orgId(), contact.id());

                activityHolder.addContact(activityContact);
                return activityContact;
            }
            return contact;
        };

        return switch (stepOption.aliasName()) {
            case "activityType" -> {
                final Activity activity = activityHolder.getActivity();

                yield activity.getType().id();
            }
            case "dueDate" -> activityHolder.getActivity().getDuration();
            case "activityStatus" -> {
                final Activity activity = activityHolder.getActivity();

                yield activity.getStatus().getId();
            }
            case "activityAssignee" -> activityHolder.getActivity().getAssignee().id();
            case "activityCreator" -> activityHolder.getActivity().getCreator().id();
            case "activityDeal" -> {
                final Deal activityDeal = activityHolder.getDeal();

                yield Objects.isNull(activityDeal) ? null : activityDeal.id();
            }
            case "activityDealOwner" -> {
                final Deal activityDeal = dealFetcher.apply(activityHolder.getDeal());

                if (Objects.nonNull(activityDeal)) {
                    yield activityDeal.getSalesOwner().id();
                }
                yield null;
            }
            case "activityContact" -> {
                final List<Contact> activityContacts = activityHolder.getContacts();

                if (!activityContacts.isEmpty()) {
                    yield Objects.equals(WorkflowAttributes.ACTIVITY, actionSource.getAliasName()) ? activityContacts.stream().map(Contact::id).toList() : activityContacts.get(0).id();
                }
                yield null;
            }
            case "activityCompany" -> {
                final Company activityCompany = activityHolder.getCompany();

                yield Objects.isNull(activityCompany) ? null : activityCompany.id();
            }
            case "activityDealValue" -> {
                final Deal activityDeal = dealFetcher.apply(activityHolder.getDeal());

                if (Objects.nonNull(activityDeal)) {
                    final DealValue dealValue = activityDeal.getDealValue();

                    yield dealValue.getValue();
                }
                yield null;
            }
            case "activityAssigneeEmail" -> {
                final User user = groupedUsers.get(activityHolder.getActivity().getAssignee().getIamId());

                yield user.getEmail();
            }
            case "activityCreatorEmail" -> {
                final User user = groupedUsers.get(activityHolder.getActivity().getCreator().getIamId());

                yield user.getEmail();
            }
            case "activityContactEmail" -> activityHolder.getContacts().stream().findFirst().map(contact -> {
                final Contact activityContact = contactFetcher.apply(contact);

                return getContactPrimaryEmail(activityHolder.orgId(), activityContact);
            }).orElseGet(() -> null);
            case "activityDealStatus" -> {
                final Deal activityDeal = dealFetcher.apply(activityHolder.getDeal());

                if (Objects.nonNull(activityDeal)) {
                    yield activityDeal.getState().id();
                }
                yield null;
            }
            case "availability" -> {
                final Availability availability = activityHolder.getActivity().getAvailability();

                yield Objects.isNull(availability) ? null : availability.getId();
            }
            case "priority" -> {
                final Priority priority = activityHolder.getActivity().getPriority();

                yield Objects.isNull(priority) ? null : priority.getId();
            }
            case "reminder" -> {
                final Reminder reminder = activityHolder.getActivity().getReminder();

                yield Objects.isNull(reminder) ? null : reminder.getId();
            }
            case "activity" -> activityHolder.getActivity().id();
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the primary email of the provided contact
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param contact Represents the {@link Contact}
     * @return the primary email of the contact
     */
    private Email getContactPrimaryEmail(final long orgId, final Contact contact) {
        final Email email = contact.getPrimaryEmail();

        if (Objects.isNull(email)) {
            final Optional<Email> optionalEmail = contactManager.getPrimaryEmail(orgId, contact.id());

            return optionalEmail.orElseGet(() -> null);
        }

        return email;
    }
}
