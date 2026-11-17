package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.sql.resource.OACDIMCONTACT;
import com.tlc.analytics.sql.resource.OACFACTCONTACTACTIVITY;
import com.tlc.analytics.sql.resource.OACFACTCONTACTDEAL;
import com.tlc.analytics.sql.resource.OACFACTCONTACTCOMPANY;

import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;

import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.sql.resource.MCOCONTACT;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete implementation of analytics enricher for Contact entities.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AnalyticsCrmContactEnricher extends AbstractAnalyticsCdcEnricher {

    private final ContactManager contactManager;

    private AnalyticsCrmContactEnricher() {
        this.contactManager = ContactManager.getInstance();
    }

    private static class Instance {
        private static final AnalyticsCrmContactEnricher INSTANCE = new AnalyticsCrmContactEnricher();
    }

    static AnalyticsCrmContactEnricher getInstance() {
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
                case OACDIMCONTACT.TABLE -> loadDimensionContact(chTableDetails, groupedRecords, enrichedRecords);
            }
        }

        return enrichedRecords;
    }

    /**
     * Loads dimension contact data from the given JSON objects into enriched records.
     *
     * @param chTableDetails  ClickHouse table details for dimension contact.
     * @param groupedRecords  Map of JSON objects containing contact data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void loadDimensionContact(final TableDetail chTableDetails, final Map<String, Collection<JsonObject>> groupedRecords, final Collection<EnrichedRecord> enrichedRecords) {
        final Collection<JsonObject> records = groupedRecords.get(MCOCONTACT.TABLE.toLowerCase());

        if (Objects.nonNull(records) && !records.isEmpty()) {

            for (final JsonObject contactJson : records) {
                final String operation = contactJson.getString(AnalyticsCdcKeys.OPERATION);
                final long transactionId = contactJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID);
                final boolean isDeleteEvent = Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, operation);
                final JsonObject jsonObject = isDeleteEvent ? getRowJson(tableResolver.resolve(OACDIMCONTACT.TABLE), contactJson.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(MCOCONTACT.ID)) : Json.object();

                if (isDeleteEvent) {
                    final long contactId = jsonObject.getLong(OACDIMCONTACT.ID);

                    jsonObject.put(OACDIMCONTACT.UPDATED_AT, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                    jsonObject.put(OACDIMCONTACT.IS_DELETED, true);

                    deleteContactCompany(contactId, enrichedRecords, transactionId);
                    deleteContactDeal(contactId, enrichedRecords, transactionId);
                    deleteContactActivity(contactId, enrichedRecords, transactionId);
                } else {
                    final JsonObject cdcJson = contactJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                    final long orgId = cdcJson.getLong(MCOCONTACT.ORG_ID);

                    secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
                        final Contact contact = contactManager.get(orgId, cdcJson.getLong(MCOCONTACT.ID));

                        contactManager.loadSummaryDetails(orgId, contact);

                        for (final ColumnDetail chColumnDetails : metaHandleService.getColumnDetails(chTableDetails.id())) {
                            final String columnName = chColumnDetails.getColumnName();

                            switch (columnName) {
                                case OACDIMCONTACT.ID -> jsonObject.put(columnName, cdcJson.getLong(MCOCONTACT.ID));
                                case OACDIMCONTACT.ORG_ID -> jsonObject.put(columnName, orgId);
                                case OACDIMCONTACT.FIRST_NAME ->
                                        jsonObject.put(columnName, cdcJson.getString(MCOCONTACT.FIRST_NAME));
                                case OACDIMCONTACT.LAST_NAME ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCONTACT.LAST_NAME, null));
                                case OACDIMCONTACT.FULL_NAME -> {
                                    final String firstName = cdcJson.getString(MCOCONTACT.FIRST_NAME);
                                    final String lastName = cdcJson.optString(MCOCONTACT.LAST_NAME, null);

                                    jsonObject.put(columnName, (Objects.nonNull(lastName) && !lastName.isBlank())
                                            ? firstName + " " + lastName : firstName);
                                }
                                case OACDIMCONTACT.EMAIL -> {
                                    final Email primaryEmail = contact.getPrimaryEmail();

                                    if (Objects.nonNull(primaryEmail)) {
                                        jsonObject.put(columnName, primaryEmail.getValue());
                                    }
                                }
                                case OACDIMCONTACT.PHONE -> {
                                    final Phone primaryPhone = contact.getPrimaryPhone();

                                    if (Objects.nonNull(primaryPhone)) {
                                        jsonObject.put(columnName, primaryPhone.getNumber());
                                    }
                                }
                                case OACDIMCONTACT.SALES_OWNER ->
                                        jsonObject.put(columnName, cdcJson.getLong(MCOCONTACT.OWNER_ID));
                                case OACDIMCONTACT.COMPANY -> {
                                    final Company company = contact.getCompany();

                                    if (Objects.nonNull(company)) {
                                        jsonObject.put(columnName, company.id());
                                        loadFactContactCompany(contact, company, enrichedRecords, transactionId);
                                    } else {
                                        jsonObject.put(columnName, company);
                                        deleteContactCompany(contact.id(), enrichedRecords, transactionId);
                                    }
                                }
                                case OACDIMCONTACT.LIFE_CYCLE_STAGE -> {
                                    final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();

                                    if (Objects.nonNull(lifeCycleStage)) {
                                        jsonObject.put(columnName, lifeCycleStage.getName());
                                    }
                                }
                                case OACDIMCONTACT.LIFE_CYCLE_STATUS -> {
                                    final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();

                                    if (Objects.nonNull(lifeCycleStatus)) {
                                        jsonObject.put(columnName, lifeCycleStatus.getName());
                                    }
                                }
                                case OACDIMCONTACT.LOST_REASON -> {
                                    final Option lostReason = contact.getLostReason();

                                    if (Objects.nonNull(lostReason)) {
                                        jsonObject.put(columnName, lostReason.getOption());
                                    }
                                }
                                case OACDIMCONTACT.TERRITORY ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCONTACT.TERRITORY_ID, null));
                                case OACDIMCONTACT.TAGS -> {
                                    final SummaryInfo companySummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(companySummary)) {
                                        final Collection<Tag> tags = companySummary.getTags();

                                        if (Objects.nonNull(tags) && !tags.isEmpty()) {
                                            jsonObject.put(columnName, tags.stream().map(Tag::getName).toList());
                                        }
                                    }
                                }
                                case OACDIMCONTACT.SUBSCRIPTION_STATUS -> {
                                    final Option subscriptionStatus = contact.getSubscriptionStatus();

                                    if (Objects.nonNull(subscriptionStatus)) {
                                        jsonObject.put(columnName, subscriptionStatus.getOption());
                                    }
                                }
                                case OACDIMCONTACT.SUBSCRIPTION_TYPES -> {
                                    final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();

                                    if (Objects.nonNull(subscriptionTypes) && !subscriptionTypes.isEmpty()) {
                                        jsonObject.put(columnName, subscriptionTypes.stream().map(Option::getOption).toList());
                                    }
                                }
                                case OACDIMCONTACT.UNSUBSCRIBE_REASON -> {
                                    final Option unsubscribeReason = contact.getUnsubscribeReason();

                                    if (Objects.nonNull(unsubscribeReason)) {
                                        jsonObject.put(columnName, unsubscribeReason.getOption());
                                    }
                                }
                                case OACDIMCONTACT.OTHER_UNSUBSCRIBE_REASON ->
                                        jsonObject.put(columnName, contact.getOtherUnsubscribeReason());
                                case OACDIMCONTACT.SOURCE -> {
                                    final Option source = contact.getContactSource();

                                    if (Objects.nonNull(source)) {
                                        jsonObject.put(columnName, source.getOption());
                                    }
                                }
                                case OACDIMCONTACT.DESIGNATION ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCONTACT.DESIGNATION, null));
                                case OACDIMCONTACT.CREATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MCOCONTACT.CREATED_BY));
                                case OACDIMCONTACT.CREATED_AT ->
                                        jsonObject.put(columnName, TemporalServiceProvider.get(contact.getCreatedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                case OACDIMCONTACT.UPDATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MCOCONTACT.UPDATED_BY));
                                case OACDIMCONTACT.UPDATED_AT ->
                                        jsonObject.put(columnName, TemporalServiceProvider.get(contact.getModifiedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                case OACDIMCONTACT.LAST_ASSIGNED_AT -> {
                                    final DateTime lastAssignedAt = contact.getLastAssignedAt();

                                    if (Objects.nonNull(lastAssignedAt)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(lastAssignedAt.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMCONTACT.DATE_OF_BIRTH -> {
                                    final Date dateOfBirth = contact.getDateOfBirth();
                                    final CrmUser crmUser = (CrmUser) LoggedInUserInfo.getUser();
                                    TimezoneInfo timezoneInfo = TimezoneInfo.UTC;

                                    if (crmUser != null && crmUser.getPreferenceHolder() != null) {
                                        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder()
                                                .get(UserRegionalPreference.TYPE);
                                        if (userRegionalPreference != null) {
                                            timezoneInfo = userRegionalPreference.getTimezoneInfo();
                                        }
                                    }

                                    if (Objects.nonNull(dateOfBirth)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(dateOfBirth.getTimeStamp()).formatUtcToDateTime(timezoneInfo, DATE_FORMATTER));
                                    }
                                }
                                case OACDIMCONTACT.DEPARTMENT ->
                                        jsonObject.put(columnName, cdcJson.optString(MCOCONTACT.DEPARTMENT, null));
                                case OACDIMCONTACT.TIMEZONE -> {
                                    final Option timeZone = contact.getTimeZone();

                                    if (Objects.nonNull(timeZone)) {
                                        jsonObject.put(columnName, timeZone.getOption());
                                    }
                                }
                                case OACDIMCONTACT.WEBFORM -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        jsonObject.put(columnName, contactSummary.getWebFormName());
                                    }
                                }
                                case OACDIMCONTACT.OPEN_DEALS -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getOpenDealsCount());
                                    }
                                }
                                case OACDIMCONTACT.CLOSED_DEALS -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getClosedDealsCount());
                                    }
                                }
                                case OACDIMCONTACT.WON_DEALS -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getWonDealsCount());
                                    }
                                }
                                case OACDIMCONTACT.LOST_DEALS -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final DealStatistics dealStatistics = contactSummary.getDealStatistics();

                                        jsonObject.put(columnName, Objects.isNull(dealStatistics) ? 0 : dealStatistics.getLostDealsCount());
                                    }
                                }
                                case OACDIMCONTACT.LAST_ACTIVITY_DATE -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                                        if (Objects.nonNull(activitySummary)) {
                                            final Date lastActivityDate = activitySummary.getLastActivityDate();

                                            if (Objects.nonNull(lastActivityDate)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(lastActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCONTACT.NEXT_ACTIVITY_DATE -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                                        if (Objects.nonNull(activitySummary)) {
                                            final Date nextActivityDate = activitySummary.getNextActivityDate();

                                            if (Objects.nonNull(nextActivityDate)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(nextActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCONTACT.LAST_ACTIVITY_TYPE -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? null : activitySummary.getLastActivityType());
                                    }
                                }
                                case OACDIMCONTACT.DONE_ACTIVITIES -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getDoneActivities());
                                    }
                                }
                                case OACDIMCONTACT.UPCOMING_ACTIVITIES -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getUpcomingActivities());
                                    }
                                }
                                case OACDIMCONTACT.TOTAL_ACTIVITIES -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final ActivitySummary activitySummary = contactSummary.getActivitySummary();

                                        jsonObject.put(columnName, Objects.isNull(activitySummary) ? 0 : activitySummary.getTotalActivities());
                                    }
                                }
                                case OACDIMCONTACT.EMAIL_MESSAGE_COUNT -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final EmailSummary emailSummary = contactSummary.getEmailSummary();

                                        jsonObject.put(columnName, Objects.isNull(emailSummary) ? 0 : emailSummary.getEmailMessageCount());
                                    }
                                }
                                case OACDIMCONTACT.LAST_EMAIL_RECEIVED -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final EmailSummary emailSummary = contactSummary.getEmailSummary();

                                        if (Objects.nonNull(emailSummary)) {
                                            final Long timestamp = emailSummary.getLastEmailReceived().getTimeStamp();

                                            if (Objects.nonNull(timestamp)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(timestamp).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCONTACT.LAST_EMAIL_SENT -> {
                                    final SummaryInfo contactSummary = contact.getSummaryInfo();

                                    if (Objects.nonNull(contactSummary)) {
                                        final EmailSummary emailSummary = contactSummary.getEmailSummary();

                                        if (Objects.nonNull(emailSummary)) {
                                            final Long timestamp = emailSummary.getLastEmailSent().getTimeStamp();

                                            if (Objects.nonNull(timestamp)) {
                                                jsonObject.put(columnName, TemporalServiceProvider.get(timestamp).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                            }
                                        }
                                    }
                                }
                                case OACDIMCONTACT.CITY -> {
                                    final Address address = contact.getAddress();

                                    if (Objects.nonNull(address)) {
                                        jsonObject.put(columnName, address.getCity());
                                    }
                                }
                                case OACDIMCONTACT.STATE -> {
                                    final Address address = contact.getAddress();

                                    if (Objects.nonNull(address)) {
                                        jsonObject.put(columnName, address.getState());
                                    }
                                }
                                case OACDIMCONTACT.COUNTRY -> {
                                    final Address address = contact.getAddress();

                                    if (Objects.nonNull(address)) {
                                        final Country country = address.getCountry();

                                        if (Objects.nonNull(country)) {
                                            jsonObject.put(columnName, address.getCountry().getName());
                                        }
                                    }
                                }
                                case OACDIMCONTACT.PIN_CODE -> {
                                    final Address address = contact.getAddress();

                                    if (Objects.nonNull(address)) {
                                        jsonObject.put(columnName, address.getZipcode());
                                    }
                                }
                                case OACDIMCONTACT.IS_DELETED -> jsonObject.put(columnName, false);
                            }
                        }
                        buildCustomFields(contact, jsonObject, Source.CONTACT);
                    });
                }

                jsonObject.put(AnalyticsCdcKeys.TOPIC, chTableDetails.getTableName().toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Loads fact contact company record using the given contact and company details.
     *
     * @param contact         Contact entity associated with the company.
     * @param company         Company entity associated with the contact.
     * @param enrichedRecords Collection of enriched records to be updated.
     * @param transactionId   Transaction identifier for the contact.
     */
    private void loadFactContactCompany(final Contact contact, final Company company, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(OACFACTCONTACTCOMPANY.ORG_ID, company.orgId());
        jsonObject.put(OACFACTCONTACTCOMPANY.CONTACT_ID, contact.id());
        jsonObject.put(OACFACTCONTACTCOMPANY.COMPANY_ID, company.id());
        jsonObject.put(OACFACTCONTACTCOMPANY.IS_DELETED, false);
        jsonObject.put(OACFACTCONTACTCOMPANY.TIMESTAMP, TemporalServiceProvider.get(contact.getModifiedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

        jsonObject.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTCOMPANY.TABLE.toLowerCase());
        enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
    }

    /**
     * Deletes fact contact company records associated with the given contact identifier.
     *
     * @param contactId       Identifier of the contact whose associated company records are to be deleted.
     * @param enrichedRecords Collection of enriched records to be updated with delete events.
     * @param transactionId   Transaction identifier for the delete events.
     */
    private void deleteContactCompany(final long contactId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table contactCompanyTable = tableResolver.resolve(OACFACTCONTACTCOMPANY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(contactCompanyTable.getColumn(OACFACTCONTACTCOMPANY.CONTACT_ID), contactId));

        if (exists(contactCompanyTable, whereClause)) {
            final Collection<JsonObject> contactCompanies = getRowsJson(contactCompanyTable, whereClause);

            for (final JsonObject contactCompanyJson : contactCompanies) {
                contactCompanyJson.put(OACFACTCONTACTCOMPANY.IS_DELETED, true);
                contactCompanyJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTCOMPANY.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, contactCompanyJson));
            }
        }
    }

    /**
     * Deletes fact contact deal records associated with the given contact identifier.
     *
     * @param contactId       Identifier of the contact whose associated deal records are to be deleted.
     * @param enrichedRecords Collection of enriched records to be updated with delete events.
     * @param transactionId   Transaction identifier for the delete events.
     */
    private void deleteContactDeal(final long contactId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table contactDealTable = tableResolver.resolve(OACFACTCONTACTDEAL.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(contactDealTable.getColumn(OACFACTCONTACTDEAL.CONTACT_ID), contactId));

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
     * Deletes fact contact activity records associated with the given contact identifier.
     *
     * @param contactId       Identifier of the contact whose associated activity records are to be deleted.
     * @param enrichedRecords Collection of enriched records to be updated with delete events.
     * @param transactionId   Transaction identifier for the delete events.
     */
    private void deleteContactActivity(final long contactId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table factContactActivityTable = tableResolver.resolve(OACFACTCONTACTACTIVITY.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(factContactActivityTable.getColumn(OACFACTCONTACTACTIVITY.CONTACT_ID), contactId));

        if (exists(factContactActivityTable, whereClause)) {
            final Collection<JsonObject> contactActivities = getRowsJson(factContactActivityTable, whereClause);

            for (final JsonObject contactActivityJson : contactActivities) {
                contactActivityJson.put(OACFACTCONTACTACTIVITY.IS_DELETED, true);
                contactActivityJson.put(AnalyticsCdcKeys.TOPIC, OACFACTCONTACTACTIVITY.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, contactActivityJson));
            }
        }
    }
}
