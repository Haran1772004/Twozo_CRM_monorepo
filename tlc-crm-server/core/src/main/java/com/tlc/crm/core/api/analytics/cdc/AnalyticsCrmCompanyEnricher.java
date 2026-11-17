package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.sql.resource.OACDIMCOMPANY;
import com.tlc.analytics.sql.resource.OACFACTCOMPANYACTIVITY;
import com.tlc.analytics.sql.resource.OACFACTCOMPANYDEAL;
import com.tlc.analytics.sql.resource.OACFACTCONTACTCOMPANY;

import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.features.api.tag.model.Tag;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.meta.DatabaseType;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Enriches CRM company CDC events with analytics specific data.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AnalyticsCrmCompanyEnricher extends AbstractAnalyticsCdcEnricher {

    private final CompanyManager companyManager;

    private AnalyticsCrmCompanyEnricher() {
        this.companyManager = CompanyManager.getInstance();
    }

    private static class Instance {
        private static final AnalyticsCrmCompanyEnricher COMPANY_ENRICHER = new AnalyticsCrmCompanyEnricher();
    }

    static AnalyticsCrmCompanyEnricher getInstance() {
        return Instance.COMPANY_ENRICHER;
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
                case OACDIMCOMPANY.TABLE -> buildDimensionCompany(chTableDetails, groupedRecords, enrichedRecords);
            }
        }

        return enrichedRecords;
    }

    /**
     * Loads dimension company data from the given JSON objects into enriched records.
     *
     * @param chTableDetails  ClickHouse table details for dimension company.
     * @param groupedRecords  Map of JSON objects containing company data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void buildDimensionCompany(final TableDetail chTableDetails, final Map<String, Collection<JsonObject>> groupedRecords, final Collection<EnrichedRecord> enrichedRecords) {
        final Collection<JsonObject> records = groupedRecords.get(MCOCOMPANY.TABLE.toLowerCase());

        if (Objects.nonNull(records) && !records.isEmpty()) {

            for (final JsonObject companyJson : records) {
                final String operation = companyJson.getString(AnalyticsCdcKeys.OPERATION);
                final long transactionId = companyJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID);
                final boolean isDeleteEvent = Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, operation);
                final JsonObject jsonObject = isDeleteEvent ? getRowJson(Table.get(DatabaseType.OLAP, OACDIMCOMPANY.TABLE), companyJson.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(MCOCOMPANY.ID)) : Json.object();

                if (isDeleteEvent) {
                    final long companyId = jsonObject.getLong(OACDIMCOMPANY.ID);

                    jsonObject.put(OACDIMCOMPANY.UPDATED_AT, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                    jsonObject.put(OACDIMCOMPANY.IS_DELETED, true);

                    deleteContactCompany(companyId, enrichedRecords, transactionId);
                    deleteDealCompany(companyId, enrichedRecords, transactionId);
                    deleteCompanyActivity(companyId, enrichedRecords, transactionId);
                } else {
                    final JsonObject cdcJson = companyJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                    final long orgId = cdcJson.getLong(MCOCOMPANY.ORG_ID);

                    secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
                        final Company company = companyManager.get(orgId, cdcJson.getLong(MCOCOMPANY.ID));

                        companyManager.loadSummaryDetails(orgId, company);

                        for (final ColumnDetail chColumnDetails : metaHandleService.getColumnDetails(chTableDetails.id())) {
                            final String columnName = chColumnDetails.getColumnName();

                            switch (columnName) {
                                case OACDIMCOMPANY.ID -> jsonObject.put(columnName, cdcJson.getLong(MCOCOMPANY.ID));
                                case OACDIMCOMPANY.ORG_ID -> jsonObject.put(columnName, orgId);
                                case OACDIMCOMPANY.NAME ->
                                        jsonObject.put(columnName, cdcJson.getString(MCOCOMPANY.NAME));
                                case OACDIMCOMPANY.WEBSITE ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCOMPANY.WEBSITE, null));
                                case OACDIMCOMPANY.SALES_OWNER ->
                                        jsonObject.put(columnName, cdcJson.getLong(MCOCOMPANY.OWNER_ID));
                                case OACDIMCOMPANY.ANNUAL_REVENUE -> {
                                    final Monetary annualRevenue = company.getAnnualRevenue();

                                    if (Objects.nonNull(annualRevenue)) {
                                        jsonObject.put(columnName, annualRevenue.getValue());
                                    }
                                }
                                case OACDIMCOMPANY.SIC_CODE ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCOMPANY.SIC_CODE, null));
                                case OACDIMCOMPANY.TERRITORY ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCOMPANY.TERRITORY_ID, null));
                                case OACDIMCOMPANY.TAGS -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final Collection<Tag> tags = companySummary.getTags();

                                        if (Objects.nonNull(tags) && !tags.isEmpty()) {
                                            jsonObject.put(columnName, tags.stream().map(Tag::getName).toList());
                                        }
                                    }
                                }
                                case OACDIMCOMPANY.EMPLOYEES ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCOMPANY.NUMBER_OF_EMPLOYEE, null));
                                case OACDIMCOMPANY.PARENT_COMPANY -> {
                                    final Company parentCompany = company.getParentCompany();

                                    if (Objects.nonNull(parentCompany)) {
                                        jsonObject.put(columnName, parentCompany.id());
                                    }
                                }
                                case OACDIMCOMPANY.ORGANISATION_STATUS -> {
                                    final Option organizationStatus = company.getOrganizationStatus();

                                    if (Objects.nonNull(organizationStatus)) {
                                        jsonObject.put(columnName, organizationStatus.getOption());
                                    }
                                }
                                case OACDIMCOMPANY.INDUSTRY_TYPE -> {
                                    final Option industryType = company.getIndustryType();

                                    if (Objects.nonNull(industryType)) {
                                        jsonObject.put(columnName, industryType.getOption());
                                    }
                                }
                                case OACDIMCOMPANY.BUSINESS_TYPE -> {
                                    final Option businessType = company.getBusinessType();

                                    if (Objects.nonNull(businessType)) {
                                        jsonObject.put(columnName, businessType.getOption());
                                    }
                                }
                                case OACDIMCOMPANY.DESCRIPTION ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCOMPANY.DESCRIPTION, null));
                                case OACDIMCOMPANY.CREATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MCOCOMPANY.CREATED_BY));
                                case OACDIMCOMPANY.CREATED_AT -> {
                                    final DateTime createdTime = company.getCreatedTime();
                                    jsonObject.put(columnName, TemporalServiceProvider.get(createdTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                }
                                case OACDIMCOMPANY.UPDATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MCOCOMPANY.UPDATED_BY));
                                case OACDIMCOMPANY.UPDATED_AT -> {
                                    final DateTime updatedTime = company.getModifiedTime();
                                    jsonObject.put(columnName, TemporalServiceProvider.get(updatedTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                }
                                case OACDIMCOMPANY.LAST_ASSIGNED_AT -> {
                                    final DateTime lastAssignedAt = company.getLastAssignedAt();

                                    if (Objects.nonNull(lastAssignedAt)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(lastAssignedAt.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMCOMPANY.OPEN_DEALS -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getOpenDealsCount());
                                    }
                                }
                                case OACDIMCOMPANY.CLOSED_DEALS -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getClosedDealsCount());
                                    }
                                }
                                case OACDIMCOMPANY.WON_DEALS -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getWonDealsCount());
                                    }
                                }
                                case OACDIMCOMPANY.LOST_DEALS -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final DealStatistics dealStatistics = companySummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getLostDealsCount());
                                    }
                                }
                                case OACDIMCOMPANY.LAST_ACTIVITY_DATE -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                                        if (Objects.nonNull(activitySummary)) {
                                            final Date lastActivityDate = activitySummary.getLastActivityDate();

                                            if (Objects.nonNull(lastActivityDate)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(lastActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCOMPANY.NEXT_ACTIVITY_DATE -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                                        if (Objects.nonNull(activitySummary)) {
                                            final Date nextActivityDate = activitySummary.getNextActivityDate();

                                            if (Objects.nonNull(nextActivityDate)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(nextActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCOMPANY.LAST_ACTIVITY_TYPE -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? null : activitySummary.getLastActivityType());
                                    }
                                }
                                case OACDIMCOMPANY.DONE_ACTIVITIES -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getDoneActivities());
                                    }
                                }
                                case OACDIMCOMPANY.UPCOMING_ACTIVITIES -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getUpcomingActivities());
                                    }
                                }
                                case OACDIMCOMPANY.TOTAL_ACTIVITIES -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final ActivitySummary activitySummary = companySummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getTotalActivities());
                                    }
                                }
                                case OACDIMCOMPANY.EMAIL_MESSAGE_COUNT -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final EmailSummary emailSummary = companySummary.getEmailSummary();

                                        jsonObject.put(columnName, Objects.isNull(emailSummary) ? 0 : emailSummary.getEmailMessageCount());
                                    }
                                }
                                case OACDIMCOMPANY.LAST_EMAIL_RECEIVED -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final EmailSummary emailSummary = companySummary.getEmailSummary();

                                        if (Objects.nonNull(emailSummary)) {
                                            final Long timestamp = emailSummary.getLastEmailReceived().getTimeStamp();

                                            if (Objects.nonNull(timestamp)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(timestamp).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCOMPANY.LAST_EMAIL_SENT -> {
                                    final SummaryInfo companySummary = company.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final EmailSummary emailSummary = companySummary.getEmailSummary();

                                        if (Objects.nonNull(emailSummary)) {
                                            final Long timestamp = emailSummary.getLastEmailSent().getTimeStamp();

                                            if (Objects.nonNull(timestamp)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(timestamp).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCOMPANY.CITY -> {
                                    final Address address = company.getAddress();

                                    if (Objects.nonNull(address)) {
                                        jsonObject.put(columnName, address.getCity());
                                    }
                                }
                                case OACDIMCOMPANY.STATE -> {
                                    final Address address = company.getAddress();

                                    if (Objects.nonNull(address)) {
                                        jsonObject.put(columnName, address.getState());
                                    }
                                }
                                case OACDIMCOMPANY.COUNTRY -> {
                                    final Address address = company.getAddress();

                                    if (Objects.nonNull(address)) {
                                        final Country country = address.getCountry();

                                        if (Objects.nonNull(country)) {
                                            jsonObject.put(columnName, address.getCountry().getName());
                                        }
                                    }
                                }
                                case OACDIMCOMPANY.PIN_CODE -> {
                                    final Address address = company.getAddress();

                                    if (Objects.nonNull(address)) {
                                        jsonObject.put(columnName, address.getZipcode());
                                    }
                                }
                                case OACDIMCOMPANY.IS_DELETED -> jsonObject.put(columnName, false);
                            }
                        }
                        buildCustomFields(company, jsonObject, Source.COMPANY);
                    });
                }

                jsonObject.put(AnalyticsCdcKeys.TOPIC, chTableDetails.getTableName().toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Marks related contact-company records as deleted for the given company ID and adds them to enriched records.
     *
     * @param companyId       The ID of the deleted company.
     * @param enrichedRecords Collection of enriched records to be updated with deleted contact-company records.
     * @param transactionId   The transaction ID associated with the delete event.
     */
    private void deleteContactCompany(final long companyId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table contactCompanyTable = tableResolver.resolve(OACFACTCONTACTCOMPANY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(contactCompanyTable.getColumn(OACFACTCONTACTCOMPANY.COMPANY_ID), companyId));

        if (exists(contactCompanyTable, whereClause)) {
            final Collection<JsonObject> contactCompanies = getRowsJson(contactCompanyTable, whereClause);

            for (final JsonObject contactCompanyJson : contactCompanies) {
                contactCompanyJson.put(OACFACTCONTACTCOMPANY.IS_DELETED, true);
                contactCompanyJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTCOMPANY.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, contactCompanyJson));
            }
        }
    }

    private void deleteDealCompany(final long companyId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table dealCompanyTable = tableResolver.resolve(OACFACTCOMPANYDEAL.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(dealCompanyTable.getColumn(OACFACTCOMPANYDEAL.COMPANY_ID), companyId));

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
     * Marks related company activity records as deleted for the given company ID and adds them to enriched records.
     *
     * @param companyId       The ID of the deleted company.
     * @param enrichedRecords Collection of enriched records to be updated with deleted company activity records.
     * @param transactionId   The transaction ID associated with the delete event.
     */
    private void deleteCompanyActivity(final long companyId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table factCompanyActivityTable = tableResolver.resolve(OACFACTCOMPANYACTIVITY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(factCompanyActivityTable.getColumn(OACFACTCOMPANYACTIVITY.COMPANY_ID), companyId));

        if (exists(factCompanyActivityTable, whereClause)) {
            final Collection<JsonObject> companyActivities = getRowsJson(factCompanyActivityTable, whereClause);

            for (final JsonObject companyActivityJson : companyActivities) {
                companyActivityJson.put(OACFACTCOMPANYACTIVITY.IS_DELETED, true);
                companyActivityJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCOMPANYACTIVITY.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, companyActivityJson));
            }
        }
    }
}
