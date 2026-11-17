package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.sql.resource.OACDIMCOMPANY;
import com.tlc.analytics.sql.resource.OACDIMCONTACT;
import com.tlc.analytics.sql.resource.OACDIMDEAL;
import com.tlc.analytics.sql.resource.OACFACTCOMPANYDEAL;
import com.tlc.analytics.sql.resource.OACFACTCONTACTDEAL;
import com.tlc.analytics.sql.resource.OACFACTDEALPRODUCT;
import com.tlc.analytics.sql.resource.OACFACTDEALACTIVITY;

import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Enriches CRM deal CDC events with analytics specific data.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AnalyticsCrmDealEnricher extends AbstractAnalyticsCdcEnricher {

    private final DealManager dealManager;

    private AnalyticsCrmDealEnricher() {
        this.dealManager = DealManager.getInstance();
    }

    private static class Instance {
        private static final AnalyticsCrmDealEnricher INSTANCE = new AnalyticsCrmDealEnricher();
    }

    static AnalyticsCrmDealEnricher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupedRecords Map of JSON objects to be enriched which is grouped by table.
     * @return Collection of enriched records.
     */
    @Override
    public Collection<EnrichedRecord> toEnrichedRecords(final Map<String, Collection<JsonObject>> groupedRecords) {
        final Collection<TableDetail> tableDetails = metaHandleService.getTableDetails(getModuleId(groupedRecords.keySet()));
        final Collection<EnrichedRecord> enrichedRecords = new HashSet<>();

        for (final TableDetail chTableDetails : tableDetails) {
            switch (chTableDetails.getTableName()) {
                case OACDIMDEAL.TABLE -> loadDimensionDeal(chTableDetails, groupedRecords, enrichedRecords);
            }
        }

        return enrichedRecords;
    }

    /**
     * Loads dimension deal data from the given JSON objects into enriched records.
     *
     * @param chTableDetails  ClickHouse table details for dimension deal.
     * @param groupedRecords  Map of JSON objects containing deal data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void loadDimensionDeal(final TableDetail chTableDetails, final Map<String, Collection<JsonObject>> groupedRecords, final Collection<EnrichedRecord> enrichedRecords) {
        final Collection<JsonObject> records = groupedRecords.get(MDODEAL.TABLE.toLowerCase());

        if (Objects.nonNull(records) && !records.isEmpty()) {

            for (final JsonObject dealJson : records) {
                final String operation = dealJson.getString(AnalyticsCdcKeys.OPERATION);
                final long transactionId = dealJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID);
                final boolean isDeleteEvent = Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, operation);
                final JsonObject jsonObject = isDeleteEvent ? getRowJson(tableResolver.resolve(OACDIMDEAL.TABLE), dealJson.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(MDODEAL.ID)) : Json.object();

                if (isDeleteEvent) {
                    final long dealId = jsonObject.getLong(OACDIMDEAL.ID);

                    jsonObject.put(OACDIMDEAL.UPDATED_AT, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                    jsonObject.put(OACDIMDEAL.IS_DELETED, true);

                    deleteDealCompany(dealId, enrichedRecords, transactionId);
                    deleteDealContact(dealId, enrichedRecords, transactionId);
                    deleteDealProduct(dealId, enrichedRecords, transactionId);
                    deleteDealActivity(dealId, enrichedRecords, transactionId);
                } else {
                    final JsonObject cdcJson = dealJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                    final long orgId = cdcJson.getLong(MDODEAL.ORG_ID);

                    secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
                        final Deal deal = dealManager.get(orgId, cdcJson.getLong(MDODEAL.ID));
                        final Long dealId = deal.id();

                        dealManager.loadSummaryDetails(orgId, deal);

                        for (final ColumnDetail chColumnDetails : metaHandleService.getColumnDetails(chTableDetails.id())) {
                            final String columnName = chColumnDetails.getColumnName();

                            switch (columnName) {
                                case OACDIMDEAL.ID -> jsonObject.put(columnName, cdcJson.getLong(MDODEAL.ID));
                                case OACDIMDEAL.ORG_ID -> jsonObject.put(columnName, orgId);
                                case OACDIMDEAL.TITLE -> jsonObject.put(columnName, cdcJson.getString(MDODEAL.TITLE));
                                case OACDIMDEAL.PIPELINE -> {
                                    final Pipeline pipeline = deal.getPipeline();

                                    if (Objects.nonNull(pipeline)) {
                                        jsonObject.put(columnName, pipeline.getName());
                                    }
                                }
                                case OACDIMDEAL.PIPELINE_STAGE -> {
                                    final PipelineStage pipelineStage = deal.getPipelineStage();

                                    if (Objects.nonNull(pipelineStage)) {
                                        jsonObject.put(columnName, pipelineStage.getName());
                                    }
                                }
                                case OACDIMDEAL.WON_REASON -> {
                                    final Option wonReason = deal.getWonReason();

                                    if (Objects.nonNull(wonReason)) {
                                        jsonObject.put(columnName, wonReason.getOption());
                                    }
                                }
                                case OACDIMDEAL.LOST_REASON -> {
                                    final Option lostReason = deal.getLostReason();

                                    if (Objects.nonNull(lostReason)) {
                                        jsonObject.put(columnName, lostReason.getOption());
                                    }
                                }
                                case OACDIMDEAL.DEAL_CLOSED_ON -> {
                                    final DateTime closedTime = deal.getClosedTime();

                                    if (Objects.nonNull(closedTime)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(closedTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMDEAL.PRIMARY_CONTACT -> {
                                    final Contact primaryContact = deal.getPrimaryContact();

                                    if (Objects.nonNull(primaryContact)) {
                                        final Long contactId = primaryContact.id();

                                        jsonObject.put(columnName, contactId);
                                        loadFactDealContact(deal, primaryContact, enrichedRecords, transactionId);
                                        buildDealStatisticsSummary(contactId, tableResolver.resolve(OACDIMCONTACT.TABLE), contactManager.getDealStatistics(orgId, contactId), enrichedRecords, transactionId, deal);
                                    }
                                }
                                case OACDIMDEAL.RELATED_CONTACT -> {
                                    final Collection<Participant> participants = deal.getParticipants();

                                    if (Objects.nonNull(participants) && !participants.isEmpty()) {
                                        jsonObject.put(columnName, participants.stream().map(participant -> participant.getContact().id()).collect(Collectors.toList()));
                                    }
                                }
                                case OACDIMDEAL.SALES_OWNER ->
                                        jsonObject.put(columnName, cdcJson.getLong(MDODEAL.OWNER_ID));
                                case OACDIMDEAL.COMPANY -> {
                                    final Company company = deal.getCompany();

                                    if (Objects.nonNull(company)) {
                                        final Long companyId = company.id();

                                        jsonObject.put(columnName, companyId);
                                        loadFactDealCompany(deal, company, enrichedRecords, transactionId);
                                        buildDealStatisticsSummary(companyId, tableResolver.resolve(OACDIMCOMPANY.TABLE), companyManager.getDealStatistics(orgId, companyId), enrichedRecords, transactionId, deal);
                                    } else {
                                        jsonObject.put(columnName, company);
                                        deleteDealCompany(dealId, enrichedRecords, transactionId);
                                    }
                                }
                                case OACDIMDEAL.DEAL_VALUE -> {
                                    final DealValue dealValue = deal.getDealValue();

                                    if (Objects.nonNull(dealValue)) {
                                        jsonObject.put(columnName, dealValue.getValue());
                                    }
                                }
                                case OACDIMDEAL.DEAL_VALUE_IN_BASE_CURRENCY -> {
                                    final Monetary dealValue = deal.getDealValue();

                                    if (Objects.nonNull(dealValue)) {
                                        final BigDecimal value = dealValue.getValue();
                                        final BigDecimal exchangeRate = baseCurrencyService.getCurrencyExchangeRate(orgId, dealValue.getCurrency());

                                        jsonObject.put(columnName, Objects.isNull(exchangeRate) ? value : value.divide(exchangeRate, NumericalConstant.FIVE, RoundingMode.HALF_EVEN));
                                    }
                                }
                                case OACDIMDEAL.EXPECTED_CLOSED_DATE -> {
                                    final Date expectedCloseDate = deal.getExpectedCloseDate();

                                    if (Objects.nonNull(expectedCloseDate)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(expectedCloseDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMDEAL.ROTTING_DAYS -> jsonObject.put(columnName, deal.getRottenDays());
                                case OACDIMDEAL.TERRITORY ->
                                        jsonObject.put(columnName, cdcJson.optString(MDODEAL.TERRITORY_ID, null));
                                case OACDIMDEAL.TAGS -> {
                                    final SummaryInfo companySummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final Collection<Tag> tags = companySummary.getTags();

                                        if (Objects.nonNull(tags) && !tags.isEmpty()) {
                                            jsonObject.put(columnName, tags.stream().map(Tag::getName).toList());
                                        }
                                    }
                                }
                                case OACDIMDEAL.AGE -> jsonObject.put(columnName, deal.getAge());
                                case OACDIMDEAL.WON_TIME -> {
                                    final DateTime wonTime = deal.getWonTime();

                                    if (Objects.nonNull(wonTime)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(wonTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMDEAL.LOST_TIME -> {
                                    final DateTime lostTime = deal.getLostTime();

                                    if (Objects.nonNull(lostTime)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(lostTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMDEAL.STAGE_PROBABILITY ->
                                        jsonObject.put(columnName, deal.getStageProbability());
                                case OACDIMDEAL.SOURCE -> {
                                    final Option source = deal.getSource();

                                    if (Objects.nonNull(source)) {
                                        jsonObject.put(columnName, source.getOption());
                                    }
                                }
                                case OACDIMDEAL.WEIGHTED_VALUE -> {
                                    final Monetary weightedRevenue = deal.getWeightedRevenue();

                                    if (Objects.nonNull(weightedRevenue)) {
                                        jsonObject.put(columnName, weightedRevenue.getValue());
                                    }
                                }
                                case OACDIMDEAL.EXPECTED_DEAL_VALUE -> {
                                    final Monetary expectedDealValue = deal.getExpectedDealValue();

                                    if (Objects.nonNull(expectedDealValue)) {
                                        jsonObject.put(columnName, expectedDealValue.getValue());
                                    }
                                }
                                case OACDIMDEAL.PRODUCT_AMOUNT -> {
                                    final Monetary productAmount = deal.getProductAmount();

                                    if (Objects.nonNull(productAmount)) {
                                        jsonObject.put(columnName, productAmount.getValue());
                                    }
                                }
                                case OACDIMDEAL.STATE -> {
                                    final Deal.State state = deal.getState();

                                    if (Objects.nonNull(state)) {
                                        jsonObject.put(columnName, state.getName());
                                    }
                                }
                                case OACDIMDEAL.DEAL_STAGE_UPDATED_AT -> {
                                    final DateTime stageUpdatedAt = deal.getStageUpdatedAt();

                                    if (Objects.nonNull(stageUpdatedAt)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(stageUpdatedAt.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMDEAL.PAYMENT_STATUS -> {
                                    final Option paymentStatus = deal.getPaymentStatus();

                                    if (Objects.nonNull(paymentStatus)) {
                                        jsonObject.put(columnName, paymentStatus.getOption());
                                    }
                                }
                                case OACDIMDEAL.PRODUCT_QUANTITY -> {
                                    final long totalProductCount = deal.getTotalProductCount();

                                    jsonObject.put(columnName, totalProductCount);

                                    if (totalProductCount > 0) {
                                        loadFactDealProduct(deal, deal.getProductDetailsHolder(), enrichedRecords, transactionId);
                                    } else {
                                        deleteDealProduct(dealId, enrichedRecords, transactionId);
                                    }
                                }
                                case OACDIMDEAL.CREATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MDODEAL.CREATED_BY));
                                case OACDIMDEAL.CREATED_AT ->
                                        jsonObject.put(columnName, TemporalServiceProvider.get(deal.getCreatedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                case OACDIMDEAL.UPDATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MDODEAL.UPDATED_BY));
                                case OACDIMDEAL.UPDATED_AT ->
                                        jsonObject.put(columnName, TemporalServiceProvider.get(deal.getModifiedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                case OACDIMDEAL.LAST_ASSIGNED_AT -> {
                                    final DateTime lastAssignedAt = deal.getLastAssignedAt();
                                    if (Objects.nonNull(lastAssignedAt)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(lastAssignedAt.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMDEAL.DESCRIPTION -> jsonObject.put(columnName, deal.getDescription());
                                case OACDIMDEAL.TYPE -> {
                                    final Option type = deal.getType();

                                    if (Objects.nonNull(type)) {
                                        jsonObject.put(columnName, type.getOption());
                                    }
                                }
                                case OACDIMDEAL.WEBFORM -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        jsonObject.put(columnName, dealSummary.getWebFormName());
                                    }
                                }
                                case OACDIMDEAL.LAST_ACTIVITY_DATE -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final ActivitySummary activitySummary = dealSummary.getActivitySummary();

                                        if (Objects.nonNull(activitySummary)) {
                                            final Date lastActivityDate = activitySummary.getLastActivityDate();

                                            if (Objects.nonNull(lastActivityDate)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(lastActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMDEAL.NEXT_ACTIVITY_DATE -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final ActivitySummary activitySummary = dealSummary.getActivitySummary();

                                        if (Objects.nonNull(activitySummary)) {
                                            final Date nextActivityDate = activitySummary.getNextActivityDate();

                                            if (Objects.nonNull(nextActivityDate)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(nextActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMDEAL.LAST_ACTIVITY_TYPE -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final ActivitySummary activitySummary = dealSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? null : activitySummary.getLastActivityType());
                                    }
                                }
                                case OACDIMDEAL.DONE_ACTIVITIES -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final ActivitySummary activitySummary = dealSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getDoneActivities());
                                    }
                                }
                                case OACDIMDEAL.UPCOMING_ACTIVITIES -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final ActivitySummary activitySummary = dealSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getUpcomingActivities());
                                    }
                                }
                                case OACDIMDEAL.TOTAL_ACTIVITIES -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final ActivitySummary activitySummary = dealSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getTotalActivities());
                                    }
                                }
                                case OACDIMDEAL.EMAIL_MESSAGE_COUNT -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final EmailSummary emailSummary = dealSummary.getEmailSummary();

                                        jsonObject.put(columnName, Objects.isNull(emailSummary) ? 0 : emailSummary.getEmailMessageCount());
                                    }
                                }
                                case OACDIMDEAL.LAST_EMAIL_RECEIVED -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final EmailSummary emailSummary = dealSummary.getEmailSummary();

                                        if (Objects.nonNull(emailSummary)) {
                                            final Long timestamp = emailSummary.getLastEmailReceived().getTimeStamp();

                                            if (Objects.nonNull(timestamp)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(timestamp).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMDEAL.LAST_EMAIL_SENT -> {
                                    final SummaryInfo dealSummary = deal.getSummaryInfo();

                                    if (Objects.nonNull(dealSummary)) {
                                        final EmailSummary emailSummary = dealSummary.getEmailSummary();

                                        if (Objects.nonNull(emailSummary)) {
                                            final Long timestamp = emailSummary.getLastEmailSent().getTimeStamp();

                                            if (Objects.nonNull(timestamp)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(timestamp).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMDEAL.IS_DELETED -> jsonObject.put(columnName, false);
                            }
                        }
                        buildCustomFields(deal, jsonObject, Source.DEAL);
                    });
                }

                jsonObject.put(AnalyticsCdcKeys.TOPIC, chTableDetails.getTableName().toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Loads fact deal company record using the given deal and company details.
     *
     * @param deal            Deal entity associated with the company.
     * @param company         Company entity associated with the deal.
     * @param enrichedRecords Collection of enriched records to be updated.
     * @param transactionId   Transaction identifier for the deal.
     */
    private void loadFactDealCompany(final Deal deal, final Company company, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final JsonObject jsonObject = Json.object();
        final Deal.State state = deal.getState();
        final DealValue dealValue = deal.getDealValue();

        jsonObject.put(OACFACTCOMPANYDEAL.ORG_ID, deal.orgId());
        jsonObject.put(OACFACTCOMPANYDEAL.DEAL_ID, deal.id());
        jsonObject.put(OACFACTCOMPANYDEAL.COMPANY_ID, company.id());
        jsonObject.put(OACFACTCOMPANYDEAL.IS_DELETED, false);
        jsonObject.put(OACFACTCOMPANYDEAL.TIMESTAMP, TemporalServiceProvider.get(deal.getModifiedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

        if (Objects.nonNull(dealValue)) {
            final Currency currency = dealValue.getCurrency();

            jsonObject.put(OACFACTCOMPANYDEAL.DEAL_VALUE, dealValue.getValue());

            if (Objects.nonNull(currency)) {
                jsonObject.put(OACFACTCOMPANYDEAL.CURRENCY, currency.getCode());
            }
        }

        if (Objects.nonNull(state)) {
            jsonObject.put(OACFACTCOMPANYDEAL.STATE, state.getName());
        }

        jsonObject.put(AnalyticsCdcKeys.TOPIC, OACFACTCOMPANYDEAL.TABLE.toLowerCase());
        enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
    }

    /**
     * Loads fact deal contact record using the given deal and contact details.
     *
     * @param deal            Deal entity associated with the contact.
     * @param contact         Contact entity associated with the deal.
     * @param enrichedRecords Collection of enriched records to be updated.
     * @param transactionId   Transaction identifier for the deal.
     */
    private void loadFactDealContact(final Deal deal, final Contact contact, Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final JsonObject jsonObject = Json.object();
        final Deal.State state = deal.getState();
        final DealValue dealValue = deal.getDealValue();

        jsonObject.put(OACFACTCONTACTDEAL.ORG_ID, deal.orgId());
        jsonObject.put(OACFACTCONTACTDEAL.DEAL_ID, deal.id());
        jsonObject.put(OACFACTCONTACTDEAL.CONTACT_ID, contact.id());
        jsonObject.put(OACFACTCONTACTDEAL.IS_DELETED, false);
        jsonObject.put(OACFACTCONTACTDEAL.TIMESTAMP, TemporalServiceProvider.get(deal.getModifiedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

        if (Objects.nonNull(dealValue)) {
            final Currency currency = dealValue.getCurrency();

            jsonObject.put(OACFACTCONTACTDEAL.DEAL_VALUE, dealValue.getValue());

            if (Objects.nonNull(currency)) {
                jsonObject.put(OACFACTCONTACTDEAL.CURRENCY, currency.getCode());
            }
        }

        if (Objects.nonNull(state)) {
            jsonObject.put(OACFACTCONTACTDEAL.STATE, state.getName());
        }

        jsonObject.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTDEAL.TABLE.toLowerCase());
        enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
    }

    /**
     * Loads fact deal product record using the given deal and product details.
     *
     * @param deal                 Deal entity associated with the product.
     * @param productDetailsHolder Product details associated with the deal.
     * @param enrichedRecords      Collection of enriched records to be updated.
     * @param transactionId        Transaction identifier for the deal.
     */
    private void loadFactDealProduct(final Deal deal, final ProductDetailsHolder productDetailsHolder, Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails) && !productDetails.isEmpty()) {
            for (final ProductDetail productDetail : productDetails) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(OACFACTDEALPRODUCT.ORG_ID, deal.orgId());
                jsonObject.put(OACFACTDEALPRODUCT.DEAL_ID, deal.id());
                jsonObject.put(OACFACTDEALPRODUCT.PRODUCT_ID, productDetail.getProduct().id());
                jsonObject.put(OACFACTDEALPRODUCT.TIMESTAMP, TemporalServiceProvider.get(deal.getModifiedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                jsonObject.put(OACFACTDEALPRODUCT.IS_DELETED, false);

                jsonObject.put(AnalyticsCdcKeys.TOPIC, OACFACTDEALPRODUCT.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Marks related fact deal company records as deleted for the given deal identifier.
     *
     * @param dealId          Identifier of the deal for which related company records are to be marked as deleted.
     * @param enrichedRecords Collection of enriched records to be updated with delete information.
     * @param transactionId   Transaction identifier for the deal.
     */
    private void deleteDealCompany(final long dealId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table dealCompanyTable = tableResolver.resolve(OACFACTCOMPANYDEAL.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(dealCompanyTable.getColumn(OACFACTCOMPANYDEAL.DEAL_ID), dealId));

        if (exists(dealCompanyTable, whereClause)) {
            final Collection<JsonObject> dealCompanies = getRowsJson(dealCompanyTable, whereClause);

            for (final JsonObject dealCompanyJson : dealCompanies) {
                dealCompanyJson.put(OACFACTCOMPANYDEAL.IS_DELETED, true);
                dealCompanyJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCOMPANYDEAL.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, dealCompanyJson));
            }
        }
    }

    /**
     * Marks related fact deal contact records as deleted for the given deal identifier.
     *
     * @param dealId          Identifier of the deal for which related contact records are to be marked as deleted.
     * @param enrichedRecords Collection of enriched records to be updated with delete information.
     * @param transactionId   Transaction identifier for the deal.
     */
    private void deleteDealContact(final long dealId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table contactDealTable = tableResolver.resolve(OACFACTCONTACTDEAL.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(contactDealTable.getColumn(OACFACTCONTACTDEAL.DEAL_ID), dealId));

        if (exists(contactDealTable, whereClause)) {
            final Collection<JsonObject> contactDeals = getRowsJson(contactDealTable, whereClause);

            for (final JsonObject contactDealJson : contactDeals) {
                contactDealJson.put(OACFACTCONTACTDEAL.IS_DELETED, true);
                contactDealJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTDEAL.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, contactDealJson));
            }
        }
    }

    /**
     * Marks related fact deal product records as deleted for the given deal identifier.
     *
     * @param dealId          Identifier of the deal for which related product records are to be marked as deleted.
     * @param enrichedRecords Collection of enriched records to be updated with delete information.
     * @param transactionId   Transaction identifier for the deal.
     */
    private void deleteDealProduct(final long dealId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table dealProductTable = tableResolver.resolve(OACFACTDEALPRODUCT.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(dealProductTable.getColumn(OACFACTDEALPRODUCT.DEAL_ID), dealId));

        if (exists(dealProductTable, whereClause)) {
            final Collection<JsonObject> contactDeals = getRowsJson(dealProductTable, whereClause);

            for (final JsonObject contactDealJson : contactDeals) {
                contactDealJson.put(OACFACTDEALPRODUCT.IS_DELETED, true);
                contactDealJson.put(AnalyticsCdcKeys.TOPIC, OACFACTDEALPRODUCT.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, contactDealJson));
            }
        }
    }

    /**
     * Marks related fact deal activity records as deleted for the given deal identifier.
     *
     * @param dealId          Identifier of the deal for which related activity records are to be marked as deleted.
     * @param enrichedRecords Collection of enriched records to be updated with delete information.
     * @param transactionId   Transaction identifier for the deal.
     */
    private void deleteDealActivity(final long dealId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table factDealActivityTable = tableResolver.resolve(OACFACTDEALACTIVITY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(factDealActivityTable.getColumn(OACFACTDEALACTIVITY.DEAL_ID), dealId));

        if (exists(factDealActivityTable, whereClause)) {
            final Collection<JsonObject> dealActivities = getRowsJson(factDealActivityTable, whereClause);

            for (final JsonObject dealActivityJson : dealActivities) {
                dealActivityJson.put(OACFACTDEALACTIVITY.IS_DELETED, true);
                dealActivityJson.put(AnalyticsCdcKeys.TOPIC, OACFACTDEALACTIVITY.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, dealActivityJson));
            }
        }
    }
}
