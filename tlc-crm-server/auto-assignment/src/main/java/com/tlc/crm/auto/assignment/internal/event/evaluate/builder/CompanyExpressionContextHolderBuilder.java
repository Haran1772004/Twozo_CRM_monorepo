package com.tlc.crm.auto.assignment.internal.event.evaluate.builder;

import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The builder that constructs the expression for a company with the provided criteria information.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class CompanyExpressionContextHolderBuilder extends AbstractExpressionContextHolderBuilder<Company> {

    private CompanyExpressionContextHolderBuilder() {
        super(Source.COMPANY);
    }

    private static final class Instance {
        private static final CompanyExpressionContextHolderBuilder INSTANCE = new CompanyExpressionContextHolderBuilder();
    }

    /**
     * Gets the instance of {@link CompanyExpressionContextHolderBuilder}.
     *
     * @return {@link CompanyExpressionContextHolderBuilder} instance.
     */
    static CompanyExpressionContextHolderBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param company Refers the model from which the value is fetched.
     * @param fieldId Refers the field id.
     * @return The field value.
     */
    @Override
    protected Object getValue(final Company company, final Long fieldId) {
        final FieldInfo fieldInfo = fieldService.getField(company.orgId(), fieldId);

        if (fieldInfo.isPersistent()) {
            final Object value = company.getValueHolder().get(fieldInfo.id());

            return getPersistentFieldValue(value, fieldInfo);
        } else {
            return switch (fieldInfo.aliasName()) {
                case "name" -> TEXT_NORMALIZER.apply(company.getName());
                case "website" -> TEXT_NORMALIZER.apply(company.getWebsite());
                case "address" -> {
                    final Address address = company.getAddress();

                    if (Objects.nonNull(address)) {
                        yield constructAddressString(address);
                    }
                    yield EMPTY_STRING;
                }
                case "salesOwner" -> company.getSalesOwner().id();
                case "facebook" -> TEXT_NORMALIZER.apply(company.getFacebook());
                case "twitter" -> TEXT_NORMALIZER.apply(company.getTwitter());
                case "linkedIn" -> TEXT_NORMALIZER.apply(company.getLinkedIn());
                case "annualRevenue" -> {
                    final Monetary annualRevenue = company.getAnnualRevenue();

                    yield Objects.nonNull(annualRevenue) ? annualRevenue.getValue() : null;
                }
                case "sicCode" -> company.getSicCode();
                case "territory" -> {
                    final Territory territory = company.getTerritory();

                    yield Objects.nonNull(territory) ? territory.id() : null;
                }
                case "tags" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final Collection<Tag> tags = companySummary.getTags();

                        yield Objects.nonNull(tags) && !tags.isEmpty()
                                ? tags.stream().map(Tag::id).collect(Collectors.toUnmodifiableSet())
                                : Collections.emptySet();
                    }
                    yield Collections.emptySet();
                }
                case "employeeCount" -> company.getEmployeeCount();
                case "parentCompany" -> {
                    final Company parentCompany = company.getParentCompany();

                    yield Objects.nonNull(parentCompany) ? parentCompany.id() : null;
                }
                case "organizationStatus" -> {
                    final Option organizationStatus = company.getOrganizationStatus();

                    yield Objects.nonNull(organizationStatus) ? organizationStatus.id() : null;
                }
                case "industryType" -> {
                    final Option industryType = company.getIndustryType();

                    yield Objects.nonNull(industryType) ? industryType.id() : null;
                }
                case "businessType" -> {
                    final Option businessType = company.getBusinessType();

                    yield Objects.nonNull(businessType) ? businessType.id() : null;
                }
                case "description" -> TEXT_NORMALIZER.apply(company.getDescription());
                case "createdAt" -> dateTimeUtil.getTimeStamp(company.getCreatedTime());
                case "updatedAt" -> dateTimeUtil.getTimeStamp(company.getModifiedTime());
                case "lastAssignedAt" -> dateTimeUtil.getTimeStamp(company.getLastAssignedAt());
                case "createdBy" -> company.getCreatedBy().id();
                case "updatedBy" -> company.getUpdatedBy().id();
                case "lastEmailReceived" -> {
                    final SummaryInfo companySummaryInfo = company.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final EmailSummary emailSummary = companySummaryInfo.getEmailSummary();

                        yield Objects.isNull(emailSummary) ? null : dateUtil.getTimeStamp(emailSummary.getLastEmailReceived());
                    }
                    yield null;
                }
                case "emailMessageCount" -> {
                    final SummaryInfo companySummaryInfo = company.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final EmailSummary emailSummary = companySummaryInfo.getEmailSummary();

                        yield Objects.isNull(emailSummary) ? null : emailSummary.getEmailMessageCount();
                    }
                    yield null;
                }
                case "lastEmailSent" -> {
                    final SummaryInfo companySummaryInfo = company.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final EmailSummary emailSummary = companySummaryInfo.getEmailSummary();

                        yield Objects.isNull(emailSummary) ? null : dateUtil.getTimeStamp(emailSummary.getLastEmailSent());
                    }
                    yield null;
                }
                case "deals" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : dealStatistics.getOpenDealsCount();
                    }
                    yield null;
                }
                case "closedDeals" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : nullifyZero(dealStatistics.getClosedDealsCount());
                    }
                    yield null;
                }
                case "wonDeals" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : nullifyZero(dealStatistics.getWonDealsCount());
                    }
                    yield null;
                }
                case "lostDeals" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                        yield Objects.isNull(dealStatistics) ? null : nullifyZero(dealStatistics.getLostDealsCount());
                    }
                    yield null;
                }
                case "lastActivityDate" -> {
                    final SummaryInfo companySummaryInfo = company.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final ActivitySummary activitySummary = companySummaryInfo.getActivitySummary();

                        if (Objects.nonNull(activitySummary)) {
                            yield dateUtil.getTimeStamp(activitySummary.getLastActivityDate());
                        }
                    }
                    yield null;
                }
                case "nextActivityDate" -> {
                    final SummaryInfo companySummaryInfo = company.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final ActivitySummary activitySummary = companySummaryInfo.getActivitySummary();

                        if (Objects.nonNull(activitySummary)) {
                            yield dateUtil.getTimeStamp(activitySummary.getNextActivityDate());
                        }
                    }
                    yield null;
                }
                case "lastActivityType" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                        if (Objects.nonNull(activitySummary)) {
                            yield getLastActivityTypeId(company.orgId(), activitySummary);
                        }
                    }
                    yield null;
                }
                case "doneActivities" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getDoneActivities());
                    }
                    yield null;
                }
                case "upcomingActivities" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getUpcomingActivities());
                    }
                    yield null;
                }
                case "totalActivities" -> {
                    final SummaryInfo companySummary = company.getSummaryInfo();

                    if (Objects.nonNull(companySummary)) {
                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getTotalActivities());
                    }
                    yield null;
                }
                case "recentNote" -> TEXT_NORMALIZER.apply(company.getRecentNote());
                default -> null;
            };
        }
    }

}
