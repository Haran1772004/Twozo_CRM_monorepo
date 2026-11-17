package com.tlc.crm.workflow.internal.trigger.evaluate.builder;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.validator.expression.model.ExpressionContextHolder;

import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The builder that constructs the expression for {@link Contact}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ContactExpressionContextHolderBuilder extends AbstractExpressionContextHolderBuilder<Contact> {

    private final DateTimeUtil dateTimeUtil;
    private final DateUtil dateUtil;

    private ContactExpressionContextHolderBuilder() {
        super(TriggerSource.CONTACT);
        dateTimeUtil = DateTimeUtil.getInstance();
        dateUtil = DateUtil.getInstance();
    }

    private static final class Instance {
        private static final ContactExpressionContextHolderBuilder INSTANCE = new ContactExpressionContextHolderBuilder();
    }

    /**
     * Gets the instance of {@link ContactExpressionContextHolderBuilder}.
     *
     * @return {@link ContactExpressionContextHolderBuilder} instance.
     */
    public static ContactExpressionContextHolderBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param contactPair  Refers to the {@link Pair} consisting of an old and current model of the same entity.
     * @param criteriaSets Refers the {@link WorkflowCriteriaSet} to be checked.
     * @return The {@link ExpressionContextHolder}.
     */
    @Override
    public ExpressionContextHolder buildExpressionContextHolder(final Pair<Contact, Contact> contactPair,
                                                                final Collection<WorkflowCriteriaSet> criteriaSets) {
        return build(contactPair, criteriaSets);
    }

    /**
     * {@inheritDoc}
     *
     * @param contact Refers the model from which the value is fetched.
     * @param fieldId Refers the field id.
     * @return The field value.
     */
    @Override
    protected Object getValue(final Contact contact, final Long fieldId) {
        final FieldInfo fieldInfo = fieldService.getField(contact.orgId(), fieldId);

        if (fieldInfo.isPersistent()) {
            final Object value = contact.getValueHolder().get(fieldInfo.id());

            return getPersistentFieldValue(value, fieldInfo);
        } else {
            return switch (fieldInfo.aliasName()) {
                case "firstName" -> TEXT_NORMALIZER.apply(contact.getFirstName());
                case "lastName" -> TEXT_NORMALIZER.apply(contact.getLastName());
                case "emails" -> {
                    final Collection<Email> emails = contact.getEmails();

                    yield Objects.nonNull(emails) && !emails.isEmpty()
                            ? emails.stream().map(Email::getValue).collect(Collectors.toUnmodifiableSet()) : List.of();
                }
                case "phones" -> {
                    final Collection<Phone> phones = contact.getPhones();

                    yield Objects.nonNull(phones) && !phones.isEmpty()
                            ? phones.stream().map(Phone::getNumber).collect(Collectors.toUnmodifiableSet()) : List.of();
                }
                case "company" -> {
                    final Company company = contact.getCompany();

                    yield Objects.nonNull(company) ? company.id() : null;
                }
                case "designation" -> TEXT_NORMALIZER.apply(contact.getDesignation());
                case "salesOwner" -> contact.getSalesOwner().id();
                case "lifeCycleStage" -> contact.getLifeCycleStage().id();
                case "lifeCycleStatus" -> contact.getLifeCycleStatus().id();
                case "contactLostReason" -> Optional.ofNullable(contact.getLostReason()).map(Option::id).orElseGet(() -> null);
                case "contactSource" -> {
                    final Option contactSource = contact.getContactSource();

                    yield Objects.nonNull(contactSource) ? contactSource.id() : null;
                }
                case "territory" -> {
                    final Territory territory = contact.getTerritory();

                    yield Objects.nonNull(territory) ? territory.id() : null;
                }
                case "tags" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final Collection<Tag> tags = contactSummary.getTags();

                        yield Objects.nonNull(tags) && !tags.isEmpty()
                                ? tags.stream().map(Tag::id).collect(Collectors.toUnmodifiableSet())
                                : Collections.emptySet();
                    }
                    yield Collections.emptySet();
                }
                case "dateOfBirth" -> dateUtil.getTimeStamp(contact.getDateOfBirth());
                case "department" -> TEXT_NORMALIZER.apply(contact.getDepartment());
                case "address" -> {
                    final Address address = contact.getAddress();

                    if (Objects.nonNull(address)) {
                        yield constructAddressString(address);
                    }
                    yield EMPTY_STRING;
                }
                case "facebook" -> TEXT_NORMALIZER.apply(contact.getFacebook());
                case "twitter" -> TEXT_NORMALIZER.apply(contact.getTwitter());
                case "linkedIn" -> TEXT_NORMALIZER.apply(contact.getLinkedIn());
                case "subscriptionStatus" -> {
                    final Option subscriptionStatus = contact.getSubscriptionStatus();

                    yield Objects.nonNull(subscriptionStatus) ? subscriptionStatus.id() : null;
                }
                case "subscriptionTypes" -> {
                    final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();

                    yield Objects.nonNull(subscriptionTypes) ? subscriptionTypes.stream()
                            .map(Option::id).collect(Collectors.toUnmodifiableSet()) : Collections.emptyList();
                }
                case "unsubscribeReason" -> {
                    final Option unsubscribeReason = contact.getUnsubscribeReason();

                    yield Objects.nonNull(unsubscribeReason) ? unsubscribeReason.id() : null;
                }
                case "otherUnsubscribeReason" -> TEXT_NORMALIZER.apply(contact.getOtherUnsubscribeReason());
                case "createdBy" -> contact.getCreatedBy().id();
                case "updatedBy" -> contact.getUpdatedBy().id();
                case "updatedAt" -> dateTimeUtil.getTimeStamp(contact.getModifiedTime());
                case "lastAssignedAt" -> dateTimeUtil.getTimeStamp(contact.getLastAssignedAt());
                case "createdAt" -> dateTimeUtil.getTimeStamp(contact.getCreatedTime());
                case "timeZone" -> {
                    final Option timezoneInfo = contact.getTimeZone();

                    yield Objects.nonNull(timezoneInfo) ? timezoneInfo.id() : null;
                }
                case "webForms" -> TEXT_NORMALIZER.apply(contact.getSummaryInfo().getWebFormName());
                case "deals" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : dealStatistics.getOpenDealsCount();
                    }
                    yield null;
                }
                case "closedDeals" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : nullifyZero(dealStatistics.getClosedDealsCount());
                    }
                    yield null;
                }
                case "wonDeals" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : nullifyZero(dealStatistics.getWonDealsCount());
                    }
                    yield null;
                }
                case "lostDeals" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : nullifyZero(dealStatistics.getLostDealsCount());
                    }
                    yield null;
                }
                case "lastActivityDate" -> {
                    final SummaryInfo contactSummaryInfo = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummaryInfo)) {
                        final ActivitySummary activitySummary = contactSummaryInfo.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : dateUtil.getTimeStamp(activitySummary.getLastActivityDate());
                    }
                    yield null;
                }
                case "nextActivityDate" -> {
                    final SummaryInfo contactSummaryInfo = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummaryInfo)) {
                        final ActivitySummary activitySummary = contactSummaryInfo.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : dateUtil.getTimeStamp(activitySummary.getNextActivityDate());
                    }
                    yield null;
                }
                case "lastActivityType" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        if (Objects.nonNull(activitySummary)) {
                            yield getLastActivityTypeId(contact.orgId(), activitySummary);
                        }
                    }
                    yield null;
                }
                case "doneActivities" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getDoneActivities());
                    }
                    yield null;
                }
                case "upcomingActivities" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getUpcomingActivities());
                    }
                    yield null;
                }
                case "totalActivities" -> {
                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getTotalActivities());
                    }
                    yield null;
                }
                case "lastEmailReceived" -> {
                    final SummaryInfo contactSummaryInfo = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummaryInfo)) {
                        final EmailSummary emailSummary = contactSummaryInfo.getEmailSummary();

                        yield Objects.isNull(emailSummary) ? null : dateUtil.getTimeStamp(emailSummary.getLastEmailReceived());
                    }
                    yield null;
                }
                case "emailMessageCount" -> {
                    final SummaryInfo companySummaryInfo = contact.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final EmailSummary emailSummary = companySummaryInfo.getEmailSummary();

                        yield Objects.isNull(emailSummary) ? null : emailSummary.getEmailMessageCount();
                    }
                    yield null;
                }
                case "lastEmailSent" -> {
                    final SummaryInfo contactSummaryInfo = contact.getSummaryInfo();

                    if (Objects.nonNull(contactSummaryInfo)) {
                        final EmailSummary emailSummary = contactSummaryInfo.getEmailSummary();

                        yield Objects.isNull(emailSummary) ? null : dateUtil.getTimeStamp(emailSummary.getLastEmailSent());
                    }
                    yield null;
                }
                case "recentNote" -> TEXT_NORMALIZER.apply(contact.getRecentNote());
                case "marketingList" -> {
                    final Collection<MarketingList> marketingLists = contact.getMarketingLists();

                    yield Objects.nonNull(marketingLists) && !marketingLists.isEmpty()
                            ? marketingLists.stream().map(MarketingList::id).collect(Collectors.toUnmodifiableSet())
                            : Collections.emptySet();
                }
                default -> null;
            };
        }
    }
}
