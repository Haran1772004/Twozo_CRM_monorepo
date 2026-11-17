package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.sql.resource.OACDIMCONTACT;
import com.tlc.analytics.sql.resource.OACDIMDEALLINEITEMS;

import com.tlc.cdc.core.enrichment.EnrichedRecord;
import com.tlc.cdc.core.manager.CdcConnectorManager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.service.MetaHandleService;
import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.Range;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.resolver.TableResolver;

import com.tlc.sql.olap.api.dml.OlapTable;
import com.tlc.sql.olap.resolver.OlapTableResolver;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Abstract base class for enriching CRM entities with analytics data through Change Data Capture (CDC).
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
abstract class AbstractAnalyticsCdcEnricher implements AnalyticsCdcEnricher, SecureUserTaskProcessor {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractAnalyticsCdcEnricher.class);

    protected final FieldService fieldService;
    protected final UserService userService;
    protected final BaseCurrencyService baseCurrencyService;
    protected final MetaHandleService metaHandleService;
    protected final TableResolver tableResolver;
    protected final ContactManager contactManager;
    protected final CompanyManager companyManager;
    protected final DealManager dealManager;
    protected final SourceActivityManager sourceActivityManager;

    private final AdminDataStore readableDataStore;
    private final CdcConnectorManager cdcConnectorManager;

    protected AbstractAnalyticsCdcEnricher() {
        this.fieldService = FieldService.getInstance();
        this.userService = UserService.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.metaHandleService = MetaHandleService.getInstance();
        this.cdcConnectorManager = CdcConnectorManager.getInstance();
        this.tableResolver = OlapTableResolver.get();
        this.companyManager = CompanyManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.readableDataStore = DataStoreProvider.getAdminDataStore(DatabaseType.OLAP);
    }

    /**
     * Builds and enriches a JSON object with custom field values from a CRM entity for analytics consumption.
     *
     * <p>This method processes all persistent custom fields associated with the given source, groups them
     * by storage type, and populates the provided JSON object with their values. Each field's value is
     * extracted from the entity, formatted according to its type, and added to the appropriate field category.</p>
     *
     * @param model      The CRM entity containing the custom field values to extract.
     * @param jsonObject The JSON object to populate with field data (modified in-place).
     * @param source     The field source defining which custom fields to process.
     */
    protected final void buildCustomFields(final Entity model, final JsonObject jsonObject, final Source source) {
        final Collection<FieldInfo> persistentFieldsBySource = fieldService.getPersistentFieldsBySource(model.orgId(), source.getName());
        final Map<StoreType, List<FieldInfo>> fields = persistentFieldsBySource.stream().collect(Collectors.groupingBy(FieldInfo::storeType));
        final ValueHolder valueHolder = model.getValueHolder();
        final BiConsumer<String, JsonObject> consumer = (name, json) -> {
            if (json.size() > 0) {
                jsonObject.put(name, json);
            }
        };

        for (final Map.Entry<StoreType, List<FieldInfo>> groupedFields : fields.entrySet()) {

            switch (groupedFields.getKey()) {
                case SINGLE_STRING -> {
                    final JsonObject singleStringFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        final Object value = valueHolder.get(fieldInfo.id());

                        if (Objects.nonNull(value)) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, value);

                            singleStringFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.STRING_FIELDS, singleStringFieldsJson);
                }
                case TEXT -> {
                    final JsonObject textFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        final Object value = valueHolder.get(fieldInfo.id());

                        if (Objects.nonNull(value)) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, value);

                            textFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.TEXT_FIELDS, textFieldsJson);
                }
                case SINGLE_NUMBER -> {
                    final JsonObject numberFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        final Object value = valueHolder.get(fieldInfo.id());

                        if (Objects.nonNull(value)) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, value);

                            numberFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.NUMBER_FIELDS, numberFieldsJson);
                }
                case OWNER -> {
                    final JsonObject ownerFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final TlcModel tlcModel) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, tlcModel.id());

                            ownerFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.USER_FIELDS, ownerFieldsJson);
                }
                case COMPANY -> {
                    final JsonObject companyFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final TlcModel tlcModel) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, tlcModel.id());

                            companyFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.COMPANY_FIELDS, companyFieldsJson);
                }
                case CONTACT -> {
                    final JsonObject contactFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final TlcModel tlcModel) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, tlcModel.id());

                            contactFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.CONTACT_FIELDS, contactFieldsJson);
                }
                case DATE -> {
                    final JsonObject dateFieldsJson = Json.object();
                    final CrmUser crmUser = (CrmUser) LoggedInUserInfo.getUser();
                    TimezoneInfo timezoneInfo = TimezoneInfo.UTC;

                    if (crmUser != null && crmUser.getPreferenceHolder() != null) {
                        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder()
                                .get(UserRegionalPreference.TYPE);
                        if (userRegionalPreference != null) {
                            timezoneInfo = userRegionalPreference.getTimezoneInfo();
                        }
                    }

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Temporal temporal) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, TemporalServiceProvider.get(temporal.getTimeStamp()).formatUtcToDateTime(timezoneInfo, DATE_FORMATTER));

                            dateFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.DATE_FIELDS, dateFieldsJson);
                }
                case TIME -> {
                    final JsonObject timeFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Temporal temporal) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, TemporalServiceProvider.get(temporal.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

                            timeFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.TIME_FIELDS, timeFieldsJson);
                }
                case DATE_RANGE -> {
                    final JsonObject dateRangeFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Range<?> range) {
                            final JsonObject json = Json.object();
                            final JsonObject valueJson = Json.object();

                            valueJson.put(StoreSubType.FROM.getAliasName(), TemporalServiceProvider.get(range.getFrom().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                            valueJson.put(StoreSubType.TO.getAliasName(), TemporalServiceProvider.get(range.getTo().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, valueJson);

                            dateRangeFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.DATE_RANGE_FIELDS, dateRangeFieldsJson);
                }
                case TIME_RANGE -> {
                    final JsonObject timeRangeFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Range<?> range) {
                            final JsonObject json = Json.object();
                            final JsonObject valueJson = Json.object();

                            valueJson.put(StoreSubType.FROM.getAliasName(), TemporalServiceProvider.get(range.getFrom().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                            valueJson.put(StoreSubType.TO.getAliasName(), TemporalServiceProvider.get(range.getTo().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, valueJson);

                            timeRangeFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.TIME_RANGE_FIELDS, timeRangeFieldsJson);
                }
                case DATE_TIME_RANGE -> {
                    final JsonObject dateTimeRangeFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Range<?> range) {
                            final JsonObject json = Json.object();
                            final JsonObject valueJson = Json.object();

                            valueJson.put(StoreSubType.FROM.getAliasName(), TemporalServiceProvider.get(range.getFrom().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                            valueJson.put(StoreSubType.TO.getAliasName(), TemporalServiceProvider.get(range.getTo().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, valueJson);

                            dateTimeRangeFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.DATE_TIME_RANGE_FIELDS, dateTimeRangeFieldsJson);
                }
                case ADDRESS -> {
                    final JsonObject addressFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Address address) {
                            final JsonObject json = Json.object();
                            final JsonObject valueJson = Json.object();
                            final Country country = address.getCountry();

                            valueJson.put(StoreSubType.ADDRESS_LINE1.getAliasName(), address.getAddressLine1());
                            valueJson.put(StoreSubType.ADDRESS_LINE2.getAliasName(), address.getAddressLine2());
                            valueJson.put(StoreSubType.STATE.getAliasName(), address.getState());
                            valueJson.put(StoreSubType.CITY.getAliasName(), address.getCity());
                            valueJson.put(StoreSubType.ZIPCODE.getAliasName(), address.getZipcode());
                            valueJson.put(StoreSubType.LONGITUDE.getAliasName(), address.getLongitude());
                            valueJson.put(StoreSubType.LATITUDE.getAliasName(), address.getLatitude());

                            if (Objects.nonNull(country)) {
                                valueJson.put(StoreSubType.COUNTRY.getAliasName(), country.getName());
                            }

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, valueJson);

                            addressFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.ADDRESS_FIELDS, addressFieldsJson);
                }
                case EMAIL -> {
                    final JsonObject emailFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Email email) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, email.getValue());

                            emailFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.EMAIL_FIELDS, emailFieldsJson);
                }
                case PHONE -> {
                    final JsonObject phoneFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Phone phone) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, phone.getNumber());

                            phoneFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.PHONE_FIELDS, phoneFieldsJson);
                }
                case MONETARY -> {
                    final JsonObject monetaryFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Monetary monetary) {
                            final JsonObject json = Json.object();
                            final JsonObject valueJson = Json.object();

                            valueJson.put(StoreSubType.CURRENCY.getAliasName(), monetary.getCurrency().getCode());
                            valueJson.put(StoreSubType.VALUE.getAliasName(), monetary.getValue());

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, valueJson);

                            monetaryFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.MONETARY_FIELDS, monetaryFieldsJson);
                }
                case DROPDOWN -> {
                    final JsonObject dropdownFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Option option) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, option.getOption());

                            dropdownFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.DROPDOWN_FIELDS, dropdownFieldsJson);
                }
                case MULTI_SELECT -> {
                    final JsonObject multiselectFieldsJson = Json.object();

                    for (final FieldInfo fieldInfo : groupedFields.getValue()) {
                        if (valueHolder.get(fieldInfo.id()) instanceof final Collection<?> models) {
                            final JsonObject json = Json.object();

                            json.put(AnalyticsCdcKeys.FIELD_ID, fieldInfo.id());
                            json.put(AnalyticsCdcKeys.VALUE, models.stream().map(option -> (Option) option).map(Option::getOption).collect(Collectors.toSet()));

                            multiselectFieldsJson.put(String.valueOf(fieldInfo.columnIndex()), json);
                        }
                    }
                    consumer.accept(OACDIMCONTACT.MULTI_FIELDS, multiselectFieldsJson);
                }
            }
        }
    }

    /**
     * Retrieves a JSON representation of a database row for the specified table and ID.
     *
     * @param table The database table to query.
     * @param id    The unique identifier of the row to retrieve.
     * @return A JsonObject representing the column values of the retrieved row, with dates formatted as strings.
     */
    protected JsonObject getRowJson(final Table table, final long id) {
        return getRowJson(table, new WhereClause(Criteria.eq(table.getColumn(OACDIMCONTACT.ID), id)));
    }

    /**
     * Retrieves a JSON representation of a database row for the specified table and whereClause.
     *
     * @param table       The database table to query.
     * @param whereClause The {@code Whereclause}.
     * @return A JsonObject representing the column values of the retrieved row, with dates formatted as strings.
     */
    protected JsonObject getRowJson(final Table table, final WhereClause whereClause) {
        final SelectQuery cteQuery = SelectQuery.get(table);

        cteQuery.setWhereClause(whereClause);

        final DataContainer dataContainer = readableDataStore.get(cteQuery);
        final com.tlc.sql.common.api.Row row = dataContainer.getRow(table);
        final JsonObject jsonObject = Json.object();

        if(Objects.nonNull(row)) {
            row.forEach((key, value) -> {
                if (value instanceof final DateTime dateTime) {
                    jsonObject.put(key, TemporalServiceProvider.get(dateTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                } else if (value instanceof final Date date) {
                    jsonObject.put(key, TemporalServiceProvider.get(date.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                } else {
                    jsonObject.put(key, value);
                }
            });
        }

        return jsonObject;
    }

    /**
     * Retrieves a collection of JSON representation of a database rows for the specified table and whereClause.
     *
     * @param table       The database table to query.
     * @param whereClause The {@code Whereclause}.
     * @return A collection of {@code JsonObject} representing the column values of the retrieved row, with dates formatted as strings.
     */
    protected Collection<JsonObject> getRowsJson(final Table table, final WhereClause whereClause) {
        final SelectQuery cteQuery = SelectQuery.get(table);

        cteQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = readableDataStore.get(cteQuery);

        return dataContainer.getRows(table).map(row -> {
            final JsonObject jsonObject = Json.object();

            row.forEach((key, value) -> {
                if (value instanceof final DateTime dateTime) {
                    jsonObject.put(key, TemporalServiceProvider.get(dateTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                } else if (value instanceof final Date date) {
                    jsonObject.put(key, TemporalServiceProvider.get(date.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                } else {
                    jsonObject.put(key, value);
                }
            });

            return jsonObject;
        }).toList();
    }

    /**
     * Checks if a record with the specified ID exists in the given table.
     *
     * @param table The database table to query.
     * @param id    The unique identifier of the record to check for existence.
     * @return true if the record exists; false otherwise.
     */
    protected final boolean exists(final Table table, final long id) {
        return exists(table, new WhereClause(Criteria.eq(table.getColumn(OACDIMCONTACT.ID), id)));
    }

    /**
     * Checks if a record with the specified ID exists in the given table.
     *
     * @param table       The database table to query.
     * @param whereClause The {@code WhereClause}.
     * @return true if the record exists; false otherwise.
     */
    protected final boolean exists(final Table table, final WhereClause whereClause) {
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(whereClause);
        return readableDataStore.get(countQuery) > 0;
    }

    /**
     * Retrieves the module ID associated with the given set of table names by checking for known aliases.
     *
     * @param tables Set of table names to check for associated module IDs.
     * @return The module ID if a known alias is found; otherwise, null.
     */
    protected final Integer getModuleId(final Set<String> tables) {

        for (final String table : tables) {
            final String aliasName = cdcConnectorManager.getAliasName(table);

            if (Objects.nonNull(aliasName)) {
                return ModuleType.getModuleTypeByName(aliasName).getTypeCode();
            }
        }
        return null;
    }

    /**
     * Builds an activity summary JSON object for a CRM entity.
     *
     * @param orgId           The organization ID to which the entity belongs.
     * @param id              The unique identifier of the entity for which to build the activity summary.
     * @param table           The database table where the entity is stored.
     * @param entityType      The type of the entity (e.g., contact, company) for which the activity summary is being built.
     * @param enrichedRecords The collection to which the enriched activity summary record will be added.
     * @param transactionId   The transaction ID associated with the CDC event for tracking purposes.
     */
    protected final void buildActivitySummary(final Long orgId, final Long id, final Table table, final String entityType,
                                              final Collection<EnrichedRecord> enrichedRecords, final long transactionId, final Activity activity) {
        if (exists(table, id)) {
            final ActivitySummary activitySummary = sourceActivityManager.getActivitySummary(orgId, id, entityType);
            final JsonObject jsonObject = getRowJson(table, id);

            if (Objects.nonNull(activitySummary)) {
                final Date lastActivityDate = activitySummary.getLastActivityDate();
                final Date nextActivityDate = activitySummary.getNextActivityDate();

                if (Objects.nonNull(lastActivityDate)) {
                    jsonObject.put(OACDIMCONTACT.LAST_ACTIVITY_DATE, TemporalServiceProvider.get(lastActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                }

                if (Objects.nonNull(nextActivityDate)) {
                    jsonObject.put(OACDIMCONTACT.NEXT_ACTIVITY_DATE, TemporalServiceProvider.get(nextActivityDate.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_FORMATTER));
                }

                jsonObject.put(OACDIMCONTACT.LAST_ACTIVITY_TYPE, activitySummary.getLastActivityType());
                jsonObject.put(OACDIMCONTACT.DONE_ACTIVITIES, activitySummary.getDoneActivities());
                jsonObject.put(OACDIMCONTACT.UPCOMING_ACTIVITIES, activitySummary.getUpcomingActivities());
                jsonObject.put(OACDIMCONTACT.TOTAL_ACTIVITIES, activitySummary.getTotalActivities());
                jsonObject.put(OACDIMCONTACT.UPDATED_AT, TemporalServiceProvider.get(activity.getUpdatedTime().getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                jsonObject.put(AnalyticsCdcKeys.TOPIC, table.getName().toLowerCase());

                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Builds a deal statistics summary JSON object for a CRM entity.
     *
     * @param id              The unique identifier of the entity for which to build the deal statistics summary.
     * @param table           The database table where the entity is stored.
     * @param dealStatistics  The deal statistics data to be included in the summary.
     * @param enrichedRecords The collection to which the enriched deal statistics summary record will be added.
     * @param transactionId   The transaction ID associated with the CDC event for tracking purposes.
     */
    protected final void buildDealStatisticsSummary(final Long id, final Table table, final DealStatistics dealStatistics,
                                                    final Collection<EnrichedRecord> enrichedRecords, final long transactionId, final Deal deal) {
        if (exists(table, id)) {
            final JsonObject jsonObject = getRowJson(table, id);

            if (Objects.nonNull(dealStatistics)) {
                jsonObject.put(OACDIMCONTACT.OPEN_DEALS, dealStatistics.getOpenDealsCount());
                jsonObject.put(OACDIMCONTACT.CLOSED_DEALS, dealStatistics.getClosedDealsCount());
                jsonObject.put(OACDIMCONTACT.WON_DEALS, dealStatistics.getWonDealsCount());
                jsonObject.put(OACDIMCONTACT.LOST_DEALS, dealStatistics.getLostDealsCount());

                if (isDealValuesChanged(jsonObject, dealStatistics)) {
                    jsonObject.put(OACDIMCONTACT.UPDATED_AT, TemporalServiceProvider.get(deal.getModifiedTime().getTimeStamp())
                            .formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                } else {
                    jsonObject.put(OACDIMCONTACT.UPDATED_AT, TemporalServiceProvider.get(deal.getModifiedTime().getTimeStamp())
                            .formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                }

                jsonObject.put(AnalyticsCdcKeys.TOPIC, table.getName().toLowerCase());

                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Checks Deal Values are changed for current contact
     *
     * @param jsonObject      The JSON object representing the current state of the contact's deal statistics.
     * @param dealStatistics  The deal statistics data to be included in the summary.
     */
    private boolean isDealValuesChanged(final JsonObject jsonObject, final DealStatistics dealStatistics) {
        return (jsonObject.getLong(OACDIMCONTACT.OPEN_DEALS) != dealStatistics.getOpenDealsCount()) ||
                (jsonObject.getLong(OACDIMCONTACT.CLOSED_DEALS) != dealStatistics.getClosedDealsCount()) ||
                (jsonObject.getLong(OACDIMCONTACT.WON_DEALS) != dealStatistics.getWonDealsCount()) ||
                (jsonObject.getLong(OACDIMCONTACT.LOST_DEALS) != dealStatistics.getLostDealsCount());
    }

    /**
     * Retrieves the existing CREATED_AT value from OACDIMDEALLINEITEMS table in ClickHouse for the given deal ID and product ID.
     * Returns the formatted datetime string if found, or {@code null} if the record does not exist.
     *
     * @param dealId    the deal ID.
     * @param productId the product ID.
     * @return the CREATED_AT datetime string (formatted as "yyyy-MM-dd HH:mm:ss"), or {@code null} if not found.
     */
    protected final String getExistingDealLineItemCreatedAt(final long dealId, final long productId) {
        try {
            final Table table = OlapTable.get(OACDIMDEALLINEITEMS.TABLE);
            final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OACDIMDEALLINEITEMS.DEAL_ID), dealId))
                    .and(Criteria.eq(table.getColumn(OACDIMDEALLINEITEMS.PRODUCT_ID), productId));
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.setWhereClause(whereClause);

            final DataContainer dataContainer = readableDataStore.get(selectQuery);
            final Row row = dataContainer.getRow(table);

            if (Objects.isNull(row) || Objects.isNull(row.get(OACDIMDEALLINEITEMS.CREATED_AT))) {
                return null;
            } else {
                final Object createdAt = dataContainer.getRow(table).get(OACDIMDEALLINEITEMS.CREATED_AT);

                if (createdAt instanceof final DateTime dateTime) {
                    return TemporalServiceProvider.get(dateTime.getTimeStamp())
                            .formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER);
                } else if (Objects.nonNull(createdAt)) {
                    return createdAt.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.info("Analytics CDC: Failed to fetch CREATED_AT for DEAL_ID: {}, PRODUCT_ID: {}", dealId, productId, e);
        }

        return null;
    }
}
