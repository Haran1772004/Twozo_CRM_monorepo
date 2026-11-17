package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.sql.resource.OACDIMACTIVITY;
import com.tlc.analytics.sql.resource.OACDIMCOMPANY;
import com.tlc.analytics.sql.resource.OACDIMCONTACT;
import com.tlc.analytics.sql.resource.OACDIMDEAL;
import com.tlc.analytics.sql.resource.OACFACTCOMPANYACTIVITY;
import com.tlc.analytics.sql.resource.OACFACTCONTACTACTIVITY;
import com.tlc.analytics.sql.resource.OACFACTDEALACTIVITY;

import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Table;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Enriches CRM activity CDC events with analytics specific data.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AnalyticsCrmActivityEnricher extends AbstractAnalyticsCdcEnricher {

    private final SourceActivityManager sourceActivityManager;

    private AnalyticsCrmActivityEnricher() {
        this.sourceActivityManager = SourceActivityManager.getInstance();
    }

    private static class Instance {
        private static final AnalyticsCrmActivityEnricher ACTIVITY_ENRICHER = new AnalyticsCrmActivityEnricher();
    }

    static AnalyticsCrmActivityEnricher getInstance() {
        return Instance.ACTIVITY_ENRICHER;
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
                case OACDIMACTIVITY.TABLE -> loadDimensionActivity(chTableDetails, groupedRecords, enrichedRecords);
            }
        }

        return enrichedRecords;
    }

    /**
     * Loads dimension activity data from the given JSON objects into enriched records.
     *
     * @param chTableDetails  ClickHouse table details for dimension activity.
     * @param groupedRecords  Map of JSON objects containing activity data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void loadDimensionActivity(final TableDetail chTableDetails, final Map<String, Collection<JsonObject>> groupedRecords, final Collection<EnrichedRecord> enrichedRecords) {
        final Collection<JsonObject> records = groupedRecords.get(OMACACTIVITY.TABLE.toLowerCase());

        if (Objects.nonNull(records) && !records.isEmpty()) {

            for (final JsonObject activityJson : records) {
                final String operation = activityJson.getString(AnalyticsCdcKeys.OPERATION);
                final long transactionId = activityJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID);
                final boolean isDeleteEvent = Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, operation);
                final JsonObject jsonObject = isDeleteEvent ? getRowJson(tableResolver.resolve(OACDIMACTIVITY.TABLE), activityJson.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(OMACACTIVITY.ID)) : Json.object();

                if (isDeleteEvent) {
                    final long activityId = jsonObject.getLong(OACDIMACTIVITY.ID);

                    jsonObject.put(OACDIMACTIVITY.UPDATED_AT, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                    jsonObject.put(OACDIMACTIVITY.IS_DELETED, true);

                    deleteContactActivity(activityId, enrichedRecords, transactionId);
                    deleteCompanyActivity(activityId, enrichedRecords, transactionId);
                    deleteDealActivity(activityId, enrichedRecords, transactionId);
                } else {
                    final JsonObject cdcJson = activityJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                    final long orgId = cdcJson.getLong(OMACACTIVITY.ORG_ID);

                    secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
                        final ActivityHolder activityHolder = sourceActivityManager.get(orgId, cdcJson.getLong(OMACACTIVITY.ID));
                        final Activity activity = activityHolder.getActivity();
                        final Long activityId = activity.id();

                        for (final ColumnDetail chColumnDetails : metaHandleService.getColumnDetails(chTableDetails.id())) {
                            final String columnName = chColumnDetails.getColumnName();

                            switch (columnName) {
                                case OACDIMACTIVITY.ID -> jsonObject.put(columnName, cdcJson.getLong(OMACACTIVITY.ID));
                                case OACDIMACTIVITY.ORG_ID -> jsonObject.put(columnName, orgId);
                                case OACDIMACTIVITY.TITLE ->
                                        jsonObject.put(columnName, cdcJson.getString(OMACACTIVITY.TITLE));
                                case OACDIMACTIVITY.STATUS -> {
                                    final Status status = activity.getStatus();

                                    if (Objects.nonNull(status)) {
                                        jsonObject.put(columnName, status.getDisplayName());
                                    }
                                }
                                case OACDIMACTIVITY.ASSIGN_TO_USER ->
                                        jsonObject.put(columnName, cdcJson.getLong(OMACACTIVITY.ASSIGNEE_ID));
                                case OACDIMACTIVITY.DEAL -> {
                                    final Deal deal = activityHolder.getDeal();

                                    if (Objects.nonNull(deal)) {
                                        jsonObject.put(columnName, deal.id());
                                        buildFactActivityDeal(activity, deal, enrichedRecords, transactionId);
                                        buildActivitySummary(orgId, deal.id(), tableResolver.resolve(OACDIMDEAL.TABLE), Deal.ENTITY_TYPE, enrichedRecords, transactionId, activity);
                                    } else {
                                        jsonObject.put(columnName, deal);
                                        deleteDealActivity(activityId, enrichedRecords, transactionId);
                                    }
                                }
                                case OACDIMACTIVITY.CONTACT -> {
                                    final List<Contact> contacts = activityHolder.getContacts();

                                    if (Objects.nonNull(contacts) && !contacts.isEmpty()) {
                                        jsonObject.put(columnName, contacts.stream().map(Contact::id).toList());
                                        buildFactActivityContacts(activity, contacts, enrichedRecords, transactionId);
                                        buildContactActivitySummary(activity, contacts, enrichedRecords, transactionId);
                                    } else {
                                        jsonObject.put(columnName, contacts);
                                        deleteContactActivity(activityId, enrichedRecords, transactionId);
                                    }
                                }
                                case OACDIMACTIVITY.COMPANY -> {
                                    final Company company = activityHolder.getCompany();

                                    if (Objects.nonNull(company)) {
                                        jsonObject.put(columnName, company.id());
                                        buildFactActivityCompany(activity, company, enrichedRecords, transactionId);
                                        buildActivitySummary(orgId, company.id(), tableResolver.resolve(OACDIMCOMPANY.TABLE), Company.ENTITY_TYPE, enrichedRecords, transactionId, activity);
                                    } else {
                                        jsonObject.put(columnName, company);
                                        deleteCompanyActivity(activityId, enrichedRecords, transactionId);
                                    }
                                }
                                case OACDIMACTIVITY.MARK_AS_DONE -> {
                                    final DateTime completedTime = activity.getCompletedTime();

                                    if (Objects.nonNull(completedTime)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(completedTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMACTIVITY.AVAILABILITY -> {
                                    final Availability availability = activity.getAvailability();

                                    if (Objects.nonNull(availability)) {
                                        jsonObject.put(columnName, availability.getDisplayName());
                                    }
                                }
                                case OACDIMACTIVITY.CREATOR ->
                                        jsonObject.put(columnName, cdcJson.getLong(OMACACTIVITY.CREATOR_ID));
                                case OACDIMACTIVITY.LOCATION ->
                                        jsonObject.put(columnName, cdcJson.optString(OMACACTIVITY.LOCATION, null));
                                case OACDIMACTIVITY.TYPE -> {
                                    final ActivityType activityType = activity.getType();

                                    if (Objects.nonNull(activityType)) {
                                        jsonObject.put(columnName, activityType.getName());
                                    }
                                }
                                case OACDIMACTIVITY.DESCRIPTION ->
                                        jsonObject.put(columnName, cdcJson.optString(OMACACTIVITY.DESCRIPTION, null));
                                case OACDIMACTIVITY.CREATED_AT -> {
                                    final DateTime createdTime = activity.getCreatedTime();
                                    jsonObject.put(columnName, TemporalServiceProvider.get(createdTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                }
                                case OACDIMACTIVITY.UPDATED_AT -> {
                                    final DateTime updatedTime = activity.getUpdatedTime();
                                    jsonObject.put(columnName, TemporalServiceProvider.get(updatedTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                }
                                case OACDIMACTIVITY.COLLABORATORS ->
                                        jsonObject.put(columnName, activity.getCollaboratorIds());
                                case OACDIMACTIVITY.PRIORITY -> {
                                    final Priority priority = activity.getPriority();

                                    if (Objects.nonNull(priority)) {
                                        jsonObject.put(columnName, priority.getDisplayName());
                                    }
                                }
                                case OACDIMACTIVITY.START_DATE_TIME -> {
                                    final DateTime startTime = activity.getStartTime();

                                    if (Objects.nonNull(startTime)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(startTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMACTIVITY.END_DATE_TIME -> {
                                    final DateTime endTime = activity.getEndTime();

                                    if (Objects.nonNull(endTime)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(endTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMACTIVITY.IS_DELETED -> jsonObject.put(columnName, false);
                            }
                        }
                    });
                }

                jsonObject.put(AnalyticsCdcKeys.TOPIC, chTableDetails.getTableName().toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Deletes associated activity records in fact tables for the given activity ID and adds the deleted records to enriched records collection.
     *
     * @param activityId      The ID of the activity being deleted.
     * @param enrichedRecords Collection of enriched records to be updated with deleted records.
     * @param transactionId   Transaction identifier for the delete operation.
     */
    private void deleteCompanyActivity(final long activityId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table factCompanyActivityTable = tableResolver.resolve(OACFACTCOMPANYACTIVITY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(factCompanyActivityTable.getColumn(OACFACTCOMPANYACTIVITY.ACTIVITY_ID), activityId));

        if (exists(factCompanyActivityTable, whereClause)) {
            final JsonObject companyActivityJson = getRowJson(factCompanyActivityTable, whereClause);

            companyActivityJson.put(OACFACTCOMPANYACTIVITY.IS_DELETED, true);
            companyActivityJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCOMPANYACTIVITY.TABLE.toLowerCase());
            enrichedRecords.add(new EnrichedRecord(transactionId, companyActivityJson));
        }
    }

    /**
     * Deletes associated activity records in fact deal activity table for the given activity ID and adds the deleted records to enriched records collection.
     *
     * @param activityId      The ID of the activity being deleted.
     * @param enrichedRecords Collection of enriched records to be updated with deleted records.
     * @param transactionId   Transaction identifier for the delete operation.
     */
    private void deleteDealActivity(final long activityId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table factDealActivityTable = tableResolver.resolve(OACFACTDEALACTIVITY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(factDealActivityTable.getColumn(OACFACTDEALACTIVITY.ACTIVITY_ID), activityId));

        if (exists(factDealActivityTable, whereClause)) {
            final JsonObject dealActivityJson = getRowJson(factDealActivityTable, whereClause);

            dealActivityJson.put(OACFACTDEALACTIVITY.IS_DELETED, true);
            dealActivityJson.put(AnalyticsCdcKeys.TOPIC, OACFACTDEALACTIVITY.TABLE.toLowerCase());
            enrichedRecords.add(new EnrichedRecord(transactionId, dealActivityJson));
        }
    }

    /**
     * Deletes associated activity records in fact contact activity table for the given activity ID and adds the deleted records to enriched records collection.
     *
     * @param activityId      The ID of the activity being deleted.
     * @param enrichedRecords Collection of enriched records to be updated with deleted records.
     * @param transactionId   Transaction identifier for the delete operation.
     */
    private void deleteContactActivity(final long activityId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table factContactActivityTable = tableResolver.resolve(OACFACTCONTACTACTIVITY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(factContactActivityTable.getColumn(OACFACTCONTACTACTIVITY.ACTIVITY_ID), activityId));

        if (exists(factContactActivityTable, whereClause)) {
            final Collection<JsonObject> contactActivities = getRowsJson(factContactActivityTable, whereClause);

            for (final JsonObject contactActivityJson : contactActivities) {
                contactActivityJson.put(OACFACTCONTACTACTIVITY.IS_DELETED, true);
                contactActivityJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTACTIVITY.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, contactActivityJson));
            }
        }
    }

    /**
     * Builds activity summary records for associated contacts of the given activity.
     *
     * @param activity        Activity entity used for building the summary records.
     * @param contacts        List of contact entities associated with the activity.
     * @param enrichedRecords Collection of enriched records to be updated.
     * @param transactionId   Transaction identifier for the activity.
     */
    private void buildContactActivitySummary(final Activity activity, final List<Contact> contacts,
                                             final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final long orgId = activity.orgId();
        final Table table = tableResolver.resolve(OACDIMCONTACT.TABLE);

        for (final Contact contact : contacts) {
            buildActivitySummary(orgId, contact.id(), table, Contact.ENTITY_TYPE, enrichedRecords, transactionId, activity);
        }
    }

    /**
     * Builds fact activity company record using the given activity and company details.
     *
     * @param activity        Activity entity used for building the record.
     * @param company         Company entity associated with the activity.
     * @param enrichedRecords Collection of enriched records to be updated.
     * @param transactionId   Transaction identifier for the activity.
     */
    private void buildFactActivityCompany(final Activity activity, final Company company, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(OACFACTCOMPANYACTIVITY.ORG_ID, activity.orgId());
        jsonObject.put(OACFACTCOMPANYACTIVITY.ACTIVITY_ID, activity.id());
        jsonObject.put(OACFACTCOMPANYACTIVITY.COMPANY_ID, company.id());
        jsonObject.put(OACFACTCOMPANYACTIVITY.IS_DELETED, false);
        jsonObject.put(OACFACTCOMPANYACTIVITY.STATUS, activity.getStatus().getId());
        jsonObject.put(OACFACTCOMPANYACTIVITY.TIMESTAMP, TemporalServiceProvider.get(activity.getUpdatedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

        jsonObject.put(AnalyticsCdcKeys.TOPIC, OACFACTCOMPANYACTIVITY.TABLE.toLowerCase());
        enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
    }

    /**
     * Builds fact activity contact records using the given activity and contact details.
     *
     * @param activity        Activity entity used for building the record.
     * @param contacts        List of contact entities associated with the activity.
     * @param enrichedRecords Collection of enriched records to be updated.
     * @param transactionId   Transaction identifier for the activity.
     */
    private void buildFactActivityContacts(final Activity activity, final List<Contact> contacts, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        for (final Contact contact : contacts) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(OACFACTCONTACTACTIVITY.ORG_ID, activity.orgId());
            jsonObject.put(OACFACTCONTACTACTIVITY.ACTIVITY_ID, activity.id());
            jsonObject.put(OACFACTCONTACTACTIVITY.CONTACT_ID, contact.id());
            jsonObject.put(OACFACTCONTACTACTIVITY.IS_DELETED, false);
            jsonObject.put(OACFACTCONTACTACTIVITY.STATUS, activity.getStatus().getId());
            jsonObject.put(OACFACTCONTACTACTIVITY.TIMESTAMP, TemporalServiceProvider.get(activity.getUpdatedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

            jsonObject.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTACTIVITY.TABLE.toLowerCase());
            enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
        }
    }

    /**
     * Builds fact activity deal record using the given activity and deal details.
     *
     * @param activity        Activity entity used for building the record.
     * @param deal            Deal entity associated with the activity.
     * @param enrichedRecords Collection of enriched records to be updated.
     * @param transactionId   Transaction identifier for the activity.
     */
    private void buildFactActivityDeal(final Activity activity, final Deal deal, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(OACFACTDEALACTIVITY.ORG_ID, activity.orgId());
        jsonObject.put(OACFACTDEALACTIVITY.ACTIVITY_ID, activity.id());
        jsonObject.put(OACFACTDEALACTIVITY.DEAL_ID, deal.id());
        jsonObject.put(OACFACTDEALACTIVITY.IS_DELETED, false);
        jsonObject.put(OACFACTDEALACTIVITY.STATUS, activity.getStatus().getId());
        jsonObject.put(OACFACTDEALACTIVITY.TIMESTAMP, TemporalServiceProvider.get(activity.getUpdatedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

        jsonObject.put(AnalyticsCdcKeys.TOPIC, OACFACTDEALACTIVITY.TABLE.toLowerCase());
        enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
    }
}
