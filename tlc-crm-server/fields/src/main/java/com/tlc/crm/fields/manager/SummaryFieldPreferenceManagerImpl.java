package com.tlc.crm.fields.manager;

import com.tlc.commons.json.Json;
import com.tlc.crm.fields.models.SummaryFieldPreference;
import com.tlc.crm.fields.sql.resource.OMETSUMMARYFIELDUSERPREFERENCE;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Objects;

/**
 * <p>
 * Manages the Summary Field User Preference related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
final class SummaryFieldPreferenceManagerImpl implements SummaryFieldPreferenceManager {

    private SummaryFieldPreferenceManagerImpl() {
    }

    /**
     * <p>
     *  Holds the instance of {@link SummaryFieldPreferenceManagerImpl}
     * </p>
     */
    private static final class InstanceHolder {
        private static final SummaryFieldPreferenceManagerImpl INSTANCE = new SummaryFieldPreferenceManagerImpl();
    }

    /**
     * Gets the singleton instance of {@link SummaryFieldPreferenceManagerImpl}
     *
     * @return the {@link SummaryFieldPreferenceManagerImpl} instance
     */
    static SummaryFieldPreferenceManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Gets the summary field user preference table
     *
     * @return the {@link Table} for summary field user preference
     */
    @Override
    public Table getTable() {
        return Table.get(OMETSUMMARYFIELDUSERPREFERENCE.TABLE);
    }

    /**
     * Gets the organization data store
     *
     * @param orgId the organization ID
     * @return the {@link OrgDataStore} for the given organization
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   the organization ID
     * @param userId  the user ID
     * @param sourceId the source ID
     * @return the {@link SummaryFieldPreference} if found, null otherwise
     */
    @Override
    public SummaryFieldPreference get(final Long orgId, final Long userId, final int sourceId) {
        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMETSUMMARYFIELDUSERPREFERENCE.USER_ID), userId))
                        .and(Criteria.eq(table.getColumn(OMETSUMMARYFIELDUSERPREFERENCE.SOURCE_ID), sourceId)));

        return dataContainer.getRows(table).map(this::convertRowToModel).findFirst().orElseGet(() -> SummaryFieldPreference.getDefaultPreference(orgId, userId, sourceId));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      the organization ID
     * @param preference the {@link SummaryFieldPreference} to update
     */
    @Override
    public void update(final Long orgId, final SummaryFieldPreference preference) {
        final Table table = getTable();
        final Long userId = preference.getUserId();
        final int sourceId = preference.getSourceId();
        final DataContainer dataContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMETSUMMARYFIELDUSERPREFERENCE.USER_ID), userId))
                        .and(Criteria.eq(table.getColumn(OMETSUMMARYFIELDUSERPREFERENCE.SOURCE_ID), sourceId)));
        final Row existingPreferenceRow = dataContainer.getRow(table);
        final Long currentTime = Instant.now().toEpochMilli();
        final Row preferenceRow = Objects.nonNull(existingPreferenceRow) ? existingPreferenceRow : new Row(table);
        final DataContainer targetContainer = DataContainer.create();

        if (preferenceRow.isNewRow()) {
            preferenceRow.setOrgId(orgId);
            preferenceRow.set(OMETSUMMARYFIELDUSERPREFERENCE.USER_ID, userId);
            preferenceRow.set(OMETSUMMARYFIELDUSERPREFERENCE.SOURCE_ID, sourceId);
            preferenceRow.set(OMETSUMMARYFIELDUSERPREFERENCE.CREATED_AT, currentTime);
        }

        preferenceRow.set(OMETSUMMARYFIELDUSERPREFERENCE.PREFERENCE, preference.getPreference());
        preferenceRow.set(OMETSUMMARYFIELDUSERPREFERENCE.UPDATED_AT, currentTime);

        targetContainer.updateRow(preferenceRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Converts a row to a SummaryFieldPreference model
     *
     * @param row the {@link Row} from database
     * @return the {@link SummaryFieldPreference} model
     */
    private SummaryFieldPreference convertRowToModel(final Row row) {
        final SummaryFieldPreference summaryFieldPreference = new SummaryFieldPreference();
        final String preference = row.get(OMETSUMMARYFIELDUSERPREFERENCE.PREFERENCE);

        summaryFieldPreference.setId(row.getPKValue());
        summaryFieldPreference.setOrgId(row.getOrgId());
        summaryFieldPreference.setUserId(row.get(OMETSUMMARYFIELDUSERPREFERENCE.USER_ID));
        summaryFieldPreference.setSourceId(((Number) row.get(OMETSUMMARYFIELDUSERPREFERENCE.SOURCE_ID)).intValue());
        summaryFieldPreference.setCreatedAt(row.get(OMETSUMMARYFIELDUSERPREFERENCE.CREATED_AT));
        summaryFieldPreference.setUpdatedAt(row.get(OMETSUMMARYFIELDUSERPREFERENCE.UPDATED_AT));

        if (Objects.nonNull(preference)) {
            summaryFieldPreference.setPreference(preference);
        }

        return summaryFieldPreference;
    }
}