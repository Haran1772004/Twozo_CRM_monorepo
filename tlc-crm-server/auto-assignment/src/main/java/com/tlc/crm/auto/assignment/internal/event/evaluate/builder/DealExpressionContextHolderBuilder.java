package com.tlc.crm.auto.assignment.internal.event.evaluate.builder;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The builder that constructs the expression for a deal with the provided criteria information.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class DealExpressionContextHolderBuilder extends AbstractExpressionContextHolderBuilder<Deal> {

    private final BaseCurrencyService baseCurrencyService;

    private DealExpressionContextHolderBuilder() {
        super(Source.DEAL);
        this.baseCurrencyService = BaseCurrencyService.getInstance();
    }

    private static final class Instance {
        private static final DealExpressionContextHolderBuilder INSTANCE = new DealExpressionContextHolderBuilder();
    }

    /**
     * Gets the instance of {@link DealExpressionContextHolderBuilder}.
     *
     * @return {@link DealExpressionContextHolderBuilder} instance.
     */
    static DealExpressionContextHolderBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal    Refers the model from which the value is fetched.
     * @param fieldId Refers the field id.
     * @return The field value.
     */
    @Override
    protected Object getValue(final Deal deal, final Long fieldId) {
        final FieldInfo fieldInfo = fieldService.getField(deal.orgId(), fieldId);

        if (fieldInfo.isPersistent()) {
            final Object value = deal.getValueHolder().get(fieldInfo.id());

            return getPersistentFieldValue(value, fieldInfo);
        } else {
            return switch (fieldInfo.aliasName()) {
                case "title" -> TEXT_NORMALIZER.apply(deal.getTitle());
                case "pipeline" -> deal.getPipeline().id();
                case "pipelineStage" -> deal.getPipelineStage().id();
                case "wonReason" -> {
                    final Option wonReason = deal.getWonReason();

                    yield Objects.nonNull(wonReason) ? wonReason.id() : null;
                }
                case "lostReason" -> {
                    final Option lostReason = deal.getLostReason();

                    yield Objects.nonNull(lostReason) ? lostReason.id() : null;
                }
                case "dealClosedOn", "closedTime", "wonTime", "lostTime" ->
                        dateTimeUtil.getTimeStamp(deal.getClosedTime());
                case "primaryContact" -> {
                    final Contact contact = deal.getPrimaryContact();

                    yield Objects.nonNull(contact) ? contact.id() : null;
                }
                case "relatedContacts" -> {
                    final Collection<Participant> participants = deal.getParticipants();

                    yield Objects.nonNull(participants) && !participants.isEmpty()
                            ? participants.stream().map(participant -> participant.getContact().id()).collect(Collectors.toUnmodifiableSet()) : List.of();
                }
                case "company" -> {
                    final Company company = deal.getCompany();

                    yield Objects.nonNull(company) ? company.id() : null;
                }
                case "salesOwner" -> deal.getSalesOwner().id();
                case "dealValue" -> deal.getDealValue().getValue();
                case "products" -> {
                    final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

                    if (Objects.nonNull(productDetailsHolder)) {
                        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

                        yield Objects.isNull(productDetails) ? null : productDetails.size();
                    }
                    yield null;
                }
                case "expectedCloseDate" -> dateUtil.getTimeStamp(deal.getExpectedCloseDate());
                case "dealSource" -> {
                    final Option dealSource = deal.getSource();

                    yield Objects.nonNull(dealSource) ? dealSource.id() : null;
                }
                case "tags" -> {
                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                    if (Objects.nonNull(dealSummary)) {
                        final Collection<Tag> tags = dealSummary.getTags();

                        yield Objects.nonNull(tags) && !tags.isEmpty()
                                ? tags.stream().map(Tag::id).collect(Collectors.toUnmodifiableSet())
                                : Collections.emptySet();
                    }
                    yield Collections.emptySet();
                }
                case "territory" -> {
                    final Territory territory = deal.getTerritory();

                    yield Objects.nonNull(territory) ? territory.id() : null;
                }
                case "rottingDays" -> deal.getRottenDays();
                case "age" -> deal.getAge();
                case "stageProbability" -> deal.getStageProbability();
                case "weightedValue" -> deal.getWeightedRevenue();
                case "expectedDealValue" -> {
                    final Monetary expectedDealValue = deal.getExpectedDealValue();

                    yield Objects.nonNull(expectedDealValue) ? expectedDealValue.getValue() : null;
                }
                case "description" -> TEXT_NORMALIZER.apply(deal.getDescription());
                case "type" -> {
                    final Option type = deal.getType();

                    yield Objects.nonNull(type) ? type.id() : null;
                }
                case "paymentStatus" -> {
                    final Option paymentStatus = deal.getPaymentStatus();

                    yield Objects.nonNull(paymentStatus) ? paymentStatus.id() : null;
                }
                case "status" -> deal.getState().getId();
                case "dealStageUpdatedAt" -> dateTimeUtil.getTimeStamp(deal.getStageUpdatedAt());
                case "webFormName" -> TEXT_NORMALIZER.apply(deal.getSummaryInfo().getWebFormName());
                case "createdBy" -> deal.getCreatedBy().id();
                case "dealCreated" -> dateTimeUtil.getTimeStamp(deal.getCreatedTime());
                case "updatedBy" -> deal.getUpdatedBy().id();
                case "updatedAt" -> dateTimeUtil.getTimeStamp(deal.getModifiedTime());
                case "lastActivityDate" -> {
                    final SummaryInfo contactSummary = deal.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        if (Objects.nonNull(activitySummary)) {
                            yield dateUtil.getTimeStamp(activitySummary.getLastActivityDate());
                        }
                    }
                    yield null;
                }
                case "nextActivityDate" -> {
                    final SummaryInfo contactSummary = deal.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        if (Objects.nonNull(activitySummary)) {
                            yield dateUtil.getTimeStamp(activitySummary.getNextActivityDate());
                        }
                    }
                    yield null;
                }
                case "lastActivityType" -> {
                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                    if (Objects.nonNull(dealSummary)) {
                        final ActivitySummary activitySummary = dealSummary.getActivitySummary();

                        if (Objects.nonNull(activitySummary)) {
                            yield getLastActivityTypeId(deal.orgId(), activitySummary);
                        }
                    }
                    yield null;
                }
                case "doneActivities" -> {
                    final SummaryInfo contactSummary = deal.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getDoneActivities());
                    }
                    yield null;
                }
                case "upcomingActivities" -> {
                    final SummaryInfo contactSummary = deal.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getUpcomingActivities());
                    }
                    yield null;
                }
                case "totalActivities" -> {
                    final SummaryInfo contactSummary = deal.getSummaryInfo();

                    if (Objects.nonNull(contactSummary)) {
                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                        yield Objects.isNull(activitySummary) ? null : nullifyZero(activitySummary.getTotalActivities());
                    }
                    yield null;
                }
                case "lastEmailReceived" -> {
                    final SummaryInfo companySummaryInfo = deal.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final EmailSummary emailSummary = companySummaryInfo.getEmailSummary();

                        if (Objects.nonNull(emailSummary)) {
                            yield dateUtil.getTimeStamp(emailSummary.getLastEmailReceived());
                        }
                    }
                    yield null;
                }
                case "emailMessageCount" -> {
                    final SummaryInfo companySummaryInfo = deal.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final EmailSummary emailSummary = companySummaryInfo.getEmailSummary();

                        yield Objects.isNull(emailSummary) ? null : emailSummary.getEmailMessageCount();
                    }
                    yield null;
                }
                case "lastEmailSent" -> {
                    final SummaryInfo companySummaryInfo = deal.getSummaryInfo();

                    if (Objects.nonNull(companySummaryInfo)) {
                        final EmailSummary emailSummary = companySummaryInfo.getEmailSummary();

                        if (Objects.nonNull(emailSummary)) {
                            yield dateUtil.getTimeStamp(emailSummary.getLastEmailSent());
                        }
                    }
                    yield null;
                }
                case "dealValueInBaseCurrency" -> {
                    final Monetary monetary = deal.getDealValue();

                    if (Objects.nonNull(monetary)) {
                        final BigDecimal value = monetary.getValue();
                        final BigDecimal exchangeRate = baseCurrencyService.getCurrencyExchangeRate(monetary.orgId(), monetary.getCurrency());

                        yield Objects.isNull(exchangeRate) ? value : value.divide(exchangeRate, NumericalConstant.FIVE, RoundingMode.HALF_EVEN);
                    }
                    yield null;
                }
                case "recentNote" -> TEXT_NORMALIZER.apply(deal.getRecentNote());
                default -> null;
            };
        }
    }
}
