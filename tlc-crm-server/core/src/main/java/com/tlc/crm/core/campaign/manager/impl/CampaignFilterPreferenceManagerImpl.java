package com.tlc.crm.core.campaign.manager.impl;

import com.tlc.commons.json.Json;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignFilterPreferenceManager;
import com.tlc.crm.core.campaign.model.*;
import com.tlc.crm.core.sql.resource.OMKCPRECENTLYAPPLIEDFILTER;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Provides the {@link CampaignFilterUserPreference} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignFilterPreferenceManagerImpl implements CampaignFilterPreferenceManager {

    private CampaignFilterPreferenceManagerImpl() {
    }

    private static final class InstanceHolder {
        private static final CampaignFilterPreferenceManagerImpl INSTANCE = new CampaignFilterPreferenceManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceManagerImpl}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceManagerImpl}.
     */
    public static CampaignFilterPreferenceManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     * 
     * @param orgId Represents the organization id.
     * @return {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link Table}.
     */
    @Override
    public Table getTable() {
        return Table.get(OMKCPRECENTLYAPPLIEDFILTER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param category Represents the campaign category.
     * @return {@link CampaignFilterUserPreference}.
     */
    @Override
    public CampaignFilterUserPreference get(final Long orgId, final Long userId, final CampaignCategory category) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKCPRECENTLYAPPLIEDFILTER.ORG_ID), orgId))
                .and(Criteria.eq(table.getColumn(OMKCPRECENTLYAPPLIEDFILTER.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMKCPRECENTLYAPPLIEDFILTER.CATEGORY), category.getId())));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(table);

        return Objects.nonNull(row) ? convertRowToPreference(orgId, row) : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @param preference Represents the filter preference.
     */
    @Override
    public void create(final CampaignFilterUserPreference preference) {
        final DataContainer targetContainer = DataContainer.create();

        loadPreferenceIntoContainer(true, preference, null, targetContainer);
        orgDataStore(preference.orgId()).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     * 
     * @param preference Represents the filter preference.
     */
    @Override
    public void update(final CampaignFilterUserPreference preference) {
        final Long orgId = preference.orgId();
        final Table preferenceTable = getTable();
        final DataContainer sourceContainer = orgDataStore(orgId).get(getTable(),
                new WhereClause(Criteria.eq(preferenceTable.getColumn(OMKCPRECENTLYAPPLIEDFILTER.USER_ID), preference.getUserId()))
                        .and(Criteria.eq(preferenceTable.getColumn(OMKCPRECENTLYAPPLIEDFILTER.CATEGORY), preference.getCategory().getId())));
        final DataContainer targetContainer = DataContainer.create();

        loadPreferenceIntoContainer(false, preference, sourceContainer, targetContainer);

        orgDataStore(preference.orgId()).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the campaign filter preference details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param preference      Holds the {@link CampaignFilterUserPreference} attributes.
     * @param sourceContainer Holds the old preference values.
     * @param targetContainer Holds the updated preference values.
     */
    private void loadPreferenceIntoContainer(final boolean isCreateEvent, final CampaignFilterUserPreference preference,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final DateTime currentTime = new DateTime(Instant.now().toEpochMilli());
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, preference.id());
        final CampaignDefaultFilter campaignDefaultFilter = preference.getCampaignDefaultFilter();
        final CampaignFolder campaignFolder = preference.getCampaignFolder();
        final Collection<Short> selectedStatusIds = preference.getSelectedStatusIds();

        if (row.isNewRow()) {
            preference.setId(row.getPKValue());
            row.setOrgId(preference.orgId());
            row.set(OMKCPRECENTLYAPPLIEDFILTER.CATEGORY, preference.getCategory().getId());
            row.set(OMKCPRECENTLYAPPLIEDFILTER.USER_ID, preference.getUserId());
        }
        row.set(OMKCPRECENTLYAPPLIEDFILTER.GROUP_ID, preference.getCampaignFilterGroup().getId());
        row.set(OMKCPRECENTLYAPPLIEDFILTER.DEFAULT_FILTER_ID, Objects.isNull(campaignDefaultFilter) ? null : campaignDefaultFilter.getId());
        row.set(OMKCPRECENTLYAPPLIEDFILTER.FOLDER_ID, Objects.isNull(campaignFolder) ? null : campaignFolder.id());

        if (Objects.nonNull(selectedStatusIds) && !selectedStatusIds.isEmpty()) {
            final JsonObject statusJson = Json.object();

            statusJson.put(CampaignFields.STATUS_IDS, selectedStatusIds);
            row.set(OMKCPRECENTLYAPPLIEDFILTER.STATUS_CONFIG, statusJson.toString());
        }
        row.set(OMKCPRECENTLYAPPLIEDFILTER.UPDATED_AT, currentTime);
        preference.setModifiedTime(currentTime);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Converts the preference {@link Row} into {@link CampaignFilterUserPreference}.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param row   Represents the preference row.
     * @return {@link CampaignFilterUserPreference}.
     */
    private CampaignFilterUserPreference convertRowToPreference(final Long orgId, final Row row) {
        final CampaignFilterUserPreference preference = new CampaignFilterUserPreference();

        preference.setId(row.getPKValue());
        preference.setOrgId(orgId);
        preference.setUserId(row.get(OMKCPRECENTLYAPPLIEDFILTER.USER_ID));
        preference.setCategory(CampaignCategory.get(row.get(OMKCPRECENTLYAPPLIEDFILTER.CATEGORY)));
        preference.setCampaignFilterGroup(CampaignFilterGroup.get(row.get(OMKCPRECENTLYAPPLIEDFILTER.GROUP_ID)));
        final Short defaultFilterId = row.get(OMKCPRECENTLYAPPLIEDFILTER.DEFAULT_FILTER_ID);

        if (Objects.nonNull(defaultFilterId)) {
            preference.setCampaignDefaultFilter(CampaignDefaultFilter.get(defaultFilterId));
        }
        final Long folderId = row.get(OMKCPRECENTLYAPPLIEDFILTER.FOLDER_ID);

        if (Objects.nonNull(folderId)) {
            preference.setCampaignFolder(new CampaignFolder(folderId));
        }
        final String statusConfig = row.get(OMKCPRECENTLYAPPLIEDFILTER.STATUS_CONFIG);

        if (Objects.nonNull(statusConfig)) {
            final JsonObject statusJson = Json.object(statusConfig);

            if (statusJson.containsKey(CampaignFields.STATUS_IDS)) {
                final Collection<CampaignStatus> statuses = new HashSet<>();

                statusJson.getJsonArray(CampaignFields.STATUS_IDS).forEach(statusId ->
                        statuses.add(CampaignStatus.get(NumberUtils.parseNumber(statusId).shortValue())));

                preference.setSelectedStatuses(statuses);
            }
        }
        preference.setModifiedTime(row.get(OMKCPRECENTLYAPPLIEDFILTER.UPDATED_AT));

        return preference;
    }
}