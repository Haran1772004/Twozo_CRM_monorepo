package com.tlc.crm.core.features.api.quota.manager.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.manager.QuotaListViewUserFilterManager;
import com.tlc.crm.core.features.api.quota.model.ForecastType;
import com.tlc.crm.core.features.api.quota.model.QuotaDateInterval;
import com.tlc.crm.core.features.api.quota.model.QuotaListViewUserFilter;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.sql.resource.OMQFLISTVIEWUSERFILTER;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.l10n.temporal.interval.DateInterval.MONTHS;
import static com.tlc.l10n.temporal.interval.DateInterval.QUARTERS;
import static com.tlc.l10n.temporal.interval.DateInterval.YEARS;

/**
 * <p>
 * Default implementation of {@link QuotaListViewUserFilterManager}. Reads the user-specific
 * quota list view filter configuration from the {@code OMQFUserFilter.CONFIG} JSON column
 * and exposes typed accessors.
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaListViewUserFilterManagerImpl implements QuotaListViewUserFilterManager {

    private QuotaListViewUserFilterManagerImpl() {
    }

    private static class Instance {
        private static final QuotaListViewUserFilterManagerImpl INSTANCE = new QuotaListViewUserFilterManagerImpl();
    }

    /**
     * Gets the {@link QuotaListViewUserFilterManagerImpl}
     *
     * @return The singleton instance of {@link QuotaListViewUserFilterManagerImpl}
     */
    public static QuotaListViewUserFilterManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the quota user filter table.
     * </p>
     *
     * @return quota user filter table.
     */
    private Table getTable() {
        return Table.get(OMQFLISTVIEWUSERFILTER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              The organization id
     * @param userId             The user id
     * @param filterCriteriaJson the {@link JsonObject} containing the filter configuration to persist
     */
    @Override
    public void updateRecentlyAppliedFilter(final long orgId, final long userId, final JsonObject filterCriteriaJson) {
        final Table userFilterTable = getTable();
        final OrgDataStore orgDataStore = DataStoreProvider.getSecureOrgDataStore(orgId);
        final Row existingRow = orgDataStore.get(userFilterTable,
                new WhereClause(Criteria.eq(userFilterTable.getColumn(OMQFLISTVIEWUSERFILTER.USER_ID), userId))).getRow(userFilterTable);
        final DataContainer targetContainer = DataContainer.create();
        final Row row = Objects.isNull(existingRow) ? new Row(userFilterTable) : existingRow;

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMQFLISTVIEWUSERFILTER.USER_ID, userId);
            row.set(OMQFLISTVIEWUSERFILTER.FILTER_CRITERIA, filterCriteriaJson);
            targetContainer.addNewRow(row);
        } else {
            final String existingCriteria = existingRow.get(OMQFLISTVIEWUSERFILTER.FILTER_CRITERIA);

            existingRow.set(OMQFLISTVIEWUSERFILTER.FILTER_CRITERIA, mergeFilterCriteria(Json.object(existingCriteria), filterCriteriaJson));
            targetContainer.updateRow(existingRow);
        }
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The {@link QuotaListViewUserFilter} resolved from the persisted CONFIG JSON,
     * or {@code null} if no filter is configured for the user
     */
    @Override
    public Optional<QuotaListViewUserFilter> get(final long orgId, final long userId) {
        final JsonObject filterCriteriaJson = getFilterCriteria(orgId, userId);

        if (Objects.isNull(filterCriteriaJson)) {
            return Optional.empty();
        }
        final QuotaListViewUserFilter filter = new QuotaListViewUserFilter();

        filter.setOrgId(orgId);
        filter.setUserId(userId);
        filter.setSelectedUserIds(toLongCollection(filterCriteriaJson.optJsonArray(QuotaListFields.USER_IDS)));
        filter.setTeamIds(toLongCollection(filterCriteriaJson.optJsonArray(QuotaListFields.TEAM_IDS)));

        if (filterCriteriaJson.containsKey(QuotaListFields.METRIC)) {
            filter.setQuotaMetric(QuotaMetric.getById(filterCriteriaJson.getShort(QuotaListFields.METRIC)));
        }

        if (filterCriteriaJson.containsKey(QuotaListFields.FORECAST_TYPE)) {
            filter.setForecastType(ForecastType.getById(filterCriteriaJson.getShort(QuotaListFields.FORECAST_TYPE)));
        }
        filter.setListType(filterCriteriaJson.getString(QuotaListFields.LIST_TYPE));
        filter.setQuotaDateInterval(getQuotaDateInterval(filterCriteriaJson));

        return Optional.of(filter);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The {@link QuotaDateInterval} resolved from the persisted CONFIG JSON,
     * or {@code null} when not configured
     */
    @Override
    public Optional<QuotaDateInterval> getQuotaDateInterval(final long orgId, final long userId) {
        return Optional.ofNullable(getFilterCriteria(orgId, userId)).map(this::getQuotaDateInterval);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The {@link QuotaMetric} resolved from the persisted CONFIG JSON,
     * or {@code null} when not configured
     */
    @Override
    public Optional<QuotaMetric> getQuotaMetric(final long orgId, final long userId) {
        return Optional.ofNullable(getFilterCriteria(orgId, userId)).map(filterCriteriaJson ->
                filterCriteriaJson.containsKey(QuotaListFields.METRIC) ? QuotaMetric.getById((short) filterCriteriaJson.getInt(QuotaListFields.METRIC)) : null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The {@link ForecastType} resolved from the persisted CONFIG JSON,
     * or {@code null} when not configured
     */
    @Override
    public Optional<ForecastType> getForecastType(final long orgId, final long userId) {
        return Optional.ofNullable(getFilterCriteria(orgId, userId)).map(filterCriteriaJson ->
                filterCriteriaJson.containsKey(QuotaListFields.FORECAST_TYPE) ? ForecastType.getById((short) filterCriteriaJson.getInt(QuotaListFields.FORECAST_TYPE)) : null);
    }

    /**
     * <p>
     * Merges the incoming filter criteria into the existing one. A key is overwritten only
     * when the incoming JSON actually contains it; an absent key leaves the existing value
     * untouched. This matters because Team View and Hierarchy View share this persisted
     * filter, so switching views must not clear the selected users or teams simply because
     * one view doesn't send that particular field.
     * </p>
     *
     * @param existingJson The currently persisted filter criteria {@link JsonObject}
     * @param incomingJson The new filter criteria {@link JsonObject} to merge in
     * @return The merged {@link JsonObject}
     */
    private JsonObject mergeFilterCriteria(final JsonObject existingJson, final JsonObject incomingJson) {

        if (incomingJson.containsKey(QuotaListFields.USER_IDS)) {
            existingJson.put(QuotaListFields.USER_IDS, incomingJson.getJsonArray(QuotaListFields.USER_IDS));
        }

        if (incomingJson.containsKey(QuotaListFields.TEAM_IDS)) {
            existingJson.put(QuotaListFields.TEAM_IDS, incomingJson.getJsonArray(QuotaListFields.TEAM_IDS));
        }

        if (incomingJson.containsKey(QuotaListFields.METRIC)) {
            existingJson.put(QuotaListFields.METRIC, incomingJson.get(QuotaListFields.METRIC));
        }

        if (incomingJson.containsKey(QuotaListFields.FORECAST_TYPE)) {
            existingJson.put(QuotaListFields.FORECAST_TYPE, incomingJson.get(QuotaListFields.FORECAST_TYPE));
        }

        if (incomingJson.containsKey(QuotaListFields.DATE_INTERVAL)) {
            existingJson.put(QuotaListFields.DATE_INTERVAL, incomingJson.get(QuotaListFields.DATE_INTERVAL));
        }

        if (incomingJson.containsKey(QuotaListFields.LIST_TYPE)) {
            existingJson.put(QuotaListFields.LIST_TYPE, incomingJson.get(QuotaListFields.LIST_TYPE));
        }

        return existingJson;
    }

    /**
     * <p>
     * Fetches the persisted user filter configuration as a {@link JsonObject}.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The deserialized config {@link JsonObject}, or {@code null} if absent
     */
    private JsonObject getFilterCriteria(final long orgId, final long userId) {
        final Table table = getTable();
        final Row row = DataStoreProvider.getOrgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMQFLISTVIEWUSERFILTER.USER_ID), userId))).getRow(table);

        if (Objects.isNull(row)) {
            return null;
        }
        final String filterCriteria = row.get(OMQFLISTVIEWUSERFILTER.FILTER_CRITERIA);

        if (Objects.isNull(filterCriteria)) {
            return null;
        }
        return Json.object(filterCriteria);
    }

    /**
     * <p>
     * Gets the {@link QuotaDateInterval} from the filter criteria {@link JsonObject}.
     * </p>
     *
     * @param filterCriteriaJson Represents the filter criteria {@link JsonObject}
     * @return The resolved {@link QuotaDateInterval}, or {@code null} if not configured
     */
    private QuotaDateInterval getQuotaDateInterval(final JsonObject filterCriteriaJson) {
        final JsonObject dateIntervalJson = filterCriteriaJson.optJsonObject(QuotaListFields.DATE_INTERVAL);

        if (Objects.isNull(dateIntervalJson)) {
            return null;
        }
        final String name = dateIntervalJson.getString(QuotaListFields.NAME);
        final DateInterval dateInterval = switch (name) {
            case QuotaFields.MONTH -> MONTHS;
            case QuotaFields.QUARTER -> QUARTERS;
            default -> YEARS;
        };

        return new QuotaDateInterval(dateIntervalJson.getShort(QuotaListFields.ID), dateInterval);
    }

    /**
     * <p>
     * Converts a {@link JsonArray} of numeric values to a {@link Collection} of {@link Long}.
     * </p>
     *
     * @param jsonArray Represents the JSON array
     * @return The collection of long values; an empty set when the array is null or empty
     */
    private Collection<Long> toLongCollection(final JsonArray jsonArray) {
        if (Objects.isNull(jsonArray)) {
            return Collections.emptySet();
        }
        final int size = jsonArray.size();
        final Collection<Long> values = new HashSet<>(size);

        for (int index = 0; index < size; index++) {
            values.add(jsonArray.getLong(index));
        }
        return values;
    }
}